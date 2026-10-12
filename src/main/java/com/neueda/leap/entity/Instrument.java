package com.neueda.leap.entity;

public class Instrument {

    // match the values stored in instruments.asset_class (MyBatis maps enums by name)
    public enum InstrumentType {
        STOCK,
        ETF,
        BOND,
        FOREX,
        CRYPTO
    }
    private Long instrumentId;
    private String name;
    private String ticker;
    private InstrumentType instrumentType;

    public Instrument() {
    }

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

    public void setInstrumentType(InstrumentType instrumentType) {
        this.instrumentType = instrumentType;
    }
}
