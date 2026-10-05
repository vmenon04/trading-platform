package com.neueda.leap.entity;

import com.neueda.leap.entity.AccountHolding.HoldingStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AccountHoldingTest {

    @Test
    void noArgsConstructorLeavesFieldsAtJavaDefaults() {
        AccountHolding holding = new AccountHolding();

        assertAll(
                () -> assertEquals(0, holding.getAccountId()),
                () -> assertEquals(0, holding.getInstrumentId()),
                () -> assertNull(holding.getAsOfDate()),
                () -> assertEquals(0.0, holding.getQuantity()),
                () -> assertNull(holding.getStatus())
        );
    }

    @Test
    void parameterizedConstructorSetsProvidedValues() {
        LocalDate date = LocalDate.of(2026, 9, 25);
        AccountHolding holding = new AccountHolding(101, 1, date, 100.5, HoldingStatus.ACTIVE);

        assertAll(
                () -> assertEquals(101, holding.getAccountId()),
                () -> assertEquals(1, holding.getInstrumentId()),
                () -> assertEquals(date, holding.getAsOfDate()),
                () -> assertEquals(100.5, holding.getQuantity()),
                () -> assertEquals(HoldingStatus.ACTIVE, holding.getStatus())
        );
    }

    @Test
    void settersUpdateFields() {
        AccountHolding holding = new AccountHolding();
        LocalDate date = LocalDate.of(2025, 1, 1);

        holding.setAccountId(200);
        holding.setInstrumentId(300);
        holding.setAsOfDate(date);
        holding.setQuantity(55.75);
        holding.setStatus(HoldingStatus.INACTIVE);

        assertAll(
                () -> assertEquals(200, holding.getAccountId()),
                () -> assertEquals(300, holding.getInstrumentId()),
                () -> assertEquals(date, holding.getAsOfDate()),
                () -> assertEquals(55.75, holding.getQuantity()),
                () -> assertEquals(HoldingStatus.INACTIVE, holding.getStatus())
        );
    }
}