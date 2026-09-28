package com.neueda.leap.service;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.external.MarketDataClient;
import com.neueda.leap.repository.InstrumentMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/**
 * Provides instrument lookups and current market price retrieval.
 */
@Service
public class InstrumentService {

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
    public Instrument getInstrumentById(int instrumentId) {
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
    public BigDecimal getCurrentPrice(int instrumentId) {
        return getCurrentPrice(getInstrumentById(instrumentId).getTicker());
    }
}
