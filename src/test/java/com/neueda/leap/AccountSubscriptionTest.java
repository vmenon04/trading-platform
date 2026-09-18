package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class AccountSubscriptionTest {

    private AccountSubscription subscription;

    @BeforeEach
    void setUp() {
        //AccountSubscription(account_id, portfolio_id, subscription_date, status)
        subscription = new AccountSubscription(
                101, 1, LocalDate.now(), SubscriptionStatus.ACTIVE);
    }

    @Test
    void testSubscriptionCanBeCreated() {
        assertNotNull(subscription);
        assertEquals(101, subscription.getAccountId());
        assertEquals(1, subscription.getModelPortfolioId());
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    void testSubscriptionDateIsRecorded() {
        LocalDate subDate = LocalDate.now();
        AccountSubscription sub = new AccountSubscription(101, 1, subDate, SubscriptionStatus.ACTIVE);
        assertEquals(subDate, sub.getSubscriptionDate());
    }

    @Test
    void testUnsubscribe() {
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertNull(subscription.unsubscribeDate);
        subscription.unsubscribe();
        assertEquals(SubscriptionStatus.INACTIVE, subscription.getStatus());
        assertNotNull(subscription.getUnsubscribeDate());
    }

    @Test
    void testAccountCanSubscribeToMultipleModelPortfolios() {
        AccountSubscription sub1 = new AccountSubscription(101, 1, LocalDate.now(), SubscriptionStatus.ACTIVE);
        AccountSubscription sub2 = new AccountSubscription(101, 2, LocalDate.now(), SubscriptionStatus.ACTIVE);

        assertEquals(101, sub1.getAccountId());
        assertEquals(101, sub2.getAccountId());
        assertNotEquals(sub1.getModelPortfolioId(), sub2.getModelPortfolioId());
    }
}