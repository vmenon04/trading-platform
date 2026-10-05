package com.neueda.leap.entity;

import com.neueda.leap.enums.Status;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AccountHoldingTest {

    @Test
    void noArgsConstructorLeavesFieldsAtJavaDefaults() {
        AccountHolding holding = new AccountHolding();

        assertAll(
                () -> assertEquals(0, holding.getAccountId()),
                () -> assertEquals(0, holding.getInstrumentId()),
                () -> assertNull(holding.getAsOfDate()),
                () -> assertNull(holding.getQuantity()),
                () -> assertNull(holding.getStatus())
        );
    }

    @Test
    void parameterizedConstructorSetsProvidedValues() {
        OffsetDateTime date = OffsetDateTime.now();
        AccountHolding holding = new AccountHolding(101, 1, date, new BigDecimal("100.5"), Status.ACTIVE);

        assertAll(
                () -> assertEquals(101, holding.getAccountId()),
                () -> assertEquals(1, holding.getInstrumentId()),
                () -> assertEquals(date, holding.getAsOfDate()),
                () -> assertEquals(BigDecimal.valueOf(100.5), holding.getQuantity()),
                () -> assertEquals(Status.ACTIVE, holding.getStatus())
        );
    }

    @Test
    void settersUpdateFields() {
        AccountHolding holding = new AccountHolding();
        OffsetDateTime date = OffsetDateTime.now();

        holding.setAccountId(200);
        holding.setInstrumentId(300);
        holding.setAsOfDate(date);
        holding.setQuantity(new BigDecimal("55.75"));
        holding.setStatus(Status.INACTIVE);

        assertAll(
                () -> assertEquals(200, holding.getAccountId()),
                () -> assertEquals(300, holding.getInstrumentId()),
                () -> assertEquals(date, holding.getAsOfDate()),
                () -> assertEquals(BigDecimal.valueOf(55.75), holding.getQuantity()),
                () -> assertEquals(Status.INACTIVE, holding.getStatus())
        );
    }
}