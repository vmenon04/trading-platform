package com.neueda.leap.entity;

import com.neueda.leap.entity.ModelPortfolioHolding.ModelPortfolioStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ModelPortfolioHoldingTest {

    @Test
    void noArgsConstructorLeavesFieldsAtDefaults() {
        ModelPortfolioHolding holding = new ModelPortfolioHolding();

        assertAll(
                () -> assertNull(holding.getModelPortfolioId()),
                () -> assertNull(holding.getInstrumentId()),
                () -> assertNull(holding.getEffectiveDate()),
                () -> assertEquals(0.0, holding.getTargetWeightPct()),
                () -> assertNull(holding.getStatus())
        );
    }

    @Test
    void parameterizedConstructorSetsProvidedValuesAndDefaultsStatusToActive() {
        LocalDate effectiveDate = LocalDate.of(2026, 9, 25);
        ModelPortfolioHolding holding = new ModelPortfolioHolding(1L, 2L, effectiveDate, 35.5);

        assertAll(
                () -> assertEquals(1L, holding.getModelPortfolioId()),
                () -> assertEquals(2L, holding.getInstrumentId()),
                () -> assertEquals(effectiveDate, holding.getEffectiveDate()),
                () -> assertEquals(35.5, holding.getTargetWeightPct()),
                () -> assertEquals(ModelPortfolioStatus.ACTIVE, holding.getStatus())
        );
    }

    @Test
    void settersUpdateAllMutableFields() {
        ModelPortfolioHolding holding = new ModelPortfolioHolding();
        LocalDate effectiveDate = LocalDate.of(2024, 12, 31);

        holding.setModelPortfolioId(10L);
        holding.setInstrumentId(20L);
        holding.setEffectiveDate(effectiveDate);
        holding.setTargetWeightPct(60.0);
        holding.setStatus(ModelPortfolioStatus.INACTIVE);

        assertAll(
                () -> assertEquals(10L, holding.getModelPortfolioId()),
                () -> assertEquals(20L, holding.getInstrumentId()),
                () -> assertEquals(effectiveDate, holding.getEffectiveDate()),
                () -> assertEquals(60.0, holding.getTargetWeightPct()),
                () -> assertEquals(ModelPortfolioStatus.INACTIVE, holding.getStatus())
        );
    }
}