package com.neueda.leap.service;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.enums.InstrumentType;
import com.neueda.leap.external.MarketDataClient;
import com.neueda.leap.external.SymbolInfo;
import com.neueda.leap.repository.InstrumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The instruments users can trade. The instruments table is the record of them: a symbol is looked up there first;
 * on a miss its details come from the market data API (Fauxnance, which the caching client caches) and it gets a row
 * and id in the table, which trades and holdings reference.
 */
@Service
public class InstrumentCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentCatalog.class);

    private final MarketDataClient marketDataClient;
    private final InstrumentMapper instrumentMapper;

    public InstrumentCatalog(MarketDataClient marketDataClient, InstrumentMapper instrumentMapper) {
        this.marketDataClient = marketDataClient;
        this.instrumentMapper = instrumentMapper;
    }

    // assetClass is the InstrumentType name, e.g. STOCK
    public record CatalogInstrument(Long instrumentId, String ticker, String name, String assetClass) {
    }

    /**
     * Finds an instrument by symbol: the database first, then the market data API.
     *
     * @param symbol a Fauxnance symbol, e.g. ORCL or X:BTC-USD (case-insensitive)
     * @return the instrument with its database id, or null if the symbol is blank, unknown or can't be looked up now
     */
    public CatalogInstrument lookup(String symbol) {
        String ticker = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        if (ticker.isEmpty()) {
            return null;
        }
        Instrument existing = instrumentMapper.findByTicker(ticker);
        if (existing != null) {
            return toCatalogInstrument(existing);
        }

        SymbolInfo info;
        try {
            info = marketDataClient.getSymbol(ticker);
        } catch (IllegalStateException | RestClientException e) {
            // still being fetched, bad key, quota used up or API down: nothing to add right now
            LOGGER.warn("Symbol lookup failed for {}: {}", ticker, e.getMessage());
            return null;
        }
        if (info == null) {
            return null;
        }

        String name = info.name() == null || info.name().isBlank() ? ticker : info.name();
        // does nothing if another request added the symbol meanwhile (tickers are unique); either way, read the row
        instrumentMapper.insertIfAbsent(new Instrument(name, ticker, assetClass(info.type())));
        return toCatalogInstrument(instrumentMapper.findByTicker(ticker));
    }

    /**
     * @return every instrument in the database, sorted by ticker
     */
    public List<CatalogInstrument> all() {
        return instrumentMapper.findAll().stream()
                .map(InstrumentCatalog::toCatalogInstrument)
                .sorted(Comparator.comparing(CatalogInstrument::ticker))
                .toList();
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

    private static CatalogInstrument toCatalogInstrument(Instrument instrument) {
        return new CatalogInstrument(instrument.getInstrumentId(), instrument.getTicker(), instrument.getName(),
                instrument.getInstrumentType().name());
    }
}
