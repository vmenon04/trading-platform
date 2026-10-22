package com.neueda.leap.service;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.enums.InstrumentType;
import com.neueda.leap.external.MarketDataClient;
import com.neueda.leap.external.SymbolInfo;
import com.neueda.leap.repository.InstrumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The instruments users can trade, stored in Redis. A symbol is looked up in Redis first; on a miss its details
 * come from the market data API (Fauxnance), it gets a row and id in the instruments table (trades and holdings
 * reference that id), and it is stored in Redis for next time.
 */
@Service
public class InstrumentCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentCatalog.class);

    // every symbol in the catalogue; each one's details are in the hash instrument:{SYMBOL}
    static final String KNOWN_KEY = "instruments:known";

    private final StringRedisTemplate redis;
    private final MarketDataClient marketDataClient;
    private final InstrumentMapper instrumentMapper;

    public InstrumentCatalog(StringRedisTemplate redis, MarketDataClient marketDataClient,
                             InstrumentMapper instrumentMapper) {
        this.redis = redis;
        this.marketDataClient = marketDataClient;
        this.instrumentMapper = instrumentMapper;
    }

    // assetClass is the InstrumentType name, e.g. STOCK
    public record CatalogInstrument(Long instrumentId, String ticker, String name, String assetClass) {
    }

    /**
     * Finds an instrument by symbol: Redis first, then the market data API.
     *
     * @param symbol e.g. ORCL or BTC-USD (case-insensitive)
     * @return the instrument with its database id, or null if the symbol is blank, unknown or can't be looked up now
     */
    public CatalogInstrument lookup(String symbol) {
        String ticker = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        if (ticker.isEmpty()) {
            return null;
        }
        CatalogInstrument cached = read(ticker);
        if (cached != null) {
            return cached;
        }

        SymbolInfo info;
        try {
            info = marketDataClient.getSymbol(ticker);
        } catch (IllegalStateException | RestClientException e) {
            // malformed symbol, bad key, quota used up or API down: nothing to add right now
            LOGGER.warn("Symbol lookup failed for {}: {}", ticker, e.getMessage());
            return null;
        }
        if (info == null) {
            return null;
        }

        String name = info.name() == null || info.name().isBlank() ? ticker : info.name();
        InstrumentType type = assetClass(info.type());
        CatalogInstrument instrument = new CatalogInstrument(idFor(ticker, name, type), ticker, name, type.name());
        write(instrument);
        return instrument;
    }

    /**
     * @return every instrument in the catalogue (anything looked up or traded so far), sorted by ticker;
     *         empty if Redis is unavailable
     */
    public List<CatalogInstrument> known() {
        Set<String> tickers;
        try {
            Set<String> members = redis.opsForSet().members(KNOWN_KEY);
            tickers = members == null ? Set.of() : new TreeSet<>(members);
        } catch (DataAccessException e) {
            LOGGER.warn("Instrument catalogue read failed: {}", e.getMessage());
            return List.of();
        }
        List<CatalogInstrument> instruments = new ArrayList<>();
        for (String ticker : tickers) {
            CatalogInstrument instrument = read(ticker);
            if (instrument != null) {
                instruments.add(instrument);
            }
        }
        return instruments;
    }

    // the instrument's id in our database, adding a row the first time a symbol reaches us
    private Long idFor(String ticker, String name, InstrumentType type) {
        Long existing = instrumentMapper.findIdByTicker(ticker);
        if (existing != null) {
            return existing;
        }
        Instrument instrument = new Instrument(name, ticker, type);
        instrumentMapper.insert(instrument);
        return instrument.getInstrumentId();
    }

    // Fauxnance types to the asset classes the instruments table allows
    static InstrumentType assetClass(String fauxnanceType) {
        if (fauxnanceType == null) {
            return InstrumentType.STOCK;
        }
        return switch (fauxnanceType.toLowerCase(Locale.ROOT)) {
            case "etf" -> InstrumentType.ETF;
            case "fx", "forex" -> InstrumentType.FOREX;
            case "crypto" -> InstrumentType.CRYPTO;
            case "bond" -> InstrumentType.BOND;
            default -> InstrumentType.STOCK;
        };
    }

    private static String key(String ticker) {
        return "instrument:" + ticker;
    }

    private CatalogInstrument read(String ticker) {
        try {
            Map<Object, Object> fields = redis.opsForHash().entries(key(ticker));
            if (fields == null || fields.get("id") == null) {
                return null;
            }
            return new CatalogInstrument(Long.valueOf(fields.get("id").toString()), ticker,
                    String.valueOf(fields.get("name")), String.valueOf(fields.get("assetClass")));
        } catch (DataAccessException e) {
            LOGGER.warn("Instrument cache read failed for {}, using the API: {}", ticker, e.getMessage());
            return null;
        }
    }

    private void write(CatalogInstrument instrument) {
        try {
            redis.opsForHash().putAll(key(instrument.ticker()), Map.of(
                    "id", instrument.instrumentId().toString(),
                    "name", instrument.name(),
                    "assetClass", instrument.assetClass()));
            redis.opsForSet().add(KNOWN_KEY, instrument.ticker());
        } catch (DataAccessException e) {
            LOGGER.warn("Instrument cache write failed for {}: {}", instrument.ticker(), e.getMessage());
        }
    }
}
