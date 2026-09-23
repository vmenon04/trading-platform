package com.neueda.leap.entity;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

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