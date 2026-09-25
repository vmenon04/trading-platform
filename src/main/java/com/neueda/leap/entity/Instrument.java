package com.neueda.leap.entity;

public class Instrument {

    public enum InstrumentType {
        EQUITY,
        BOND,
        FOREX,
        CRYPTO
    }
    private Long instrumentId;
    private String name;
    private String ticker;
    private final InstrumentType instrumentType;

    public Instrument(String name, String ticker, InstrumentType instrumentType) {
        instrumentId = null;
        this.name = name;
        this.ticker = ticker;
        this.instrumentType = instrumentType;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public void  setInstrumentId(Long instrumentId) {
        this.instrumentId = instrumentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public InstrumentType getInstrumentType() {
        return instrumentType;
    }
}
