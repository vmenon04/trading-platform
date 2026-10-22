package com.neueda.leap.external;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachingMarketDataClientTest {

    private static final String TICKER = "AAPL";
    private static final String KEY = "price:AAPL";
    private static final Duration TTL = Duration.ofMinutes(5);

    @Mock
    private MarketDataClient delegate;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    private CachingMarketDataClient client;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        client = new CachingMarketDataClient(delegate, redis, TTL);
    }

    @Test
    void cachedPriceIsReturnedWithoutCallingTheApi() {
        when(values.get(KEY)).thenReturn("189.25");
        assertEquals(new BigDecimal("189.25"), client.getPrice(TICKER));
        verifyNoInteractions(delegate);
    }

    @Test
    void cacheMissCallsTheApiAndCachesThePriceWithTheTtl() {
        when(values.get(KEY)).thenReturn(null);
        when(delegate.getPrice(TICKER)).thenReturn(new BigDecimal("189.25"));

        assertEquals(new BigDecimal("189.25"), client.getPrice(TICKER));
        verify(values).set(KEY, "189.25", TTL);
    }

    @Test
    void cachedPriceKeepsItsScale() {
        when(values.get(KEY)).thenReturn("189.50");
        // assertEquals on BigDecimal compares scale too, so 189.5 would fail
        assertEquals(new BigDecimal("189.50"), client.getPrice(TICKER));
    }

    @Test
    void missingPriceIsNotCached() {
        when(values.get(KEY)).thenReturn(null);
        when(delegate.getPrice(TICKER)).thenReturn(null);

        assertNull(client.getPrice(TICKER));
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void apiErrorPropagatesAndNothingIsCached() {
        when(values.get(KEY)).thenReturn(null);
        when(delegate.getPrice(TICKER)).thenThrow(new IllegalStateException("Fauxnance returned 429 for AAPL"));

        assertThrows(IllegalStateException.class, () -> client.getPrice(TICKER));
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void redisReadFailureFallsBackToTheApi() {
        when(values.get(KEY)).thenThrow(new RedisConnectionFailureException("Redis down"));
        when(delegate.getPrice(TICKER)).thenReturn(new BigDecimal("189.25"));

        assertEquals(new BigDecimal("189.25"), client.getPrice(TICKER));
    }

    @Test
    void getPrices_usesRedisFirstAndOnlyAsksTheApiForMisses() {
        when(values.multiGet(List.of("price:AAPL", "price:MSFT", "price:NVDA"))).thenReturn(Arrays.asList("189.25", null, null));
        when(delegate.getPrices(List.of("MSFT", "NVDA"))).thenReturn(Map.of("MSFT", new BigDecimal("410.10")));

        Map<String, BigDecimal> prices = client.getPrices(List.of("AAPL", "MSFT", "NVDA"));

        assertEquals(Map.of("AAPL", new BigDecimal("189.25"), "MSFT", new BigDecimal("410.10")), prices);
        // the fetched price is cached; NVDA had no price, so nothing is cached for it
        verify(values).set("price:MSFT", "410.10", TTL);
        verify(values, never()).set(eq("price:NVDA"), anyString(), any(Duration.class));
    }

    @Test
    void getPrices_allCached_doesNotCallTheApi() {
        when(values.multiGet(List.of("price:AAPL"))).thenReturn(List.of("189.25"));

        assertEquals(Map.of("AAPL", new BigDecimal("189.25")), client.getPrices(List.of("AAPL")));
        verifyNoInteractions(delegate);
    }

    @Test
    void getPrices_redisDown_fetchesEverythingFromTheApi() {
        when(values.multiGet(List.of("price:AAPL"))).thenThrow(new RedisConnectionFailureException("Redis down"));
        when(delegate.getPrices(List.of("AAPL"))).thenReturn(Map.of("AAPL", new BigDecimal("189.25")));

        assertEquals(Map.of("AAPL", new BigDecimal("189.25")), client.getPrices(List.of("AAPL")));
    }

    @Test
    void redisWriteFailureStillReturnsTheApiPrice() {
        when(values.get(KEY)).thenReturn(null);
        when(delegate.getPrice(TICKER)).thenReturn(new BigDecimal("189.25"));
        doThrow(new RedisConnectionFailureException("Redis down"))
                .when(values).set(KEY, "189.25", TTL);

        assertEquals(new BigDecimal("189.25"), client.getPrice(TICKER));
    }
}
