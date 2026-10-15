package com.neueda.leap.controller;

import com.neueda.leap.dto.AccountHoldingResponseDTO;
import com.neueda.leap.dto.AccountSummaryResponseDTO;
import com.neueda.leap.dto.TaskStatusResponseDTO;
import com.neueda.leap.dto.TradeResponseDTO;
import com.neueda.leap.entity.AccountHolding;
import com.neueda.leap.entity.AccountTrade;
import com.neueda.leap.enums.ActivityStatus;
import com.neueda.leap.repository.AccountHoldingMapper;
import com.neueda.leap.repository.AccountMapper;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.service.AccountService;
import com.neueda.leap.service.InstrumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final AccountHoldingMapper accountHoldingMapper;
    private final AccountTradeMapper accountTradeMapper;
    private final InstrumentService instrumentService;

    public AccountController(AccountService accountService, AccountMapper accountMapper,
                             AccountHoldingMapper accountHoldingMapper, AccountTradeMapper accountTradeMapper,
                             InstrumentService instrumentService) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
        this.accountHoldingMapper = accountHoldingMapper;
        this.accountTradeMapper = accountTradeMapper;
        this.instrumentService = instrumentService;
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountSummaryResponseDTO> getSummary(@PathVariable UUID accountId) {
        Long internalId = accountService.getAccountIdByExternalAccountId(accountId);
        BigDecimal investmentValue = activeHoldings(internalId).stream()
                .map(holding -> holding.getQuantity().multiply(instrumentService.getCurrentPrice(holding.getInstrumentId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cashBalance = accountService.getBalance(internalId);
        return ResponseEntity.ok(new AccountSummaryResponseDTO(accountId, investmentValue, cashBalance, investmentValue));
    }

    @GetMapping("/{accountId}/holdings")
    public ResponseEntity<List<AccountHoldingResponseDTO>> getHoldings(@PathVariable UUID accountId) {
        Long internalId = accountService.getAccountIdByExternalAccountId(accountId);
        List<AccountHoldingResponseDTO> response = activeHoldings(internalId).stream()
                .map(holding -> new AccountHoldingResponseDTO(accountId, holding.getInstrumentId(),
                        instrumentService.getInstrumentById(holding.getInstrumentId()).getTicker(), holding.getQuantity(),
                        instrumentService.getCurrentPrice(holding.getInstrumentId())))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/orders")
    public ResponseEntity<List<TradeResponseDTO>> getOrders(@PathVariable UUID accountId,
                                                              @RequestParam(required = false) String side) {
        Long internalId = accountService.getAccountIdByExternalAccountId(accountId);
        List<TradeResponseDTO> response = accountTradeMapper.findByAccountId(internalId).stream()
                .filter(trade -> side == null || trade.getTradeSide().name().equalsIgnoreCase(side))
                .map(this::toTradeResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/tasks/{taskId}")
    public ResponseEntity<TaskStatusResponseDTO> getTask(@PathVariable UUID accountId,
                                                          @PathVariable Long taskId) {
        accountService.getAccountIdByExternalAccountId(accountId);
        AccountTrade trade = accountTradeMapper.findById(taskId);
        if (trade == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new TaskStatusResponseDTO(taskId, trade.getInstrumentId(),
                instrumentService.getInstrumentById(trade.getInstrumentId()).getTicker(), trade.getQuantity(),
                trade.getTradeSide().name(), trade.getPrice(), trade.getStatus().name(), null));
    }

    private List<AccountHolding> activeHoldings(Long accountId) {
        return accountHoldingMapper.findByAccountId(accountId).stream()
                .filter(holding -> holding.getStatus() == ActivityStatus.ACTIVE)
                .toList();
    }

    private TradeResponseDTO toTradeResponse(AccountTrade trade) {
        return new TradeResponseDTO(trade.getInstrumentId(),
                instrumentService.getInstrumentById(trade.getInstrumentId()).getTicker(), trade.getTradeId(),
                trade.getTradeSide().name(), trade.getQuantity(), trade.getPrice(), trade.getStatus().name(), null);
    }
}
