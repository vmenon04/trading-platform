package com.neueda.leap.service;

import com.neueda.leap.repository.AccountMapper;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    // Compares by value, ignoring scale (so 1250.5 matches 1250.50)
    private static BigDecimal amountEqualTo(String expected) {
        return argThat(actual -> actual.compareTo(new BigDecimal(expected)) == 0);
    }

    @Test
    void getBalanceReturnsBalanceFromMapper() {
        when(accountMapper.findBalance(1)).thenReturn(new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("1000.00"), accountService.getBalance(1));
    }

    @Test
    void getBalanceThrowsWhenAccountNotFound() {
        when(accountMapper.findBalance(99)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> accountService.getBalance(99));
    }

    @Test
    void depositAddsToBalance() {
        when(accountMapper.findBalance(1)).thenReturn(new BigDecimal("1000.00"));
        accountService.deposit(1, new BigDecimal("250.50"));
        verify(accountMapper).updateBalance(eq(1), amountEqualTo("1250.50"));
    }

    @Test
    void depositRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(1, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(1, new BigDecimal("-5")));
        verifyNoInteractions(accountMapper);
    }

    @Test
    void depositThrowsWhenAccountNotFound() {
        when(accountMapper.findBalance(99)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> accountService.deposit(99, new BigDecimal("100")));
        verify(accountMapper, never()).updateBalance(anyInt(), any());
    }

    @Test
    void withdrawSubtractsFromBalance() {
        when(accountMapper.findBalance(1)).thenReturn(new BigDecimal("1000.00"));
        accountService.withdraw(1, new BigDecimal("250.50"));
        verify(accountMapper).updateBalance(eq(1), amountEqualTo("749.50"));
    }

    @Test
    void withdrawAllowsExactBalance() {
        when(accountMapper.findBalance(1)).thenReturn(new BigDecimal("1000.00"));
        accountService.withdraw(1, new BigDecimal("1000.00"));
        verify(accountMapper).updateBalance(eq(1), amountEqualTo("0"));
    }

    @Test
    void withdrawThrowsWhenInsufficientFunds() {
        when(accountMapper.findBalance(1)).thenReturn(new BigDecimal("1000.00"));
        assertThrows(IllegalStateException.class, () -> accountService.withdraw(1, new BigDecimal("1000.01")));
        verify(accountMapper, never()).updateBalance(anyInt(), any());
    }

    @Test
    void withdrawRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(1, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(1, new BigDecimal("-5")));
        verifyNoInteractions(accountMapper);
    }

    @Test
    void withdrawThrowsWhenAccountNotFound() {
        when(accountMapper.findBalance(99)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> accountService.withdraw(99, new BigDecimal("100")));
        verify(accountMapper, never()).updateBalance(anyInt(), any());
    }
}
