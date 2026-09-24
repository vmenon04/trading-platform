package com.neueda.leap.entity;

import java.util.HashMap;

public class ModelPortfolio {
    private Long modelPortfolioId;
    private String name;
    private HashMap<ModelPortfolioHolding, Double> holdings;

    public ModelPortfolio(Long modelPortfolioId, String name, HashMap<ModelPortfolioHolding, Double> holdings) {
        this.modelPortfolioId = modelPortfolioId;
        this.name = name;
        this.holdings = holdings;
    }

    public Long getModelPortfolioId() {
        return modelPortfolioId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<ModelPortfolioHolding, Double> getHoldings() {
        return holdings;
    }
}
