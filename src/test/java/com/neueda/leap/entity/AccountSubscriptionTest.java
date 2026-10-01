package com.neueda.leap.entity;

import com.neueda.leap.entity.AccountSubscription.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AccountSubscriptionTest {

    @Test
    void noArgsConstructorLeavesFieldsAtDefaults() {
        AccountSubscription subscription = new AccountSubscription();

        assertAll(
                () -> assertEquals(0, subscription.getAccountId()),
                () -> assertEquals(0, subscription.getModelPortfolioId()),
                () -> assertNull(subscription.getSubscriptionDate()),
                () -> assertNull(subscription.getStatus())
        );
    }

    @Test
    void parameterizedConstructorSetsIdsAndDateAndDefaultsStatusToActive() {
        LocalDate date = LocalDate.of(2026, 9, 25);
        AccountSubscription subscription = new AccountSubscription(101, 7, date);

        assertAll(
                () -> assertEquals(101, subscription.getAccountId()),
                () -> assertEquals(7, subscription.getModelPortfolioId()),
                () -> assertEquals(date, subscription.getSubscriptionDate()),
                () -> assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus())
        );
    }

    @Test
    void settersUpdateAllMutableFields() {
        AccountSubscription subscription = new AccountSubscription();
        LocalDate date = LocalDate.of(2025, 6, 30);

        subscription.setAccountId(11);
        subscription.setModelPortfolioId(22);
        subscription.setSubscriptionDate(date);
        subscription.setStatus(SubscriptionStatus.INACTIVE);

        assertAll(
                () -> assertEquals(11, subscription.getAccountId()),
                () -> assertEquals(22, subscription.getModelPortfolioId()),
                () -> assertEquals(date, subscription.getSubscriptionDate()),
                () -> assertEquals(SubscriptionStatus.INACTIVE, subscription.getStatus())
        );
    }
}
