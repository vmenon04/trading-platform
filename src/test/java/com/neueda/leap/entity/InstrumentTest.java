package com.neueda.leap.entity;

import com.neueda.leap.entity.Instrument.InstrumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InstrumentTest {

    private Instrument instrument;

    @BeforeEach
    void setUp() {
        instrument = new Instrument("Apple Inc.", "AAPL", InstrumentType.EQUITY);
    }

    // BR-12: Platform pricing various instrument classes
    @Test
    void testEquityInstrumentCanBeCreated() {
        Instrument equity = new Instrument("Microsoft", "MSFT", InstrumentType.EQUITY);
        assertEquals("MSFT", equity.getTicker());
        assertEquals(InstrumentType.EQUITY, equity.getInstrumentType());
    }

    @Test
    void testForexInstrumentCanBeCreated() {
        Instrument forex = new Instrument("Euro/Dollar", "EURUSD", InstrumentType.FOREX);
        assertEquals(InstrumentType.FOREX, forex.getInstrumentType());
    }

    @Test
    void testCryptoInstrumentCanBeCreated() {
        Instrument crypto = new Instrument("Bitcoin", "BTC", InstrumentType.CRYPTO);
        assertEquals(InstrumentType.CRYPTO, crypto.getInstrumentType());
    }

    @Test
    void testInstrumentCannotHaveNullTicker() {
        assertThrows(IllegalArgumentException.class,
                () -> new Instrument("Invalid", null, InstrumentType.EQUITY));
    }

    @Test
    void testInstrumentCannotHaveEmptyName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Instrument("", "", InstrumentType.EQUITY));
    }
}