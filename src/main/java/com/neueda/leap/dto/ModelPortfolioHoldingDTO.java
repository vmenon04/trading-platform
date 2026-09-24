package com.neueda.leap.dto;

public class ModelPortfolioHoldingDTO {
    private int modelPortfolioId;
    private int instrumentId;
    private String effectiveDate;
    private double targetWeightPct;
    private String status;

    public int getModelPortfolioId() {
        return modelPortfolioId;
    }

    public int getInstrumentId() {
        return instrumentId;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public double getTargetWeightPct() {
        return targetWeightPct;
    }

    public void setTargetWeightPct(double targetWeightPct) {
        this.targetWeightPct = targetWeightPct;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
