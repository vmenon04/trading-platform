package com.neueda.leap.service;

import com.neueda.leap.repository.AccountHoldingMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountHoldingServiceTest {

    private static final int ACCOUNT_ID = 1;
    private static final int INSTRUMENT_ID = 10;

    @Mock
    private AccountHoldingMapper accountHoldingMapper;

    @InjectMocks
    private AccountHoldingService accountHoldingService;

    // Compares by value, ignoring scale (so 150 matches 150.00000000)
    private static BigDecimal amountEqualTo(String expected) {
        return argThat(actual -> actual != null && actual.compareTo(new BigDecimal(expected)) == 0);
    }

    private void verifyNothingInserted() {
        verify(accountHoldingMapper, never()).insertSnapshot(anyInt(), anyInt(), any(), any());
    }

    @Test
    void getQuantityReturnsLatestQuantity() {
        when(accountHoldingMapper.findLatestQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(new BigDecimal("100"));
        assertEquals(0, new BigDecimal("100").compareTo(accountHoldingService.getQuantity(ACCOUNT_ID, INSTRUMENT_ID)));
    }

    @Test
    void getQuantityReturnsZeroWhenNeverHeld() {
        when(accountHoldingMapper.findLatestQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(null);
        assertEquals(0, BigDecimal.ZERO.compareTo(accountHoldingService.getQuantity(ACCOUNT_ID, INSTRUMENT_ID)));
    }

    @Test
    void addQuantityInsertsIncreasedSnapshot() {
        when(accountHoldingMapper.findLatestQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(new BigDecimal("100"));
        accountHoldingService.addQuantity(ACCOUNT_ID, INSTRUMENT_ID, new BigDecimal("50"));
        verify(accountHoldingMapper).insertSnapshot(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), amountEqualTo("150"), eq("active"));
    }

    @Test
    void addQuantityStartsFromZeroForNewHolding() {
        when(accountHoldingMapper.findLatestQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(null);
        accountHoldingService.addQuantity(ACCOUNT_ID, INSTRUMENT_ID, new BigDecimal("50"));
        verify(accountHoldingMapper).insertSnapshot(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), amountEqualTo("50"), eq("active"));
    }

    @Test
    void addQuantityRejectsNonPositive() {
        assertThrows(IllegalArgumentException.class,
                () -> accountHoldingService.addQuantity(ACCOUNT_ID, INSTRUMENT_ID, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> accountHoldingService.addQuantity(ACCOUNT_ID, INSTRUMENT_ID, new BigDecimal("-5")));
        verifyNoInteractions(accountHoldingMapper);
    }

    @Test
    void removeQuantityInsertsDecreasedSnapshot() {
        when(accountHoldingMapper.findLatestQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(new BigDecimal("100"));
        accountHoldingService.removeQuantity(ACCOUNT_ID, INSTRUMENT_ID, new BigDecimal("30"));
        verify(accountHoldingMapper).insertSnapshot(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), amountEqualTo("70"), eq("active"));
    }

    @Test
    void removeQuantityMarksInactiveOnFullSell() {
        when(accountHoldingMapper.findLatestQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(new BigDecimal("100"));
        accountHoldingService.removeQuantity(ACCOUNT_ID, INSTRUMENT_ID, new BigDecimal("100"));
        verify(accountHoldingMapper).insertSnapshot(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), amountEqualTo("0"), eq("inactive"));
    }

    @Test
    void removeQuantityThrowsWhenInsufficient() {
        when(accountHoldingMapper.findLatestQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(new BigDecimal("100"));
        assertThrows(IllegalStateException.class,
                () -> accountHoldingService.removeQuantity(ACCOUNT_ID, INSTRUMENT_ID, new BigDecimal("100.5")));
        verifyNothingInserted();
    }

    @Test
    void removeQuantityThrowsWhenNeverHeld() {
        when(accountHoldingMapper.findLatestQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(null);
        assertThrows(IllegalStateException.class,
                () -> accountHoldingService.removeQuantity(ACCOUNT_ID, INSTRUMENT_ID, new BigDecimal("10")));
        verifyNothingInserted();
    }

    @Test
    void removeQuantityRejectsNonPositive() {
        assertThrows(IllegalArgumentException.class,
                () -> accountHoldingService.removeQuantity(ACCOUNT_ID, INSTRUMENT_ID, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> accountHoldingService.removeQuantity(ACCOUNT_ID, INSTRUMENT_ID, new BigDecimal("-5")));
        verifyNoInteractions(accountHoldingMapper);
    }
}
