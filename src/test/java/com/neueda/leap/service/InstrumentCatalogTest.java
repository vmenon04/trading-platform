package com.neueda.leap.service;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.enums.InstrumentType;
import com.neueda.leap.external.MarketDataClient;
import com.neueda.leap.external.SymbolInfo;
import com.neueda.leap.repository.InstrumentMapper;
import com.neueda.leap.service.InstrumentCatalog.CatalogInstrument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstrumentCatalogTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private HashOperations<String, Object, Object> hashes;

    @Mock
    private SetOperations<String, String> sets;

    @Mock
    private MarketDataClient marketDataClient;

    @Mock
    private InstrumentMapper instrumentMapper;

    private InstrumentCatalog catalog;

    @BeforeEach
    void setUp() {
        when(redis.<Object, Object>opsForHash()).thenReturn(hashes);
        when(redis.opsForSet()).thenReturn(sets);
        catalog = new InstrumentCatalog(redis, marketDataClient, instrumentMapper);
    }

    @Test
    void lookup_cachedInRedis_doesNotCallTheApiOrDatabase() {
        when(hashes.entries("instrument:ORCL")).thenReturn(Map.of("id", "22", "name", "Oracle Corporation", "assetClass", "STOCK"));

        assertEquals(new CatalogInstrument(22L, "ORCL", "Oracle Corporation", "STOCK"), catalog.lookup(" orcl "));
        verifyNoInteractions(marketDataClient, instrumentMapper);
    }

    @Test
    void lookup_newSymbol_getsADatabaseIdAndIsCachedInRedis() {
        when(hashes.entries("instrument:ORCL")).thenReturn(Map.of());
        when(marketDataClient.getSymbol("ORCL")).thenReturn(new SymbolInfo("ORCL", "Oracle Corporation", "equity"));
        when(instrumentMapper.findIdByTicker("ORCL")).thenReturn(null);
        doAnswer(invocation -> {
            invocation.<Instrument>getArgument(0).setInstrumentId(400L);
            return null;
        }).when(instrumentMapper).insert(any(Instrument.class));

        CatalogInstrument found = catalog.lookup("ORCL");

        assertEquals(new CatalogInstrument(400L, "ORCL", "Oracle Corporation", "STOCK"), found);
        ArgumentCaptor<Instrument> inserted = ArgumentCaptor.forClass(Instrument.class);
        verify(instrumentMapper).insert(inserted.capture());
        assertEquals("ORCL", inserted.getValue().getTicker());
        assertEquals(InstrumentType.STOCK, inserted.getValue().getInstrumentType());
        verify(hashes).putAll("instrument:ORCL", Map.of("id", "400", "name", "Oracle Corporation", "assetClass", "STOCK"));
        verify(sets).add("instruments:known", "ORCL");
    }

    @Test
    void lookup_symbolAlreadyInTheDatabase_reusesItsId() {
        when(hashes.entries("instrument:AAPL")).thenReturn(Map.of());
        when(marketDataClient.getSymbol("AAPL")).thenReturn(new SymbolInfo("AAPL", "Apple Inc", "equity"));
        when(instrumentMapper.findIdByTicker("AAPL")).thenReturn(1L);

        assertEquals(1L, catalog.lookup("AAPL").instrumentId());
        verify(instrumentMapper, never()).insert(any());
    }

    @Test
    void lookup_unknownSymbol_returnsNullAndStoresNothing() {
        when(hashes.entries("instrument:NOPE")).thenReturn(Map.of());
        when(marketDataClient.getSymbol("NOPE")).thenReturn(null);

        assertNull(catalog.lookup("NOPE"));
        verifyNoInteractions(instrumentMapper);
        verify(sets, never()).add(anyString(), any(String[].class));
    }

    @Test
    void lookup_apiFailure_returnsNull() {
        when(hashes.entries("instrument:ORCL")).thenReturn(Map.of());
        when(marketDataClient.getSymbol("ORCL")).thenThrow(new IllegalStateException("Fauxnance returned 429"));

        assertNull(catalog.lookup("ORCL"));
    }

    @Test
    void lookup_blankSymbol_returnsNull() {
        assertNull(catalog.lookup("  "));
        verifyNoInteractions(marketDataClient);
    }

    @Test
    void lookup_redisDown_stillFindsTheSymbol() {
        when(hashes.entries("instrument:ORCL")).thenThrow(new RedisConnectionFailureException("Redis down"));
        when(marketDataClient.getSymbol("ORCL")).thenReturn(new SymbolInfo("ORCL", "Oracle Corporation", "equity"));
        when(instrumentMapper.findIdByTicker("ORCL")).thenReturn(22L);
        when(sets.add(anyString(), anyString())).thenThrow(new RedisConnectionFailureException("Redis down"));
        doAnswer(invocation -> {
            throw new RedisConnectionFailureException("Redis down");
        }).when(hashes).putAll(anyString(), any());

        assertEquals(22L, catalog.lookup("ORCL").instrumentId());
    }

    @Test
    void known_returnsEveryCachedInstrumentSortedByTicker() {
        when(sets.members("instruments:known")).thenReturn(Set.of("ORCL", "AAPL"));
        when(hashes.entries("instrument:AAPL")).thenReturn(Map.of("id", "1", "name", "Apple Inc", "assetClass", "STOCK"));
        when(hashes.entries("instrument:ORCL")).thenReturn(Map.of("id", "22", "name", "Oracle Corporation", "assetClass", "STOCK"));

        assertEquals(List.of(new CatalogInstrument(1L, "AAPL", "Apple Inc", "STOCK"),
                new CatalogInstrument(22L, "ORCL", "Oracle Corporation", "STOCK")), catalog.known());
    }

    @Test
    void known_redisDown_returnsEmptyList() {
        when(sets.members("instruments:known")).thenThrow(new RedisConnectionFailureException("Redis down"));

        assertEquals(List.of(), catalog.known());
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
