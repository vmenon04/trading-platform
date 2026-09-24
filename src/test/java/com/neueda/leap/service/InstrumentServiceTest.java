package com.neueda.leap.service;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.external.MarketDataClient;
import com.neueda.leap.repository.InstrumentMapper;
import java.math.BigDecimal;
import java.util.List;
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

    private final Instrument instrument = new Instrument();

    @Test
    void getInstrumentByIdReturnsInstrumentFromMapper() {
        when(instrumentMapper.findById(1)).thenReturn(instrument);
        assertSame(instrument, instrumentService.getInstrumentById(1));
    }

    @Test
    void getInstrumentByIdThrowsWhenNotFound() {
        when(instrumentMapper.findById(99)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> instrumentService.getInstrumentById(99));
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
}
