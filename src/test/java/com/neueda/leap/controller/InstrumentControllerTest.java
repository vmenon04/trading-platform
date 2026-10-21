package com.neueda.leap.controller;

import com.neueda.leap.service.InstrumentCatalog;
import com.neueda.leap.service.InstrumentCatalog.CatalogInstrument;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InstrumentControllerTest {

    private static final CatalogInstrument APPLE = new CatalogInstrument(1L, "AAPL", "Apple Inc", "STOCK");
    private static final CatalogInstrument BITCOIN = new CatalogInstrument(2L, "BTC-USD", "Bitcoin USD", "CRYPTO");

    @Mock
    private InstrumentService instrumentService;

    @Mock
    private InstrumentCatalog instrumentCatalog;

    @InjectMocks
    private InstrumentController instrumentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(instrumentController).build();
    }

    @Test
    void getInstruments_listsTheCatalogueWithCurrentPrices() throws Exception {
        when(instrumentCatalog.known()).thenReturn(List.of(APPLE, BITCOIN));
        when(instrumentService.getCurrentPrices(List.of("AAPL", "BTC-USD"))).thenReturn(Map.of(
                "AAPL", new BigDecimal("189.50"), "BTC-USD", new BigDecimal("64000.00")));

        mockMvc.perform(get("/api/v1/instruments").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].instrumentId").value(1))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].name").value("Apple Inc"))
                .andExpect(jsonPath("$[0].price").value(189.50))
                .andExpect(jsonPath("$[0].assetClass").value("STOCK"))
                .andExpect(jsonPath("$[1].ticker").value("BTC-USD"))
                .andExpect(jsonPath("$[1].assetClass").value("CRYPTO"))
                .andExpect(jsonPath("$[1].price").value(64000.00));
        verify(instrumentCatalog, never()).lookup(anyString());
    }

    @Test
    void getInstruments_instrumentWithoutPrice_isStillListedWithNullPrice() throws Exception {
        when(instrumentCatalog.known()).thenReturn(List.of(APPLE, BITCOIN));
        when(instrumentService.getCurrentPrices(List.of("AAPL", "BTC-USD"))).thenReturn(Map.of("AAPL", new BigDecimal("189.50")));

        mockMvc.perform(get("/api/v1/instruments").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].ticker").value("BTC-USD"))
                .andExpect(jsonPath("$[1].price").doesNotExist());
    }

    @Test
    void getInstruments_emptyCatalogue_returnsEmptyList() throws Exception {
        when(instrumentCatalog.known()).thenReturn(List.of());
        when(instrumentService.getCurrentPrices(List.of())).thenReturn(Map.of());

        mockMvc.perform(get("/api/v1/instruments").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getInstruments_search_looksUpTheSymbol() throws Exception {
        CatalogInstrument oracle = new CatalogInstrument(22L, "ORCL", "Oracle Corporation", "STOCK");
        when(instrumentCatalog.lookup("orcl")).thenReturn(oracle);
        when(instrumentService.getCurrentPrices(List.of("ORCL"))).thenReturn(Map.of("ORCL", new BigDecimal("130.16")));

        mockMvc.perform(get("/api/v1/instruments").param("search", "orcl").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].instrumentId").value(22))
                .andExpect(jsonPath("$[0].ticker").value("ORCL"))
                .andExpect(jsonPath("$[0].price").value(130.16));
        verify(instrumentCatalog, never()).known();
    }

    @Test
    void getInstruments_searchUnknownSymbol_returnsEmptyList() throws Exception {
        when(instrumentCatalog.lookup("NOPE")).thenReturn(null);
        when(instrumentService.getCurrentPrices(List.of())).thenReturn(Map.of());

        mockMvc.perform(get("/api/v1/instruments").param("search", "NOPE").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
