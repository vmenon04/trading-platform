package com.neueda.leap.entity;

import com.neueda.leap.enums.Status;
import com.neueda.leap.enums.TradeStatus;

import java.math.BigDecimal;

import java.time.LocalDate;

public class ModelPortfolioHolding {

    private Long modelPortfolioId;
    private Long instrumentId;
    private LocalDate effectiveDate;
    private BigDecimal targetWeightPct;
    private Status status;

    public ModelPortfolioHolding() {
    }

    public ModelPortfolioHolding(Long modelPortfolioId, Long instrumentId, LocalDate effectiveDate, BigDecimal targetWeightPct) {
        this.modelPortfolioId = modelPortfolioId;
        this.instrumentId = instrumentId;
        this.effectiveDate = effectiveDate;
        this.targetWeightPct = targetWeightPct;
        this.status = Status.ACTIVE;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
