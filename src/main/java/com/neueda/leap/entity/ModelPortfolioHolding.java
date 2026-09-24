package com.neueda.leap.entity;

import java.time.LocalDate;

public class ModelPortfolioHolding {

    public enum ModelPortfolioStatus {
        ACTIVE, INACTIVE
    };

    private Long modelPortfolioId;
    private Long instrumentId;
    private LocalDate effectiveDate;
    private double targetWeightPct;
    private ModelPortfolioStatus status;

    public ModelPortfolioHolding(Long instrumentId, LocalDate effectiveDate, double targetWeightPct, ModelPortfolioStatus status) {
        this.modelPortfolioId = null;
        this.instrumentId = instrumentId;
        this.effectiveDate = effectiveDate;
        this.targetWeightPct = targetWeightPct;
        this.status = status;
    }

    public Long getModelPortfolioId() {
        return modelPortfolioId;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public double getTargetWeightPct() {
        return targetWeightPct;
    }

    public ModelPortfolioStatus getStatus() {
        return status;
    }

    public void setStatus(ModelPortfolioStatus status) {
        this.status = status;
    }
}
