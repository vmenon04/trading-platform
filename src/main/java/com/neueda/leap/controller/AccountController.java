package com.neueda.leap.controller;

import com.neueda.leap.dto.AccountHoldingResponseDTO;
import com.neueda.leap.dto.AccountSummaryResponseDTO;
import com.neueda.leap.dto.DepositRequestDTO;
import com.neueda.leap.dto.TradeResponseDTO;
import com.neueda.leap.entity.AccountHolding;
import com.neueda.leap.entity.AccountTrade;
import com.neueda.leap.entity.Instrument;
import com.neueda.leap.enums.ActivityStatus;
import com.neueda.leap.repository.AccountHoldingMapper;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.service.AccountService;
import com.neueda.leap.service.InstrumentService;
import com.neueda.leap.service.TradeAuthorizationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;
    private final AccountHoldingMapper accountHoldingMapper;
    private final AccountTradeMapper accountTradeMapper;
    private final InstrumentService instrumentService;
    private final TradeAuthorizationService tradeAuthorizationService;

    public AccountController(AccountService accountService,
                             AccountHoldingMapper accountHoldingMapper, AccountTradeMapper accountTradeMapper,
                             InstrumentService instrumentService,
                             TradeAuthorizationService tradeAuthorizationService) {
        this.accountService = accountService;
        this.accountHoldingMapper = accountHoldingMapper;
        this.accountTradeMapper = accountTradeMapper;
        this.instrumentService = instrumentService;
        this.tradeAuthorizationService = tradeAuthorizationService;
    }

    @PostMapping("/{accountId}/deposits")
    public ResponseEntity<Void> deposit(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID accountId,
                                        @Valid @RequestBody DepositRequestDTO request) {
        // the depositor is the logged-in user from the token, never an ID the caller puts in the request
        Long userId = Long.parseLong(jwt.getSubject());
        Long accountInternalId = accountService.getAccountIdByExternalAccountId(accountId);

        if (!tradeAuthorizationService.ownsAccount(userId, accountInternalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        accountService.deposit(accountInternalId, request.amount());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountSummaryResponseDTO> getSummary(@PathVariable UUID accountId) {
        Long internalId = accountService.getAccountIdByExternalAccountId(accountId);
        BigDecimal cashBalance = accountService.getBalance(internalId);

        // market value of the active holdings; a holding with no price available counts as 0
        BigDecimal investmentValue = activeHoldings(internalId).stream()
                .map(holding -> {
                    BigDecimal price = marketPrice(holding.getInstrumentId());
                    return price == null ? BigDecimal.ZERO : holding.getQuantity().multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // TODO: calculate cost basis
        return ResponseEntity.ok(new AccountSummaryResponseDTO(accountId, investmentValue, cashBalance, BigDecimal.ZERO));
    }

    @GetMapping("/{accountId}/holdings")
    public ResponseEntity<List<AccountHoldingResponseDTO>> getHoldings(@PathVariable UUID accountId) {
        Long internalId = accountService.getAccountIdByExternalAccountId(accountId);
        List<AccountHoldingResponseDTO> response = activeHoldings(internalId).stream()
                .map(holding -> {
                    Instrument instrument = instrumentService.getInstrumentById(holding.getInstrumentId());
                    return new AccountHoldingResponseDTO(accountId, instrument.getName(), instrument.getTicker(),
                            holding.getQuantity(), marketPrice(holding.getInstrumentId()));
                })
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


    // current price (cached in Redis), or null if there's no price for it right now, so one instrument
    // without a quote doesn't fail the whole portfolio
    private BigDecimal marketPrice(Long instrumentId) {
        try {
            return instrumentService.getCurrentPrice(instrumentId);
        } catch (IllegalStateException | RestClientException e) {
            LOGGER.warn("No market price for instrument {}: {}", instrumentId, e.getMessage());
            return null;
        }
    }

    private List<AccountHolding> activeHoldings(Long accountId) {
        return accountHoldingMapper.findByAccountId(accountId).stream()
                .filter(holding -> holding.getStatus() == ActivityStatus.ACTIVE)
                .toList();
    }

    private TradeResponseDTO toTradeResponse(AccountTrade trade) {
        var instrument = instrumentService.getInstrumentById(trade.getInstrumentId());
        return new TradeResponseDTO(trade.getInstrumentId(),
            instrument.getTicker(), instrument.getName(), trade.getExternalTradeId(),
                trade.getTradeSide().name(), trade.getQuantity(), trade.getPPU(), trade.getTotalPrice(), trade.getStatus().name(), null);
    }
}
