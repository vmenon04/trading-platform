package com.neueda.leap.sprint6;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

    @Test
    void aValidBuyOrderIsAccepted() throws Exception {
        InstrumentRow instrument = new Instrument();
        instrument.setInstrumentId(101);
        instrument.setTicker("ULVR.L");
        instrument.setAssetClass("EQUITY");

        when(accountMapper.findInstrument("ULVR.L")).thenReturn(instrument);
        when(accountMapper.findHolding(1, "ULVR.L")).thenReturn(null);
        doNothing().when(accountMapper).insertHolding(eq(1), eq(101), eq(10.0));

        mockMvc.perform(post("/accounts/1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "ticker":"ULVR.L",
                              "instrumentType":"EQUITY",
                              "quantity":10,
                              "price":40.0,
                              "side":"BUY"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.fee").value(0.4));
    }

    @Test
    void sellingMoreThanHeldIsRejected() throws Exception {
        InstrumentRow instrument = new InstrumentRow();
        instrument.setInstrumentId(101);
        instrument.setTicker("ULVR.L");
        instrument.setAssetClass("EQUITY");

        HoldingRow holding = new HoldingRow();
        holding.setHoldingId(500);
        holding.setQuantity(100.0);

        when(accountMapper.findInstrument("ULVR.L")).thenReturn(instrument);
        when(accountMapper.findHolding(1, "ULVR.L")).thenReturn(holding);

        mockMvc.perform(post("/accounts/1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "ticker":"ULVR.L",
                              "instrumentType":"EQUITY",
                              "quantity":999999,
                              "price":40.0,
                              "side":"SELL"
                            }
                            """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void anUnknownTickerReturns404() throws Exception {
        when(accountMapper.findInstrument("NOTREAL")).thenReturn(null);

        mockMvc.perform(post("/accounts/1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "ticker":"NOTREAL",
                              "instrumentType":"EQUITY",
                              "quantity":10,
                              "price":1.0,
                              "side":"BUY"
                            }
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    void aMalformedRequestReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/accounts/1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrumentType":"EQUITY",
                              "quantity":-5,
                              "price":40.0,
                              "side":"BUY"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }
}

