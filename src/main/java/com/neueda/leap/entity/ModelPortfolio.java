package com.neueda.leap.entity;

import java.util.HashMap;

public class ModelPortfolio {
    private Long modelPortfolioId;
    private String name;
    private HashMap<ModelPortfolioHolding, Double> holdings;

    public ModelPortfolio() {
        this.holdings = new HashMap<>();
    }

    public Long getModelPortfolioId() {
        return modelPortfolioId;
    }


    public void  setModelPortfolioId(Long modelPortfolioId) {
        this.modelPortfolioId = modelPortfolioId;
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

    public void setHoldings(HashMap<ModelPortfolioHolding, Double> holdings) {
        this.holdings = holdings;
    }
}
