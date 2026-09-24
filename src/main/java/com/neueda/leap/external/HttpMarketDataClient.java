package com.neueda.leap.external;

import java.math.BigDecimal;
import org.springframework.web.client.RestClient;

// MarketDataClient backed by the Fauxnance API (GET /quotes/{symbol})
public class HttpMarketDataClient implements MarketDataClient {

    private final RestClient restClient;

    public HttpMarketDataClient(RestClient.Builder builder, String baseUrl, String apiKey) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Api-Key", apiKey)
                .build();
    }

    @Override
    public BigDecimal getPrice(String ticker) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
