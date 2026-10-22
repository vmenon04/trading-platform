package com.neueda.leap.service;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.enums.InstrumentType;
import com.neueda.leap.external.MarketDataClient;
import com.neueda.leap.external.SymbolInfo;
import com.neueda.leap.repository.InstrumentMapper;
import com.neueda.leap.service.InstrumentCatalog.CatalogInstrument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstrumentCatalogTest {

    @Mock
    private MarketDataClient marketDataClient;

    @Mock
    private InstrumentMapper instrumentMapper;

    @InjectMocks
    private InstrumentCatalog catalog;

    private static Instrument instrument(Long id, String ticker, String name, InstrumentType type) {
        Instrument instrument = new Instrument(name, ticker, type);
        instrument.setInstrumentId(id);
        return instrument;
    }

    @Test
    void lookup_inTheDatabase_doesNotCallTheMarketDataApi() {
        when(instrumentMapper.findByTicker("ORCL"))
                .thenReturn(instrument(22L, "ORCL", "Oracle Corporation", InstrumentType.STOCK));

        assertEquals(new CatalogInstrument(22L, "ORCL", "Oracle Corporation", "STOCK"), catalog.lookup(" orcl "));
        verifyNoInteractions(marketDataClient);
    }

    @Test
    void lookup_newSymbol_isAddedToTheDatabase() {
        when(instrumentMapper.findByTicker("ORCL"))
                .thenReturn(null)
                .thenReturn(instrument(400L, "ORCL", "Oracle Corporation", InstrumentType.STOCK));
        when(marketDataClient.getSymbol("ORCL")).thenReturn(new SymbolInfo("ORCL", "Oracle Corporation", "equity"));
        when(instrumentMapper.insertIfAbsent(any(Instrument.class))).thenReturn(1);

        assertEquals(new CatalogInstrument(400L, "ORCL", "Oracle Corporation", "STOCK"), catalog.lookup("ORCL"));

        ArgumentCaptor<Instrument> inserted = ArgumentCaptor.forClass(Instrument.class);
        verify(instrumentMapper).insertIfAbsent(inserted.capture());
        assertEquals("ORCL", inserted.getValue().getTicker());
        assertEquals("Oracle Corporation", inserted.getValue().getName());
        assertEquals(InstrumentType.STOCK, inserted.getValue().getInstrumentType());
    }

    // two lookups of a new symbol at once: the second insert does nothing (UNIQUE ticker), and both get the one row
    @Test
    void lookup_addedByAnotherRequestMeanwhile_usesTheExistingRow() {
        when(instrumentMapper.findByTicker("ORCL"))
                .thenReturn(null)
                .thenReturn(instrument(22L, "ORCL", "Oracle Corporation", InstrumentType.STOCK));
        when(marketDataClient.getSymbol("ORCL")).thenReturn(new SymbolInfo("ORCL", "Oracle Corporation", "equity"));
        when(instrumentMapper.insertIfAbsent(any(Instrument.class))).thenReturn(0);

        assertEquals(22L, catalog.lookup("ORCL").instrumentId());
    }

    @Test
    void lookup_noNameFromTheApi_usesTheTicker() {
        when(instrumentMapper.findByTicker("X:BTC-USD"))
                .thenReturn(null)
                .thenReturn(instrument(5L, "X:BTC-USD", "X:BTC-USD", InstrumentType.CRYPTO));
        when(marketDataClient.getSymbol("X:BTC-USD")).thenReturn(new SymbolInfo("X:BTC-USD", " ", "crypto"));

        catalog.lookup("x:btc-usd");

        ArgumentCaptor<Instrument> inserted = ArgumentCaptor.forClass(Instrument.class);
        verify(instrumentMapper).insertIfAbsent(inserted.capture());
        assertEquals("X:BTC-USD", inserted.getValue().getName());
        assertEquals(InstrumentType.CRYPTO, inserted.getValue().getInstrumentType());
    }

    @Test
    void lookup_unknownSymbol_returnsNullAndAddsNothing() {
        when(instrumentMapper.findByTicker("NOPE")).thenReturn(null);
        when(marketDataClient.getSymbol("NOPE")).thenReturn(null);

        assertNull(catalog.lookup("NOPE"));
        verify(instrumentMapper, never()).insertIfAbsent(any());
    }

    // e.g. still being fetched, quota used up or the API down: nothing to add right now
    @Test
    void lookup_apiFailure_returnsNull() {
        when(instrumentMapper.findByTicker("ORCL")).thenReturn(null);
        when(marketDataClient.getSymbol("ORCL")).thenThrow(new IllegalStateException("Fauxnance returned 429"));

        assertNull(catalog.lookup("ORCL"));
        verify(instrumentMapper, never()).insertIfAbsent(any());
    }

    @Test
    void lookup_blankSymbol_returnsNull() {
        assertNull(catalog.lookup("  "));
        verifyNoInteractions(marketDataClient, instrumentMapper);
    }

    @Test
    void all_listsEveryInstrumentInTheDatabaseSortedByTicker() {
        when(instrumentMapper.findAll()).thenReturn(List.of(
                instrument(22L, "ORCL", "Oracle Corporation", InstrumentType.STOCK),
                instrument(1L, "AAPL", "Apple Inc", InstrumentType.STOCK)));

        assertEquals(List.of(new CatalogInstrument(1L, "AAPL", "Apple Inc", "STOCK"),
                new CatalogInstrument(22L, "ORCL", "Oracle Corporation", "STOCK")), catalog.all());
        verifyNoInteractions(marketDataClient);
    }

    @Test
    void assetClass_mapsFauxnanceTypes() {
        assertEquals(InstrumentType.STOCK, InstrumentCatalog.assetClass("equity"));
        assertEquals(InstrumentType.ETF, InstrumentCatalog.assetClass("etf"));
        assertEquals(InstrumentType.FOREX, InstrumentCatalog.assetClass("fx"));
        assertEquals(InstrumentType.CRYPTO, InstrumentCatalog.assetClass("crypto"));
        assertEquals(InstrumentType.BOND, InstrumentCatalog.assetClass("bond"));
        assertEquals(InstrumentType.STOCK, InstrumentCatalog.assetClass(null));
    }
}
