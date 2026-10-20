package com.neueda.leap.config;

import com.neueda.leap.external.CachingMarketDataClient;
import com.neueda.leap.external.HttpMarketDataClient;
import com.neueda.leap.external.MarketDataClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.client.RestClient;

@Configuration
@EnableKafka
public class MarketDataConfiguration {

    // Fauxnance prices, cached in Redis for market.data.cache-ttl (e.g. 5m)
    @Bean
    public MarketDataClient marketDataClient(RestClient.Builder builder, StringRedisTemplate redis,
                                             @Value("${market.data.base-url}") String baseUrl,
                                             @Value("${market.data.api-key}") String apiKey,
                                             @Value("${market.data.cache-ttl}") Duration cacheTtl) {
        return new CachingMarketDataClient(new HttpMarketDataClient(builder, baseUrl, apiKey), redis, cacheTtl);
    }
}
