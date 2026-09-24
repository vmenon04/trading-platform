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
        return restClient.get()
                .uri("/quotes/{symbol}", ticker)
                .exchange((request, response) -> {
                    int status = response.getStatusCode().value();
                    // 404: unknown symbol; 202: data still being fetched. but either way, no price right now.
                    if (status == 404 || status == 202) {
                        return null;
                    }
                    // 401/403: bad or expired key; 429: daily quota used up; 5xx: API unavailable
                    if (response.getStatusCode().isError()) {
                        throw new IllegalStateException("Fauxnance returned " + status + " for " + ticker);
                    }
                    QuoteResponse body = response.bodyTo(QuoteResponse.class);
                    if (body == null || body.meta().stale()) {
                        return null;
                    }
                    return body.data().price();
                });
    }

    // take the fields we use from the JSON response
    record QuoteResponse(Quote data, Meta meta) {
    }

    record Quote(BigDecimal price) {
    }

    record Meta(boolean stale) {
    }
}
