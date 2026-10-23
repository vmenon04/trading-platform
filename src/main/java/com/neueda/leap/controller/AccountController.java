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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountHoldingMapper accountHoldingMapper;
    private final AccountTradeMapper accountTradeMapper;
    private final InstrumentService instrumentService;

    public AccountController(AccountService accountService,
                             AccountHoldingMapper accountHoldingMapper, AccountTradeMapper accountTradeMapper,
                             InstrumentService instrumentService) {
        this.accountService = accountService;
        this.accountHoldingMapper = accountHoldingMapper;
        this.accountTradeMapper = accountTradeMapper;
        this.instrumentService = instrumentService;
    }

    @PreAuthorize("@accessControlService.userOwnsAccount(#accountId, #jwt.getSubject())")
    @PostMapping("/{accountId}/deposits")
    public ResponseEntity<Void> deposit(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID accountId,
                                        @Valid @RequestBody DepositRequestDTO request) {
        // the depositor is the logged-in user from the token, never an ID the caller puts in the request
        Long accountInternalId = accountService.getAccountIdByExternalAccountId(accountId);
        accountService.deposit(accountInternalId, request.amount());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@accessControlService.userOwnsAccount(#accountId, #jwt.getSubject())")
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountSummaryResponseDTO> getSummary(@PathVariable UUID accountId,
                                                                @AuthenticationPrincipal Jwt jwt) {
        Long internalId = accountService.getAccountIdByExternalAccountId(accountId);
        BigDecimal cashBalance = accountService.getBalance(internalId);

        // market value of the active holdings; a holding with no price available counts as 0
        List<AccountHolding> holdings = activeHoldings(internalId);
        Map<Long, Instrument> instruments = instrumentsOf(holdings);
        Map<String, BigDecimal> prices = displayPrices(instruments);
        BigDecimal investmentValue = holdings.stream()
                .map(holding -> {
                    BigDecimal price = prices.get(instruments.get(holding.getInstrumentId()).getTicker());
                    return price == null ? BigDecimal.ZERO : holding.getQuantity().multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // what was paid for the shares still held (average cost of the filled trades)
        BigDecimal costBasis = accountService.getCostBasis(internalId);
        BigDecimal portfolioValue = investmentValue.add(cashBalance);

        return ResponseEntity.ok(new AccountSummaryResponseDTO(accountId, investmentValue,  portfolioValue, cashBalance, costBasis));
    }

    @PreAuthorize("@accessControlService.userOwnsAccount(#accountId, #jwt.getSubject())")
    @GetMapping("/{accountId}/holdings")
    public ResponseEntity<List<AccountHoldingResponseDTO>> getHoldings(@PathVariable UUID accountId,
                                                                       @AuthenticationPrincipal Jwt jwt) {
        Long internalId = accountService.getAccountIdByExternalAccountId(accountId);
        List<AccountHolding> holdings = activeHoldings(internalId);
        Map<Long, Instrument> instruments = instrumentsOf(holdings);
        Map<String, BigDecimal> prices = displayPrices(instruments);
        List<AccountHoldingResponseDTO> response = holdings.stream()
                .map(holding -> {
                    Instrument instrument = instruments.get(holding.getInstrumentId());
                    return new AccountHoldingResponseDTO(accountId, instrument.getName(), instrument.getTicker(),
                            holding.getQuantity(), prices.get(instrument.getTicker()), holding.getInstrumentId(),
                            instrument.getInstrumentType().name());
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@accessControlService.userOwnsAccount(#accountId, #jwt.getSubject())")
    @GetMapping("/{accountId}/orders")
    public ResponseEntity<List<TradeResponseDTO>> getOrders(@PathVariable UUID accountId,
                                                              @RequestParam(required = false) String side,
                                                            @AuthenticationPrincipal Jwt jwt) {
        Long internalId = accountService.getAccountIdByExternalAccountId(accountId);
        List<TradeResponseDTO> response = accountTradeMapper.findByAccountId(internalId).stream()
                .filter(trade -> side == null || trade.getTradeSide().name().equalsIgnoreCase(side))
                .map(this::toTradeResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@accessControlService.userOwnsAccount(#accountId, #jwt.getSubject())")
    @GetMapping("/{accountId}/tasks/{externalTradeId}")
    public ResponseEntity<TradeResponseDTO> getTask(@PathVariable UUID accountId,
                                                          @PathVariable UUID externalTradeId,
                                                          @AuthenticationPrincipal Jwt jwt) {

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

    @PreAuthorize("@accessControlService.userOwnsAccount(#accountId, #jwt.getSubject())")
    @GetMapping("/{accountId}/trades/{externalTradeId}")
    public ResponseEntity<TradeResponseDTO> getTrade(@PathVariable UUID accountId,
                                                          @PathVariable UUID externalTradeId,
                                                          @AuthenticationPrincipal Jwt jwt) {


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


    // each holding's instrument, looked up once
    private Map<Long, Instrument> instrumentsOf(List<AccountHolding> holdings) {
        Map<Long, Instrument> instruments = new HashMap<>();
        holdings.forEach(holding -> instruments.computeIfAbsent(holding.getInstrumentId(),
                instrumentService::getInstrumentById));
        return instruments;
    }

    // prices to show, by ticker, in one batch (cached in Redis): a stale quote shows its last known price, and an
    // instrument with no price right now is left out, so one instrument without a quote doesn't fail the portfolio
    private Map<String, BigDecimal> displayPrices(Map<Long, Instrument> instruments) {
        return instrumentService.getCurrentPrices(instruments.values().stream().map(Instrument::getTicker).toList());
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
