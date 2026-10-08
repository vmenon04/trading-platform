package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.dto.TradeFinishedDTO;
import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.enums.TradeStatus;
import com.neueda.leap.repository.AccountTradePriceMapper;
import com.neueda.leap.repository.AccountTradeStatusMapper;
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

    private final AccountService accountService;
    private final AccountHoldingService accountHoldingService;
    private final InstrumentService instrumentService;
    private final AccountTradeStatusMapper accountTradeStatusMapper;
    private final AccountTradePriceMapper accountTradePriceMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * Creates an execution service for pricing trades and applying their balance, holding and status updates.
     *
     * @param accountService service used to adjust account cash balances
     * @param accountHoldingService service used to adjust account holdings
     * @param instrumentService service used to get current instrument prices
     * @param accountTradeStatusMapper mapper used to read and record trade statuses
     * @param accountTradePriceMapper mapper used to read and record trade prices
     * @param transactionTemplate template used to apply a fulfilled trade's updates as a single transaction
     */
    public ExecutionService(AccountService accountService, AccountHoldingService accountHoldingService,
                            InstrumentService instrumentService, AccountTradeStatusMapper accountTradeStatusMapper,
                            AccountTradePriceMapper accountTradePriceMapper, TransactionTemplate transactionTemplate) {
        this.accountService = accountService;
        this.accountHoldingService = accountHoldingService;
        this.instrumentService = instrumentService;
        this.accountTradeStatusMapper = accountTradeStatusMapper;
        this.accountTradePriceMapper = accountTradePriceMapper;
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
//        Long accountId = order.accountId();
        Long accountId = 1L;
        applyTrade(accountId, order.instrumentId(), order.side(), order.quantity(), price.multiply(order.quantity()));
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
        TradeStatus status = accountTradeStatusMapper.getTradeStatusByTradeId(tradeId);
        if (status == TradeStatus.FULFILLED || status == TradeStatus.REJECTED) {
            Double storedPrice = accountTradePriceMapper.getTradePriceByTradeId(tradeId);
            return outcome(trade, storedPrice == null ? null : BigDecimal.valueOf(storedPrice), null);
        }

        try {
            BigDecimal price = instrumentService.getCurrentPrice(trade.instrumentId());
            BigDecimal total = price.multiply(trade.quantity());

            transactionTemplate.executeWithoutResult(transaction -> {
                applyTrade(trade.accountId(), trade.instrumentId(), trade.side(), trade.quantity(), total);
                // the price mapper takes Doubles for now
                accountTradePriceMapper.insertTradePrice(tradeId, price.doubleValue(), total.doubleValue());
                accountTradeStatusMapper.insertTradeStatus(tradeId, TradeStatus.FULFILLED);
            });
            return outcome(trade, price, null);
        } catch (IllegalStateException | IllegalArgumentException | NoSuchElementException | RestClientException e) {
            // business failures (no price, Fauxnance down, insufficient funds or holdings, unknown instrument)
            // reject the trade; anything else propagates so the message is retried
            accountTradeStatusMapper.insertTradeStatus(tradeId, TradeStatus.REJECTED);
            return outcome(trade, null, e.getMessage());
        }
    }

    private void applyTrade(Long accountId, Long instrumentId, TradeSide side, BigDecimal quantity, BigDecimal total) {
        if (side == TradeSide.BUY) {
            accountService.purchase(accountId, total);
            accountHoldingService.addQuantity(accountId, instrumentId, quantity);
        } else if (side == TradeSide.SELL) {
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
