package com.neueda.leap.controller;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.enums.InstrumentType;
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
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InstrumentControllerTest {

    @Mock
    private InstrumentService instrumentService;

    @InjectMocks
    private InstrumentController instrumentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(instrumentController).build();
    }

    @Test
    void getInstruments_returnsEachInstrumentWithCurrentPrice() throws Exception {
        Instrument apple = instrument(1L, "Apple Inc", "AAPL");
        Instrument bitcoin = instrument(2L, "Bitcoin", "X:BTC-USD");
        when(instrumentService.getAllInstruments()).thenReturn(List.of(apple, bitcoin));
        when(instrumentService.getCurrentPrices(List.of(apple, bitcoin))).thenReturn(Map.of(
                "AAPL", new BigDecimal("189.50"), "X:BTC-USD", new BigDecimal("64000.00")));

        mockMvc.perform(get("/api/v1/instruments").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].instrumentId").value(1))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].name").value("Apple Inc"))
                .andExpect(jsonPath("$[0].price").value(189.50))
                .andExpect(jsonPath("$[0].assetClass").value("STOCK"))
                .andExpect(jsonPath("$[1].ticker").value("X:BTC-USD"))
                .andExpect(jsonPath("$[1].price").value(64000.00));
    }

    @Test
    void getInstruments_instrumentWithoutPrice_isStillListedWithNullPrice() throws Exception {
        Instrument apple = instrument(1L, "Apple Inc", "AAPL");
        Instrument bitcoin = instrument(2L, "Bitcoin", "X:BTC-USD");
        when(instrumentService.getAllInstruments()).thenReturn(List.of(apple, bitcoin));
        when(instrumentService.getCurrentPrices(List.of(apple, bitcoin))).thenReturn(Map.of("AAPL", new BigDecimal("189.50")));

        mockMvc.perform(get("/api/v1/instruments").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].ticker").value("X:BTC-USD"))
                .andExpect(jsonPath("$[1].price").doesNotExist());
    }

    @Test
    void getInstruments_noInstruments_returnsEmptyList() throws Exception {
        when(instrumentService.getAllInstruments()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/instruments").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private static Instrument instrument(Long id, String name, String ticker) {
        Instrument instrument = new Instrument(name, ticker, InstrumentType.STOCK);
        instrument.setInstrumentId(id);
        return instrument;
    }
}
