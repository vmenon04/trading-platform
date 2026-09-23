package com.neueda.leap;

import com.neueda.leap.entity.Instrument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InstrumentTest {

    private Instrument instrument;

    @BeforeEach
    void setUp() {
        instrument = new Instrument(1, "Apple Inc.", "AAPL");
    }

    // BR-12: Platform pricing various instrument classes
    @Test
    void testEquityInstrumentCanBeCreated() {
        Instrument equity = new Instrument(101, "Microsoft", "MSFT", InstrumentType.EQUITY);
        assertEquals("MSFT", equity.getTicker());
        assertEquals(InstrumentType.EQUITY, equity.getType());
    }

    @Test
    void testForexInstrumentCanBeCreated() {
        Instrument forex = new Instrument(201, "Euro/Dollar", "EURUSD", InstrumentType.FOREX);
        assertEquals(InstrumentType.FOREX, forex.getType());
    }

    @Test
    void testCryptoInstrumentCanBeCreated() {
        Instrument crypto = new Instrument(301, "Bitcoin", "BTC", InstrumentType.CRYPTO);
        assertEquals(InstrumentType.CRYPTO, crypto.getType());
    }

    @Test
    void testInstrumentCannotHaveNullTicker() {
        assertThrows(IllegalArgumentException.class,
                () -> new Instrument(1, "Invalid", null));
    }

    @Test
    void testInstrumentCannotHaveEmptyName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Instrument(1, "", "TEST"));
    }

    // BR-12: Current market quotes for execution
    @Test
    void testInstrumentCanFetchCurrentMarketQuote() {
        MarketQuote quote = instrument.getCurrentMarketQuote();
        assertNotNull(quote);
    }
}