package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.dto.TradeFinishedDTO;
import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.repository.AccountTradeMapper;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

/**
 * Executes recorded trades by pricing them and applying coordinated cash, holding and trade status updates.
 */
@Service
public class ExecutionService {

    // must match the CHECK constraint on account_trade_status.status
    private static final String REJECTED = "REJECTED";
    private static final String FULFILLED = "FULFILLED";

    private final AccountService accountService;
    private final AccountHoldingService accountHoldingService;
    private final InstrumentService instrumentService;
    private final AccountTradeMapper accountTradeMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * Creates an execution service for pricing trades and applying their balance, holding and status updates.
     *
     * @param accountService service used to adjust account cash balances
     * @param accountHoldingService service used to adjust account holdings
     * @param instrumentService service used to get current instrument prices
     * @param accountTradeMapper mapper used to record trade prices and statuses
     * @param transactionTemplate template used to apply a fulfilled trade's updates as a single transaction
     */
    public ExecutionService(AccountService accountService, AccountHoldingService accountHoldingService,
                            InstrumentService instrumentService, AccountTradeMapper accountTradeMapper,
                            TransactionTemplate transactionTemplate) {
        this.accountService = accountService;
        this.accountHoldingService = accountHoldingService;
        this.instrumentService = instrumentService;
        this.accountTradeMapper = accountTradeMapper;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Executes a validated order as a single transaction.
     *
     * @param order order request to execute
     * @param price execution price used to calculate the total consideration
     * @throws IllegalArgumentException if the order side is unsupported
     * @throws RuntimeException if underlying balance or holding updates fail
     */
    @Transactional
    public void execute(TradeRequestDTO order, BigDecimal price) {
        applyTrade(order.accountId(), order.instrumentId(), order.side(), order.quantity(),
                price.multiply(order.quantity()));
    }

    /**
     * Executes a recorded trade at the current market price. The balance, holding, price and FULFILLED status
     * updates commit together; if the trade can't be priced or settled, none of them are applied and the trade
     * is marked REJECTED instead. A trade that already finished is not executed again, its stored outcome is
     * returned so it can be republished.
     *
     * @param trade recorded trade to execute
     * @return the outcome of the trade to publish to trade.finished, with a null price and a reason if it was rejected
     * @throws RuntimeException if the database is unavailable, so the message can be redelivered
     */
    public TradeFinishedDTO execute(TradeRecordedDTO trade) {
        Long tradeId = trade.tradeId();

        // a redelivered message: report the outcome again rather than trading twice
        String status = accountTradeMapper.findCurrentStatus(tradeId);
        if (FULFILLED.equals(status) || REJECTED.equals(status)) {
            return outcome(trade, accountTradeMapper.findPrice(tradeId), null);
        }

        try {
            // the account, holding and instrument services still take int ids
            int accountId = Math.toIntExact(trade.accountId());
            int instrumentId = Math.toIntExact(trade.instrumentId());
            BigDecimal price = instrumentService.getCurrentPrice(instrumentId);
            BigDecimal total = price.multiply(trade.quantity());

            transactionTemplate.executeWithoutResult(transaction -> {
                applyTrade(accountId, instrumentId, trade.side(), trade.quantity(), total);
                accountTradeMapper.updatePrice(tradeId, price);
                accountTradeMapper.insertTotalPrice(tradeId, total);
                accountTradeMapper.recordStatus(tradeId, FULFILLED);
            });
            return outcome(trade, price, null);
        } catch (IllegalStateException | IllegalArgumentException | NoSuchElementException | RestClientException e) {
            // business failures (no price, Fauxnance down, insufficient funds or holdings, unknown instrument)
            // reject the trade; anything else propagates so the message is retried
            accountTradeMapper.recordStatus(tradeId, REJECTED);
            return outcome(trade, null, e.getMessage());
        }
    }

    private void applyTrade(int accountId, int instrumentId, String side, BigDecimal quantity, BigDecimal total) {
        if ("BUY".equals(side)) {
            accountService.purchase(accountId, total);
            accountHoldingService.addQuantity(accountId, instrumentId, quantity);
        } else if ("SELL".equals(side)) {
            accountHoldingService.removeQuantity(accountId, instrumentId, quantity);
            accountService.sell(accountId, total);
        } else {
            throw new IllegalArgumentException("Side must be BUY or SELL, got " + side);
        }
    }

    // a null price means the trade was rejected
    private static TradeFinishedDTO outcome(TradeRecordedDTO trade, BigDecimal price, String reason) {
        return new TradeFinishedDTO(trade.tradeId(), trade.accountId(), trade.instrumentId(), trade.side(),
                trade.quantity(), price, reason);
    }
}
