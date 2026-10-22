package com.neueda.leap.service;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.enums.InstrumentType;
import com.neueda.leap.external.MarketDataClient;
import com.neueda.leap.external.PriceQuote;
import com.neueda.leap.repository.InstrumentMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentServiceTest {

    @Mock
    private InstrumentMapper instrumentMapper;

    @Mock
    private MarketDataClient marketDataClient;

    @InjectMocks
    private InstrumentService instrumentService;

    private final Instrument instrument = new Instrument("Apple Inc", "AAPL", InstrumentType.STOCK);

    @Test
    void getInstrumentByIdReturnsInstrumentFromMapper() {
        when(instrumentMapper.findById(1L)).thenReturn(instrument);
        assertSame(instrument, instrumentService.getInstrumentById(1L));
    }

    @Test
    void getInstrumentByIdThrowsWhenNotFound() {
        when(instrumentMapper.findById(99L)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> instrumentService.getInstrumentById(99L));
    }

    @Test
    void getInstrumentByTickerReturnsInstrumentFromMapper() {
        when(instrumentMapper.findByTicker("AAPL")).thenReturn(instrument);
        assertSame(instrument, instrumentService.getInstrumentByTicker("AAPL"));
    }

    @Test
    void getInstrumentByTickerThrowsWhenNotFound() {
        when(instrumentMapper.findByTicker("NOPE")).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> instrumentService.getInstrumentByTicker("NOPE"));
    }

    @Test
    void getInstrumentByTickerRejectsBlankTicker() {
        assertThrows(IllegalArgumentException.class, () -> instrumentService.getInstrumentByTicker(" "));
        verifyNoInteractions(instrumentMapper);
    }

    @Test
    void getAllInstrumentsReturnsMapperList() {
        when(instrumentMapper.findAll()).thenReturn(List.of(instrument));
        assertEquals(List.of(instrument), instrumentService.getAllInstruments());
    }

    @Test
    void getAllInstrumentsReturnsEmptyListWhenNoneExist() {
        when(instrumentMapper.findAll()).thenReturn(List.of());
        assertTrue(instrumentService.getAllInstruments().isEmpty());
    }

    @Test
    void getCurrentPriceReturnsPriceFromClient() {
        when(instrumentMapper.findByTicker("AAPL")).thenReturn(instrument);
        when(marketDataClient.getPrice("AAPL")).thenReturn(new BigDecimal("189.25"));
        assertEquals(new BigDecimal("189.25"), instrumentService.getCurrentPrice("AAPL"));
    }

    @Test
    void getCurrentPriceThrowsForUnknownInstrumentWithoutCallingApi() {
        when(instrumentMapper.findByTicker("NOPE")).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> instrumentService.getCurrentPrice("NOPE"));
        verifyNoInteractions(marketDataClient);
    }

    @Test
    void getCurrentPriceRejectsBlankTicker() {
        assertThrows(IllegalArgumentException.class, () -> instrumentService.getCurrentPrice(" "));
        verifyNoInteractions(instrumentMapper, marketDataClient);
    }

    @Test
    void getCurrentPriceThrowsWhenApiHasNoQuote() {
        when(instrumentMapper.findByTicker("AAPL")).thenReturn(instrument);
        when(marketDataClient.getPrice("AAPL")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> instrumentService.getCurrentPrice("AAPL"));
    }

    @Test
    void getCurrentPriceRejectsNonPositivePrice() {
        when(instrumentMapper.findByTicker("AAPL")).thenReturn(instrument);
        when(marketDataClient.getPrice("AAPL")).thenReturn(BigDecimal.ZERO);
        assertThrows(IllegalStateException.class, () -> instrumentService.getCurrentPrice("AAPL"));
    }

    // note that this needs Instrument.getTicker() from entity
    @Test
    void getCurrentPriceByIdLooksUpTickerAndReturnsPrice() {
        Instrument apple = mock(Instrument.class);
        when(apple.getTicker()).thenReturn("AAPL");
        when(instrumentMapper.findById(1L)).thenReturn(apple);
        when(instrumentMapper.findByTicker("AAPL")).thenReturn(apple);
        when(marketDataClient.getPrice("AAPL")).thenReturn(new BigDecimal("189.25"));
        assertEquals(new BigDecimal("189.25"), instrumentService.getCurrentPrice(1L));
    }

    @Test
    void getInstrumentByTickerRejectsNullTicker() {
        assertThrows(IllegalArgumentException.class, () -> instrumentService.getInstrumentByTicker(null));
        verifyNoInteractions(instrumentMapper);
    }

    @Test
    void getCurrentPricesAsksForEachTickerOnceAndDropsNonPositivePrices() {
        when(marketDataClient.getPrices(List.of("AAPL", "MSFT"))).thenReturn(Map.of(
                "AAPL", new PriceQuote(new BigDecimal("189.50"), false), "MSFT", new PriceQuote(BigDecimal.ZERO, false)));

        assertEquals(Map.of("AAPL", new BigDecimal("189.50")),
                instrumentService.getCurrentPrices(List.of("AAPL", "MSFT", "AAPL")));
    }

    // the caching client leaves unknown symbols out, but a quote with no price must never break the listing
    @Test
    void getCurrentPricesSkipsQuotesWithNoPrice() {
        when(marketDataClient.getPrices(List.of("NOPE"))).thenReturn(Map.of("NOPE", new PriceQuote(null, false)));

        assertEquals(Map.of(), instrumentService.getCurrentPrices(List.of("NOPE")));
    }

    // getCurrentPrices is for showing prices, so a stale quote still shows its last known price
    @Test
    void getCurrentPricesKeepsStalePrices() {
        when(marketDataClient.getPrices(List.of("AAPL")))
                .thenReturn(Map.of("AAPL", new PriceQuote(new BigDecimal("189.50"), true)));

        assertEquals(Map.of("AAPL", new BigDecimal("189.50")), instrumentService.getCurrentPrices(List.of("AAPL")));
    }

    @Test
    void getCurrentPricesReturnsNoPricesWhenTheApiIsUnavailable() {
        when(marketDataClient.getPrices(List.of("AAPL"))).thenThrow(new IllegalStateException("Fauxnance returned 429"));

        assertEquals(Map.of(), instrumentService.getCurrentPrices(List.of("AAPL")));
    }
}
