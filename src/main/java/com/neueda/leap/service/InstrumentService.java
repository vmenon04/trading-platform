package com.neueda.leap.service;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.external.MarketDataClient;
import com.neueda.leap.repository.InstrumentMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class InstrumentService {

    private final InstrumentMapper instrumentMapper;
    private final MarketDataClient marketDataClient;

    public InstrumentService(InstrumentMapper instrumentMapper, MarketDataClient marketDataClient) {
        this.instrumentMapper = instrumentMapper;
        this.marketDataClient = marketDataClient;
    }

    public Instrument getInstrumentById(int instrumentId) {
        Instrument instrument = instrumentMapper.findById(instrumentId);
        if (instrument == null) {
            throw new NoSuchElementException("No instrument with id " + instrumentId);
        }
        return instrument;
    }

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

    public List<Instrument> getAllInstruments() {
        return instrumentMapper.findAll();
    }

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

    // note that we need Instrument.getTicker() from entity
    public BigDecimal getCurrentPrice(int instrumentId) {
        return getCurrentPrice(getInstrumentById(instrumentId).getTicker());
    }
}
