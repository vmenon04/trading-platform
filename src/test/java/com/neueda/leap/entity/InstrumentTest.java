package com.neueda.leap.entity;

import com.neueda.leap.entity.Instrument.InstrumentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

    @BeforeEach
    void setUp() {
        instrument = new Instrument("Apple Inc.", "AAPL", InstrumentType.STOCK);
    }

    @Test
    void testEquityInstrumentCanBeCreated() {
        Instrument equity = new Instrument("Microsoft", "MSFT", InstrumentType.STOCK);
        assertEquals("MSFT", equity.getTicker());
        assertEquals(InstrumentType.STOCK, equity.getInstrumentType());
    }

    @Test
    void parameterizedConstructorSetsProvidedValues() {
        Instrument equity = new Instrument("Microsoft", "MSFT", InstrumentType.EQUITY);

    @Test
    void testInstrumentCannotHaveNullTicker() {
        assertThrows(IllegalArgumentException.class,
                () -> new Instrument("Invalid", null, InstrumentType.STOCK));
    }

    @Test
    void testInstrumentCannotHaveEmptyName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Instrument("", "", InstrumentType.STOCK));
    }
}