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
        AccountSubscriptionDTO dto = new AccountSubscriptionDTO(1L, 2L, "2026-09-25", "ACTIVE");
        
        assertEquals(1L, dto.accountId());
        assertEquals(2L, dto.modelPortfolioId());
        assertEquals("2026-09-25", dto.subscriptionDate());
        assertEquals("ACTIVE", dto.status());
    }

    @Test
    @DisplayName("Should support record equality")
    void testRecordEquality() {
        Long accountId = 1L;
        AccountSubscriptionDTO dto1 = new AccountSubscriptionDTO(accountId, 2L, "2026-09-25", "ACTIVE");
        AccountSubscriptionDTO dto2 = new AccountSubscriptionDTO(accountId, 2L, "2026-09-25", "ACTIVE");
        AccountSubscriptionDTO dto3 = new AccountSubscriptionDTO(accountId, 2L, "2026-09-25", "INACTIVE");
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }
}

