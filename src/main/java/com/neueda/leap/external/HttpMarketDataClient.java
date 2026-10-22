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
                    // this prices trades, so a stale quote (past Fauxnance's freshness window) means no price
                    if (body == null || body.data() == null || body.meta() == null || body.meta().stale()) {
                        return null;
                    }
                    return body.data().price();
                });
    }

    // Prices for showing: a symbol that errors (unknown, still being fetched) gets no price, and a stale quote keeps
    // its last known price, marked stale. The request fails as a whole only if Fauxnance itself does (bad key,
    // quota, outage).
    @Override
    public Map<String, PriceQuote> getPrices(Collection<String> tickers) {
        List<String> symbols = List.copyOf(tickers);
        Map<String, PriceQuote> prices = new HashMap<>();
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
                if (entry.quote() != null && entry.quote().price() != null) {
                    prices.put(entry.symbol(), new PriceQuote(entry.quote().price(), Boolean.TRUE.equals(entry.stale())));
                }
            }
        }
        return prices;
    }

    // GET /symbols/{symbol}; 404 (unknown symbol) or 202 (still being fetched) means no symbol right now
    @Override
    public SymbolInfo getSymbol(String symbol) {
        return restClient.get()
                .uri("/symbols/{symbol}", symbol)
                .exchange((request, response) -> {
                    int status = response.getStatusCode().value();
                    if (status == 404 || status == 202) {
                        return null;
                    }
                    if (response.getStatusCode().isError()) {
                        throw new IllegalStateException("Fauxnance returned " + status + " for symbol " + symbol);
                    }
                    SymbolResponse body = response.bodyTo(SymbolResponse.class);
                    return body == null ? null : body.data();
                });
    }

    // take the fields we use from the JSON response
    record QuoteResponse(Quote data, Meta meta) {
    }

    record Meta(boolean stale) {
    }

    record SymbolResponse(SymbolInfo data) {
    }

    // GET /quotes?symbols=: {"data": {"quotes": [{"symbol", "stale", "quote": {"price"}} or {"symbol", "error"}]}}
    record BatchQuoteResponse(BatchData data) {
    }

    record BatchData(List<BatchEntry> quotes) {
    }

    // an entry for a symbol that errored has no stale flag (or quote), so stale can be missing
    record BatchEntry(String symbol, Boolean stale, Quote quote) {
    }

    record Quote(BigDecimal price) {
    }
}
