package com.neueda.leap.service;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.external.MarketDataClient;
import com.neueda.leap.repository.InstrumentMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Provides instrument lookups and current market price retrieval.
 */
@Service
public class InstrumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentService.class);

    private final InstrumentMapper instrumentMapper;
    private final MarketDataClient marketDataClient;

    /**
     * Creates an instrument service backed by instrument persistence and market data providers.
     *
     * @param instrumentMapper mapper used to query instrument reference data
     * @param marketDataClient client used to retrieve current prices
     */
    public InstrumentService(InstrumentMapper instrumentMapper, MarketDataClient marketDataClient) {
        this.instrumentMapper = instrumentMapper;
        this.marketDataClient = marketDataClient;
    }

    /**
     * Returns an instrument by its identifier.
     *
     * @param instrumentId instrument identifier
     * @return matching instrument
     * @throws NoSuchElementException if the instrument does not exist
     */
    public Instrument getInstrumentById(Long instrumentId) {
        Instrument instrument = instrumentMapper.findById(instrumentId);
        if (instrument == null) {
            throw new NoSuchElementException("No instrument with id " + instrumentId);
        }
        return instrument;
    }

    /**
     * Returns an instrument by its ticker symbol.
     *
     * @param ticker instrument ticker symbol
     * @return matching instrument
     * @throws IllegalArgumentException if the ticker is blank
     * @throws NoSuchElementException if the instrument does not exist
     */
    public Instrument getInstrumentByTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker must not be blank");
        }
        Instrument instrument = instrumentMapper.findByTicker(ticker);
        if (instrument == null) {
            throw new NoSuchElementException("No instrument with ticker " + ticker);
        }
        return instrument;
    }

    /**
     * Returns all known instruments.
     *
     * @return list of instrument reference records
     */
    public List<Instrument> getAllInstruments() {
        return instrumentMapper.findAll();
    }

    /**
     * Returns the current price for an instrument ticker.
     *
     * @param ticker instrument ticker symbol
     * @return positive current market price
     * @throws IllegalArgumentException if the ticker is blank
     * @throws NoSuchElementException if the ticker does not map to a known instrument
     * @throws IllegalStateException if no valid market price is available
     */
    public BigDecimal getCurrentPrice(String ticker) {
        getInstrumentByTicker(ticker);
        BigDecimal price = marketDataClient.getPrice(ticker);
        if (price == null) {
            throw new IllegalStateException("No price available for " + ticker);
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invalid price " + price + " for " + ticker);
        }
        return price;
    }

    /**
     * Returns the current price for an instrument identifier.
     *
     * @param instrumentId instrument identifier
     * @return positive current market price
     * @throws NoSuchElementException if the instrument does not exist
     * @throws IllegalStateException if no valid market price is available
     */
    public BigDecimal getCurrentPrice(Long instrumentId) {
        return getCurrentPrice(getInstrumentById(instrumentId).getTicker());
    }

    /**
     * Returns current prices for many instruments at once: cached prices from Redis, and the rest from the
     * market data API in batches. Instruments with no valid price right now are left out.
     *
     * @param instruments instruments to price
     * @return positive prices by ticker; empty if the market data API is unavailable
     */
    public Map<String, BigDecimal> getCurrentPrices(List<Instrument> instruments) {
        List<String> tickers = instruments.stream().map(Instrument::getTicker).distinct().toList();
        try {
            Map<String, BigDecimal> prices = new HashMap<>(marketDataClient.getPrices(tickers));
            prices.values().removeIf(price -> price.compareTo(BigDecimal.ZERO) <= 0);
            return prices;
        } catch (IllegalStateException | RestClientException e) {
            // listing instruments still works without prices (bad key, quota used up, API down)
            LOGGER.warn("No market prices for {} instruments: {}", tickers.size(), e.getMessage());
            return Map.of();
        }
    }
}
