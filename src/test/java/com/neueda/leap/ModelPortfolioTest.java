package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class ModelPortfolioTest {

    private ModelPortfolio modelPortfolio;

    @BeforeEach
    void setUp() {
        modelPortfolio = new ModelPortfolio(1, "Conservative Income");
    }

    @Test
    void testModelPortfolioCanBeCreated() {
        assertNotNull(modelPortfolio);
        assertEquals(1, modelPortfolio.getModelPortfolioId());
        assertEquals("Conservative Income", modelPortfolio.getName());
    }

    @Test
    void testModelPortfolioCanContainHoldings() {
        Instrument bond = new Instrument(1, "US Treasury Bond", "UST");
        ModelPortfolioHolding holding = new ModelPortfolioHolding(
                1, 1, LocalDate.now(), 40.0, HoldingStatus.ACTIVE);

        modelPortfolio.addHolding(holding);
        assertEquals(1, modelPortfolio.getHoldings().size());
    }

    @Test
    void testModelPortfolioWeightsSum() {
        // Holdings should sum to 100% target weight
        ModelPortfolioHolding h1 = new ModelPortfolioHolding(1, 1, LocalDate.now(), 40.0, HoldingStatus.ACTIVE);
        ModelPortfolioHolding h2 = new ModelPortfolioHolding(1, 2, LocalDate.now(), 60.0, HoldingStatus.ACTIVE);

        modelPortfolio.addHolding(h1);
        modelPortfolio.addHolding(h2);

        assertEquals(100.0, modelPortfolio.getTotalTargetWeight());
    }
}

public class ModelPortfolioHoldingTest {

    private ModelPortfolioHolding holding;

    @BeforeEach
    void setUp() {
        holding = new ModelPortfolioHolding(1, 1, LocalDate.now(), 35.5, HoldingStatus.ACTIVE);
    }

    @Test
    void testModelPortfolioHoldingCanBeCreated() {
        assertEquals(1, holding.getModelPortfolioId());
        assertEquals(1, holding.getInstrumentId());
        assertEquals(35.5, holding.getTargetWeightPct());
    }

    @Test
    void testTargetWeightMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModelPortfolioHolding(1, 1, LocalDate.now(), -10.0, HoldingStatus.ACTIVE));
    }

    @Test
    void testTargetWeightCannotExceed100Percent() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModelPortfolioHolding(1, 1, LocalDate.now(), 150.0, HoldingStatus.ACTIVE));
    }

    @Test
    void testEffectiveDateCanBeInTheFuture() {
        LocalDate futureDate = LocalDate.now().plusMonths(1);
        ModelPortfolioHolding futureHolding = new ModelPortfolioHolding(
                1, 1, futureDate, 25.0, HoldingStatus.ACTIVE);
        assertEquals(futureDate, futureHolding.getEffectiveDate());
    }
}