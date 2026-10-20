package com.neueda.leap.controller;

import com.neueda.leap.dto.AccountHoldingResponseDTO;
import com.neueda.leap.dto.AccountSummaryResponseDTO;
import com.neueda.leap.dto.DepositRequestDTO;
import com.neueda.leap.dto.TradeResponseDTO;
import com.neueda.leap.entity.AccountHolding;
import com.neueda.leap.entity.AccountTrade;
import com.neueda.leap.enums.ActivityStatus;
import com.neueda.leap.repository.AccountHoldingMapper;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.service.AccountService;
import com.neueda.leap.service.ClientAccountService;
import com.neueda.leap.service.ClientService;
import com.neueda.leap.service.InstrumentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountHoldingMapper accountHoldingMapper;
    private final AccountTradeMapper accountTradeMapper;
    private final InstrumentService instrumentService;
    private final ClientService clientService;
    private final ClientAccountService clientAccountService;

    public AccountController(AccountService accountService,
                             AccountHoldingMapper accountHoldingMapper, AccountTradeMapper accountTradeMapper,
                             InstrumentService instrumentService, ClientService clientService,
                             ClientAccountService clientAccountService) {
        this.accountService = accountService;
        this.accountHoldingMapper = accountHoldingMapper;
        this.accountTradeMapper = accountTradeMapper;
        this.instrumentService = instrumentService;
        this.clientService = clientService;
        this.clientAccountService = clientAccountService;
    }

    @PostMapping("/{accountId}/deposits")
    public ResponseEntity<Void> deposit(@PathVariable UUID accountId, @Valid @RequestBody DepositRequestDTO request) {
        Long accountInternalId = accountService.getAccountIdByExternalAccountId(request.accountId());
        Long clientInternalId = clientService.getInternalClientId(request.clientId());
        if (clientInternalId == null) {
            throw new NoSuchElementException("No client with external client id " + request.clientId());
        }

        if (!clientAccountService.existsByClientIdAndAccountId(clientInternalId, accountInternalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        accountService.deposit(accountInternalId, request.amount());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountSummaryResponseDTO> getSummary(@PathVariable UUID accountId) {
        Long internalId = accountService.getAccountIdByExternalAccountId(accountId);
        BigDecimal cashBalance = accountService.getBalance(internalId);
        
        // TODO: calculate cost basis and investment value 
        return ResponseEntity.ok(new AccountSummaryResponseDTO(accountId, new BigDecimal(0), cashBalance, new BigDecimal(0)));
    }

    @GetMapping("/{accountId}/holdings")
    public ResponseEntity<List<AccountHoldingResponseDTO>> getHoldings(@PathVariable UUID accountId) {
        Long internalId = accountService.getAccountIdByExternalAccountId(accountId);
        List<AccountHoldingResponseDTO> response = activeHoldings(internalId).stream()
                .map(holding -> new AccountHoldingResponseDTO(accountId, instrumentService.getInstrumentById(holding.getInstrumentId()).getName(),
                        instrumentService.getInstrumentById(holding.getInstrumentId()).getTicker(), holding.getQuantity(),
                        new BigDecimal(0)))
                .toList();
                // TODO: add market price
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

    @GetMapping("/{accountId}/tasks/{externalTradeId}")
    public ResponseEntity<TradeResponseDTO> getTask(@PathVariable UUID accountId,
                                                          @PathVariable UUID externalTradeId) {

        Long taskInternalId = accountTradeMapper.findTradeIdByExternalTradeId(externalTradeId);
        AccountTrade task = accountTradeMapper.findById(taskInternalId);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        Long accountInternalId = accountService.getAccountIdByExternalAccountId(accountId);
        
        if (!accountInternalId.equals(task.getAccountId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        TradeResponseDTO responseDTO = toTradeResponse(task);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/{accountId}/trades/{externalTradeId}")
    public ResponseEntity<TradeResponseDTO> getTrade(@PathVariable UUID accountId,
                                                          @PathVariable UUID externalTradeId) {

        
        Long tradeInternalId = accountTradeMapper.findTradeIdByExternalTradeId(externalTradeId);
        AccountTrade trade = accountTradeMapper.findById(tradeInternalId);

        if (trade == null) {
            return ResponseEntity.notFound().build();
        }

        Long accountInternalId = accountService.getAccountIdByExternalAccountId(accountId);
        
        if (!accountInternalId.equals(trade.getAccountId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        TradeResponseDTO responseDTO = toTradeResponse(trade);
        return ResponseEntity.ok(responseDTO);
    }


    private List<AccountHolding> activeHoldings(Long accountId) {
        return accountHoldingMapper.findByAccountId(accountId).stream()
                .filter(holding -> holding.getStatus() == ActivityStatus.ACTIVE)
                .toList();
    }

    private TradeResponseDTO toTradeResponse(AccountTrade trade) {
        return new TradeResponseDTO(trade.getInstrumentId(),
                instrumentService.getInstrumentById(trade.getInstrumentId()).getTicker(), trade.getExternalTradeId(),
                trade.getTradeSide().name(), trade.getQuantity(), trade.getPPU(), trade.getTotalPrice(), trade.getStatus().name(), null);
    }
}
