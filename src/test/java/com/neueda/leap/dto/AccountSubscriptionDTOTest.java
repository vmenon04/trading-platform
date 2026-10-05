package com.neueda.leap.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;

@DisplayName("AccountSubscriptionDTO Tests")
class AccountSubscriptionDTOTest {
    @Test
    @DisplayName("Should create AccountSubscriptionDTO with valid data")
    void testValidAccountSubscriptionDTO() {
        UUID accountId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        AccountSubscriptionDTO dto = new AccountSubscriptionDTO(accountId, 2, "2026-09-25", "ACTIVE");
        
        assertEquals(accountId, dto.accountId());
        assertEquals(2, dto.modelPortfolioId());
        assertEquals("2026-09-25", dto.subscriptionDate());
        assertEquals("ACTIVE", dto.status());
    }

    @Test
    @DisplayName("Should support record equality")
    void testRecordEquality() {
        UUID accountId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        AccountSubscriptionDTO dto1 = new AccountSubscriptionDTO(accountId, 2, "2026-09-25", "ACTIVE");
        AccountSubscriptionDTO dto2 = new AccountSubscriptionDTO(accountId, 2, "2026-09-25", "ACTIVE");
        AccountSubscriptionDTO dto3 = new AccountSubscriptionDTO(accountId, 2, "2026-09-25", "INACTIVE");
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }
}

