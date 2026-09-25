package com.neueda.leap.external;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

// use a fake HTTP server
class HttpMarketDataClientTest {

    private static final String BASE_URL = "https://fauxnance.test/v1";
    private static final String API_KEY = "test-key";

    private MockRestServiceServer server;
    private HttpMarketDataClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpMarketDataClient(builder, BASE_URL, API_KEY);
    }

    // Shape of a GET /quotes/{symbol} response, from the API spec
    private static String quoteJson(String symbol, String price, boolean stale) {
        return """
                {
                  "data": {
                    "symbol": "%s", "price": %s, "bid": 313.5, "ask": 313.56, "spreadBps": 1.8587,
                    "currency": "USD", "change": 3.63, "changePercent": 1.1713, "previousClose": 309.9,
                    "asOf": "2026-08-26T16:13:24Z", "marketState": "open"
                  },
                  "meta": {
                    "asOf": "2026-08-26T16:13:24Z", "disclaimer": "Educational data. Not for investment use.",
                    "symbol": "%s", "source": "cache", "stale": %s, "spreadSource": "modelled"
                  }
                }
                """.formatted(symbol, price, symbol, stale);
    }

    private static String errorJson(String code) {
        return """
                {"error": {"code": "%s", "message": "Error", "details": {}}}
                """.formatted(code);
    }

    @Test
    void returnsPriceFromQuote() {
        server.expect(requestTo(BASE_URL + "/quotes/AAPL"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Api-Key", API_KEY))
                .andRespond(withSuccess(quoteJson("AAPL", "313.53", false), MediaType.APPLICATION_JSON));

        assertEquals(0, new BigDecimal("313.53").compareTo(client.getPrice("AAPL")));
        server.verify();
    }

    @Test
    void returnsNullForStaleQuote() {
        server.expect(requestTo(BASE_URL + "/quotes/AAPL"))
                .andRespond(withSuccess(quoteJson("AAPL", "313.53", true), MediaType.APPLICATION_JSON));

        assertNull(client.getPrice("AAPL"));
    }

    @Test
    void returnsNullWhenSymbolNotFound() {
        server.expect(requestTo(BASE_URL + "/quotes/NOPE"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON).body(errorJson("SYMBOL_NOT_FOUND")));

        assertNull(client.getPrice("NOPE"));
    }

    @Test
    void returnsNullWhileBackfillInProgress() {
        server.expect(requestTo(BASE_URL + "/quotes/AAPL"))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON).body(errorJson("BACKFILL_IN_PROGRESS")));

        assertNull(client.getPrice("AAPL"));
    }

    @Test
    void throwsWhenApiKeyRejected() {
        server.expect(requestTo(BASE_URL + "/quotes/AAPL"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(IllegalStateException.class, () -> client.getPrice("AAPL"));
    }

    @Test
    void throwsWhenQuotaExhausted() {
        server.expect(requestTo(BASE_URL + "/quotes/AAPL"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON).body(errorJson("RATE_LIMITED")));

        assertThrows(IllegalStateException.class, () -> client.getPrice("AAPL"));
    }

    @Test
    void throwsOnServerError() {
        server.expect(requestTo(BASE_URL + "/quotes/AAPL"))
                .andRespond(withServiceUnavailable());

        assertThrows(IllegalStateException.class, () -> client.getPrice("AAPL"));
    }

    @Test
    void requestsCryptoSymbolUnchanged() {
        // The ':' is percent-encoded in the URL (%3A); the API decodes it back to X:BTC-USD
        server.expect(requestTo(BASE_URL + "/quotes/X%3ABTC-USD"))
                .andRespond(withSuccess(quoteJson("X:BTC-USD", "64250.12", false), MediaType.APPLICATION_JSON));

        assertEquals(0, new BigDecimal("64250.12").compareTo(client.getPrice("X:BTC-USD")));
    }
}
