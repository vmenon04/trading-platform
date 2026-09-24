package com.neueda.leap.service;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.repository.InstrumentMapper;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class InstrumentService {

    private final InstrumentMapper instrumentMapper;

    public InstrumentService(InstrumentMapper instrumentMapper) {
        this.instrumentMapper = instrumentMapper;
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
}
