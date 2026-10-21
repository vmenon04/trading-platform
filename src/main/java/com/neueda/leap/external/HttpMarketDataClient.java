package com.neueda.leap.external;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;

// MarketDataClient backed by the Fauxnance API (GET /quotes/{symbol}, and GET /quotes?symbols= for batches)
public class HttpMarketDataClient implements MarketDataClient {

    // Fauxnance allows up to 25 symbols per batch request, and a batch counts as one request against the quota
    static final int MAX_BATCH = 25;

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

    // Same rules as getPrice: a symbol that errors (unknown, still being fetched) or whose quote is stale
    // gets no price. The request fails as a whole only if Fauxnance itself does (bad key, quota, outage).
    @Override
    public Map<String, BigDecimal> getPrices(Collection<String> tickers) {
        List<String> symbols = List.copyOf(tickers);
        Map<String, BigDecimal> prices = new HashMap<>();
        for (int start = 0; start < symbols.size(); start += MAX_BATCH) {
            String batch = String.join(",", symbols.subList(start, Math.min(start + MAX_BATCH, symbols.size())));
            BatchQuoteResponse body = restClient.get()
                    .uri(uri -> uri.path("/quotes").queryParam("symbols", batch).build())
                    .exchange((request, response) -> {
                        if (response.getStatusCode().isError()) {
                            throw new IllegalStateException("Fauxnance returned " + response.getStatusCode().value()
                                    + " for a batch quote");
                        }
                        return response.bodyTo(BatchQuoteResponse.class);
                    });
            if (body == null || body.data() == null || body.data().quotes() == null) {
                continue;
            }
            for (BatchEntry entry : body.data().quotes()) {
                if (entry.quote() != null && !Boolean.TRUE.equals(entry.stale()) && entry.quote().price() != null) {
                    prices.put(entry.symbol(), entry.quote().price());
                }
            }
        }
        return prices;
    }

    // take the fields we use from the JSON response
    record QuoteResponse(Quote data, Meta meta) {
    }

    // GET /quotes?symbols=: {"data": {"quotes": [{"symbol", "stale", "quote": {"price"}} or {"symbol", "error"}]}}
    record BatchQuoteResponse(BatchData data) {
    }

    record BatchData(List<BatchEntry> quotes) {
    }

    // error entries have no stale field, so it's a Boolean (null there)
    record BatchEntry(String symbol, Boolean stale, Quote quote) {
    }

    record Quote(BigDecimal price) {
    }

    record Meta(boolean stale) {
    }
}
