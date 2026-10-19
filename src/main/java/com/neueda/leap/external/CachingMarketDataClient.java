package com.neueda.leap.external;

import java.math.BigDecimal;
import java.time.Duration;
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
