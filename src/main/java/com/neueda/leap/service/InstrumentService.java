package com.neueda.leap.service;

import com.neueda.leap.entity.Instrument;
import com.neueda.leap.repository.InstrumentMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InstrumentService {

    private final InstrumentMapper instrumentMapper;

    public InstrumentService(InstrumentMapper instrumentMapper) {
        this.instrumentMapper = instrumentMapper;
    }

    public Instrument getInstrumentById(int instrumentId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Instrument getInstrumentByTicker(String ticker) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<Instrument> getAllInstruments() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
