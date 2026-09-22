package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.plaf.SeparatorUI;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class AccountSubscriptionTest {

    private AccountSubscription subscription;

    private final int ACCOUNT_ID = 101;
    private final int PORTFOLIO_ID = 1;

    @BeforeEach
    void setUp() {
        //AccountSubscription(account_id, portfolio_id, status)
        subscription = new AccountSubscription(
                ACCOUNT_ID, PORTFOLIO_ID, SubscriptionStatus.ACTIVE);
    }

    @Test
    void testSubscriptionCanBeCreated() {
        assertNotNull(subscription);
        assertEquals(ACCOUNT_ID, subscription.getAccountId());
        assertEquals(PORTFOLIO_ID, subscription.getModelPortfolioId());
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    void testSubscriptionDateIsRecorded() {
        LocalDate start = LocalDate.now();
        AccountSubscription sub = new AccountSubscription(ACCOUNT_ID, PORTFOLIO_ID, SubscriptionStatus.ACTIVE);
        LocalDate end = LocalDate.now();
        assertAll(
                () -> assertTrue(auditedTrade.getCreatedTime().after(start) || auditedTrade.getCreatedTime().equals(start)),
                () -> assertTrue(auditedTrade.getCreatedTime().before(end) || auditedTrade.getCreatedTime().equals(end))
        );
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
        AccountSubscription sub1 = new AccountSubscription(ACCOUNT_ID, PORTFOLIO_ID, SubscriptionStatus.ACTIVE);
        AccountSubscription sub2 = new AccountSubscription(ACCOUNT_ID, 2, SubscriptionStatus.ACTIVE);

        assertEquals(ACCOUNT_ID, sub1.getAccountId());
        assertEquals(ACCOUNT_ID, sub2.getAccountId());
        assertNotEquals(sub1.getModelPortfolioId(), sub2.getModelPortfolioId());
    }

    @Test
    void testAccountCannotSubscribeToSamePortfolio() {
        assertEquals(ACCOUNT_ID, subscription.getAccountId());
        assertThrows(AlreadySubscribedError.class,
                () -> new AccountSubscription(ACCOUNT_ID, PORTFOLIO_ID, SubscriptionStatus.ACTIVE));

    }

    @Test
    void testTrackSubscriptionHistory() {
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        String outputString = subscription.toString();
        assertEquals(outputString, subscription.getSubscriptionHistory());

        subscription.unsubscribe();
        assertEquals(SubscriptionStatus.INACTIVE, subscription.getStatus());
        outputString = outputString + ("\n" + subscription.toString());
        assertEquals(outputString, subscription.getSubscriptionHistory());

        subscription.subscribe();
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        outputString = outputString + ("\n" + subscription.toString());
        assertEquals(outputString, subscription.getSubscriptionHistory());

        assertEquals(3, subscription.getSubscriptionHistory().size());
    }
}