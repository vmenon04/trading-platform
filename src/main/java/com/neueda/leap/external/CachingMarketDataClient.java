package com.neueda.leap.external;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;

// caches prices from another MarketDataClient in Redis, to save Fauxnance's daily quota.
// Redis is only a cache: if it is down, prices come straight from the API.
public class CachingMarketDataClient implements MarketDataClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachingMarketDataClient.class);

    private final MarketDataClient delegate;
    private final StringRedisTemplate redis;
    private final Duration ttl;

    public CachingMarketDataClient(MarketDataClient delegate, StringRedisTemplate redis, Duration ttl) {
        this.delegate = delegate;
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public BigDecimal getPrice(String ticker) {
        String key = "price:" + ticker;
        BigDecimal cached = readCache(key);
        if (cached != null) {
            return cached;
        }
        BigDecimal price = delegate.getPrice(ticker);
        // no price (unknown symbol, stale quote) isn't cached, so the next lookup asks the API again
        if (price != null) {
            writeCache(key, price);
        }
        return price;
    }

    // Redis first for every ticker; only the ones not in the cache go to the API (in batches), and the
    // prices that come back are cached for next time
    @Override
    public Map<String, BigDecimal> getPrices(Collection<String> tickers) {
        List<String> symbols = List.copyOf(tickers);
        Map<String, BigDecimal> prices = new HashMap<>();
        List<String> misses = new ArrayList<>();

        List<String> cached = readCache(symbols.stream().map(CachingMarketDataClient::key).toList());
        for (int i = 0; i < symbols.size(); i++) {
            String value = cached == null ? null : cached.get(i);
            if (value == null) {
                misses.add(symbols.get(i));
            } else {
                prices.put(symbols.get(i), new BigDecimal(value));
            }
        }

        if (!misses.isEmpty()) {
            Map<String, BigDecimal> fetched = delegate.getPrices(misses);
            fetched.forEach((ticker, price) -> writeCache(key(ticker), price));
            prices.putAll(fetched);
        }
        return prices;
    }

    private static String key(String ticker) {
        return "price:" + ticker;
    }

    // one round trip for all the keys; null when Redis is down, so every ticker counts as a miss
    private List<String> readCache(List<String> keys) {
        try {
            return redis.opsForValue().multiGet(keys);
        } catch (DataAccessException e) {
            LOGGER.warn("Price cache read failed for {} prices, using the API: {}", keys.size(), e.getMessage());
            return null;
        }
    }

    private BigDecimal readCache(String key) {
        try {
            String value = redis.opsForValue().get(key);
            // stored as a plain string so the scale survives (189.50 stays 189.50)
            return value == null ? null : new BigDecimal(value);
        } catch (DataAccessException e) {
            LOGGER.warn("Price cache read failed for {}, using the API: {}", key, e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, BigDecimal price) {
        try {
            redis.opsForValue().set(key, price.toPlainString(), ttl);
        } catch (DataAccessException e) {
            LOGGER.warn("Price cache write failed for {}: {}", key, e.getMessage());
        }
    }
}
