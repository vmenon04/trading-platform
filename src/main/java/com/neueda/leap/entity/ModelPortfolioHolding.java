package com.neueda.leap.entity;

import java.time.LocalDate;

public class ModelPortfolioHolding {

    public enum ModelPortfolioStatus {
        ACTIVE, INACTIVE
    }

    private final Long modelPortfolioId;
    private final Long instrumentId;
    private final LocalDate effectiveDate;
    private final double targetWeightPct;
    private ModelPortfolioStatus status;

    public ModelPortfolioHolding(Long modelPortfolioId, Long instrumentId, LocalDate effectiveDate, double targetWeightPct, ModelPortfolioStatus status) {
        this.modelPortfolioId = modelPortfolioId;
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
