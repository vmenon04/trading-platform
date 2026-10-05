package com.neueda.leap.config;

import com.neueda.leap.external.HttpMarketDataClient;
import com.neueda.leap.external.MarketDataClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.client.RestClient;

@Configuration
@EnableKafka
public class MarketDataConfiguration {

    @Bean
    public MarketDataClient marketDataClient(RestClient.Builder builder,
                                             @Value("${market.data.base-url}") String baseUrl,
                                             @Value("${market.data.api-key}") String apiKey) {
        return new HttpMarketDataClient(builder, baseUrl, apiKey);
    }
}

