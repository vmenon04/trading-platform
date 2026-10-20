package com.neueda.leap.controller;

import com.neueda.leap.entity.AccountHolding;
import com.neueda.leap.entity.AccountTrade;
import com.neueda.leap.entity.Instrument;
import com.neueda.leap.enums.ActivityStatus;
import com.neueda.leap.enums.InstrumentType;
import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.enums.TradeStatus;
import com.neueda.leap.repository.AccountHoldingMapper;
import com.neueda.leap.repository.AccountMapper;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.service.AccountService;
import com.neueda.leap.service.InstrumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private static final Long ACCOUNT_ID = 5L;
    private static final Long OTHER_ACCOUNT_ID = 6L;
    private static final Long INSTRUMENT_ID = 1L;

    @Mock
    private AccountService accountService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private AccountHoldingMapper accountHoldingMapper;

    @Mock
    private AccountTradeMapper accountTradeMapper;

    @Mock
    private InstrumentService instrumentService;

    @InjectMocks
    private AccountController accountController;

    private MockMvc mockMvc;

    private final UUID externalAccountId = UUID.randomUUID();
    private final UUID externalTradeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();
    }

    @Test
    void getSummary_returnsCashBalance() throws Exception {
        when(accountService.getAccountIdByExternalAccountId(externalAccountId)).thenReturn(ACCOUNT_ID);
        when(accountService.getBalance(ACCOUNT_ID)).thenReturn(new BigDecimal("2500.00"));

        mockMvc.perform(get("/api/v1/accounts/{accountId}", externalAccountId).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(externalAccountId.toString()))
                .andExpect(jsonPath("$.cashBalance").value(2500.00))
                .andExpect(jsonPath("$.investmentValue").value(0))
                .andExpect(jsonPath("$.costBasis").value(0));
    }

    @Test
    void getHoldings_returnsOnlyActiveHoldings() throws Exception {
        when(accountService.getAccountIdByExternalAccountId(externalAccountId)).thenReturn(ACCOUNT_ID);
        when(accountHoldingMapper.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(
                new AccountHolding(ACCOUNT_ID, INSTRUMENT_ID, null, new BigDecimal("10"), ActivityStatus.ACTIVE),
                new AccountHolding(ACCOUNT_ID, INSTRUMENT_ID, null, new BigDecimal("4"), ActivityStatus.INACTIVE)));
        when(instrumentService.getInstrumentById(INSTRUMENT_ID)).thenReturn(apple());

        mockMvc.perform(get("/api/v1/accounts/{accountId}/holdings", externalAccountId).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountId").value(externalAccountId.toString()))
                .andExpect(jsonPath("$[0].name").value("Apple Inc"))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].quantity").value(10));
    }

    @Test
    void getOrders_withoutSide_returnsAllTrades() throws Exception {
        when(accountService.getAccountIdByExternalAccountId(externalAccountId)).thenReturn(ACCOUNT_ID);
        when(accountTradeMapper.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(
                trade(ACCOUNT_ID, TradeSide.BUY), trade(ACCOUNT_ID, TradeSide.SELL)));
        when(instrumentService.getInstrumentById(INSTRUMENT_ID)).thenReturn(apple());

        mockMvc.perform(get("/api/v1/accounts/{accountId}/orders", externalAccountId).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].side").value("BUY"))
                .andExpect(jsonPath("$[1].side").value("SELL"));
    }

    @Test
    void getOrders_withSide_filtersCaseInsensitively() throws Exception {
        when(accountService.getAccountIdByExternalAccountId(externalAccountId)).thenReturn(ACCOUNT_ID);
        when(accountTradeMapper.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(
                trade(ACCOUNT_ID, TradeSide.BUY), trade(ACCOUNT_ID, TradeSide.SELL)));
        when(instrumentService.getInstrumentById(INSTRUMENT_ID)).thenReturn(apple());

        mockMvc.perform(get("/api/v1/accounts/{accountId}/orders", externalAccountId)
                        .contextPath("/api")
                        .param("side", "sell"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].side").value("SELL"));
    }

    @Test
    void getTrade_tradeOnAccount_returns200WithTradeDetails() throws Exception {
        when(accountTradeMapper.findTradeIdByExternalTradeId(externalTradeId)).thenReturn(42L);
        when(accountTradeMapper.findById(42L)).thenReturn(trade(ACCOUNT_ID, TradeSide.BUY));
        when(accountService.getAccountIdByExternalAccountId(externalAccountId)).thenReturn(ACCOUNT_ID);
        when(instrumentService.getInstrumentById(INSTRUMENT_ID)).thenReturn(apple());

        mockMvc.perform(get("/api/v1/accounts/{accountId}/trades/{tradeId}", externalAccountId, externalTradeId)
                        .contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instrumentId").value(1))
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.tradeId").value(externalTradeId.toString()))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.ppu").value(150.00))
                .andExpect(jsonPath("$.totalPrice").value(1500.00))
                .andExpect(jsonPath("$.status").value("FULFILLED"));
    }

    @Test
    void getTrade_unknownTrade_returns404() throws Exception {
        when(accountTradeMapper.findTradeIdByExternalTradeId(externalTradeId)).thenReturn(null);
        when(accountTradeMapper.findById(null)).thenReturn(null);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/trades/{tradeId}", externalAccountId, externalTradeId)
                        .contextPath("/api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTrade_tradeOnAnotherAccount_returns403() throws Exception {
        when(accountTradeMapper.findTradeIdByExternalTradeId(externalTradeId)).thenReturn(42L);
        when(accountTradeMapper.findById(42L)).thenReturn(trade(OTHER_ACCOUNT_ID, TradeSide.BUY));
        when(accountService.getAccountIdByExternalAccountId(externalAccountId)).thenReturn(ACCOUNT_ID);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/trades/{tradeId}", externalAccountId, externalTradeId)
                        .contextPath("/api"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTask_taskOnAccount_returns200() throws Exception {
        when(accountTradeMapper.findTradeIdByExternalTradeId(externalTradeId)).thenReturn(42L);
        when(accountTradeMapper.findById(42L)).thenReturn(trade(ACCOUNT_ID, TradeSide.SELL));
        when(accountService.getAccountIdByExternalAccountId(externalAccountId)).thenReturn(ACCOUNT_ID);
        when(instrumentService.getInstrumentById(INSTRUMENT_ID)).thenReturn(apple());

        mockMvc.perform(get("/api/v1/accounts/{accountId}/tasks/{tradeId}", externalAccountId, externalTradeId)
                        .contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").value(externalTradeId.toString()))
                .andExpect(jsonPath("$.side").value("SELL"));
    }

    @Test
    void getTask_unknownTask_returns404() throws Exception {
        when(accountTradeMapper.findTradeIdByExternalTradeId(externalTradeId)).thenReturn(null);
        when(accountTradeMapper.findById(null)).thenReturn(null);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/tasks/{tradeId}", externalAccountId, externalTradeId)
                        .contextPath("/api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTask_taskOnAnotherAccount_returns403() throws Exception {
        when(accountTradeMapper.findTradeIdByExternalTradeId(externalTradeId)).thenReturn(42L);
        when(accountTradeMapper.findById(42L)).thenReturn(trade(OTHER_ACCOUNT_ID, TradeSide.BUY));
        when(accountService.getAccountIdByExternalAccountId(externalAccountId)).thenReturn(ACCOUNT_ID);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/tasks/{tradeId}", externalAccountId, externalTradeId)
                        .contextPath("/api"))
                .andExpect(status().isForbidden());
    }

    private static Instrument apple() {
        Instrument instrument = new Instrument("Apple Inc", "AAPL", InstrumentType.STOCK);
        instrument.setInstrumentId(INSTRUMENT_ID);
        return instrument;
    }

    private AccountTrade trade(Long accountId, TradeSide side) {
        AccountTrade trade = new AccountTrade(accountId, INSTRUMENT_ID, side, new BigDecimal("10"),
                new BigDecimal("150.00"), new BigDecimal("1500.00"));
        trade.setExternalTradeId(externalTradeId);
        trade.setTradeStatus(TradeStatus.FULFILLED);
        return trade;
    }
}
