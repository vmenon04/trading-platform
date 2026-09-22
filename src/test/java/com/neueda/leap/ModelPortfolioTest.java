package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class ModelPortfolioTest {

    private ModelPortfolio modelPortfolio;
    private int model_portfolio_id = 1;
    private String model_portfolio_name = "Conservative Income";

    @BeforeEach
    // ModelPortfolio(model_portfolio_id, model_portfolio_name)
    void setUp() {
        modelPortfolio = new ModelPortfolio(model_portfolio_id, model_portfolio_name);
    }

    @Test
    void testModelPortfolioCanBeCreated() {
        assertNotNull(modelPortfolio);
        assertEquals(model_portfolio_id, modelPortfolio.getModelPortfolioId());
        assertEquals(model_portfolio_name, modelPortfolio.getName());
    }
    @Test
    void testModelPortfolioNameCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModelPortfolio(model_portfolio_id, ""));
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
    void testTargetWeightMustBeNonZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModelPortfolioHolding(1, 1, LocalDate.now(), 0, HoldingStatus.ACTIVE));
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
}