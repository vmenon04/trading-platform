package com.neueda.leap.sprint6;

import com.neueda.leap.entity.AccountHolding;
import com.neueda.leap.entity.Instrument;
import com.neueda.leap.repository.AccountHoldingMapper;
import com.neueda.leap.repository.AccountMapper;
import com.neueda.leap.repository.InstrumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Mockito-based controller test - no security, no auth stub needed
@WebMvcTest(TradeController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerMockitoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountMapper accountMapper;

    @MockBean
    private InstrumentMapper instrumentMapper;

    @MockBean
    private AccountHoldingMapper holdingMapper;


    @Test
    void aValidBuyOrderIsAccepted() throws Exception {
        Instrument instrument = new Instrument("APPLE", "APPL", InstrumentType.EQUITY);

        when(instrumentMapper.findByTicker("APPL")).thenReturn(instrument);
        when(holdingMapper.findByAccountInstrumentAndDate(1, 1, "09252026")).thenReturn(null);
        doNothing().when(holdingMapper).insertSnapshot(1, 1, BigDecimal.valueOf(10.0), "PENDING");

        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrument_id": 1,
                              "ticker":"APPL",
                              "tradeType":"BUY",
                              "quantity":10,
                              "price":40.0
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.expectedPrice").value(40.0))
                .andExpect(jsonPath("$.expectedQuantity").value(10));
    }

    @Test
    void aValidSellOrderIsAccepted() throws Exception {
        Instrument instrument = new Instrument("APPLE", "APPL", InstrumentType.EQUITY);

        when(instrumentMapper.findByTicker("APPL")).thenReturn(instrument);
        when(holdingMapper.findByAccountInstrumentAndDate(1, 1, "09252026")).thenReturn(null);
        doNothing().when(holdingMapper).insertSnapshot(1, 1, BigDecimal.valueOf(10.0), "PENDING");

        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrument_id": 1,
                               "ticker":"APPL",
                              "tradeType":"SELL",
                              "quantity":5,
                              "price":40.0
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.expectedPrice").value(40.0))
                .andExpect(jsonPath("$.expectedQuantity").value(5));
    }

    @Test
    void sellingMoreThanHeldIsRejected() throws Exception {
        Instrument instrument = new Instrument("APPLE", "APPL", InstrumentType.EQUITY);

        AccountHolding holding = new AccountHolding(1, 1, LocalDate.now(), 100.0, HoldingStatus.ACTIVE);

        when(instrumentMapper.findByTicker("APPL")).thenReturn(instrument);
        when(holdingMapper.findByAccountInstrumentAndDate(1, 1, "09252026")).thenReturn(holding);

        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrument_id": 1,
                              "ticker":"APPL",
                              "tradeType":"SELL",
                              "quantity":110,
                              "price": 30
                            }
                            """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Insufficient holding for the requested trade"));
    }

    @Test
    void sellingMoreThanHeldIsRejected() throws Exception {
        Instrument instrument = new Instrument("APPLE", "APPL", InstrumentType.EQUITY);

        AccountHolding holding = new AccountHolding(1, 1, LocalDate.now(), 100.0, HoldingStatus.ACTIVE);

        when(instrumentMapper.findByTicker("APPL")).thenReturn(instrument);
        when(holdingMapper.findByAccountInstrumentAndDate(1, 1, "09252026")).thenReturn(holding);

        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrument_id": 1,
                              "ticker":"APPL",                            
                              "tradeType":"SELL",
                              "quantity":110,
                              "price": 30
                            }
                            """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Insufficient holding for the requested trade"));
    }

    @Test
    void anUnknownTickerReturns404() throws Exception {
        when(instrumentMapper.findByTicker("NOTREAL")).thenReturn(null);

        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrument_id": 1,
                              "ticker":"NOTREAL",                            
                              "tradeType":"SELL",
                              "quantity":110,
                              "price": 30
                            }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("404"))
                .andExpect(jsonPath("$.message").value("Resource not found"));;
    }

    @Test
    void aMalformedRequestReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrument_id": 1,
                              "ticker":"NOTREAL",                            
                              "tradeType":"SELL",
                              "quantity":-5,
                              "price": 30
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }
}

