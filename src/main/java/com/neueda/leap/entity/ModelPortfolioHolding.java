package com.neueda.leap.entity;

import java.math.BigDecimal;

import java.time.LocalDate;

public class ModelPortfolioHolding {

    public enum ModelPortfolioStatus {
        ACTIVE, INACTIVE
    }

    private Long modelPortfolioId;
    private Long instrumentId;
    private LocalDate effectiveDate;
    private BigDecimal targetWeightPct;
    private ModelPortfolioStatus status;

    public ModelPortfolioHolding() {
    }

    public ModelPortfolioHolding(Long modelPortfolioId, Long instrumentId, LocalDate effectiveDate, BigDecimal targetWeightPct) {
        this.modelPortfolioId = modelPortfolioId;
        this.instrumentId = instrumentId;
        this.effectiveDate = effectiveDate;
        this.targetWeightPct = targetWeightPct;
        this.status = ModelPortfolioStatus.ACTIVE;
    }

    public Long getModelPortfolioId() {
        return modelPortfolioId;
    }

    public void setModelPortfolioId(Long modelPortfolioId) {
        this.modelPortfolioId = modelPortfolioId;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Long instrumentId) {
        this.instrumentId = instrumentId;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public BigDecimal getTargetWeightPct() {
        return targetWeightPct;
    }

    public void setTargetWeightPct(BigDecimal targetWeightPct) {
        this.targetWeightPct = targetWeightPct;
    }

    public ModelPortfolioStatus getStatus() {
        return status;
    }

    public void setStatus(ModelPortfolioStatus status) {
        this.status = status;
    }
}
