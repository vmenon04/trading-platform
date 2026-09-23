package com.neueda.leap.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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