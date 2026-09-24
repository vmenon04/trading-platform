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

    // Compares by value, ignoring scale (so 250.5 matches 250.50)
    private static BigDecimal amountEqualTo(String expected) {
        return argThat(actual -> actual != null && actual.compareTo(new BigDecimal(expected)) == 0);
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
    void depositPassesAmountToMapper() {
        when(accountMapper.increaseBalance(eq(1), amountEqualTo("250.50"))).thenReturn(1);
        accountService.deposit(1, new BigDecimal("250.50"));
        verify(accountMapper).increaseBalance(eq(1), amountEqualTo("250.50"));
    }

    @Test
    void depositRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(1, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(1, new BigDecimal("-5")));
        verifyNoInteractions(accountMapper);
    }

    @Test
    void depositThrowsWhenAccountNotFound() {
        when(accountMapper.increaseBalance(eq(99), any())).thenReturn(0);
        assertThrows(NoSuchElementException.class, () -> accountService.deposit(99, new BigDecimal("100")));
    }

    @Test
    void withdrawPassesAmountToMapper() {
        when(accountMapper.reduceBalance(eq(1), amountEqualTo("250.50"))).thenReturn(1);
        accountService.withdraw(1, new BigDecimal("250.50"));
        verify(accountMapper).reduceBalance(eq(1), amountEqualTo("250.50"));
    }

    @Test
    void withdrawRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(1, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(1, new BigDecimal("-5")));
        verifyNoInteractions(accountMapper);
    }

    @Test
    void withdrawThrowsWhenInsufficientFunds() {
        when(accountMapper.reduceBalance(eq(1), any())).thenReturn(0);
        when(accountMapper.findBalance(1)).thenReturn(new BigDecimal("1000.00"));
        assertThrows(IllegalStateException.class, () -> accountService.withdraw(1, new BigDecimal("1000.01")));
    }

    @Test
    void withdrawThrowsWhenAccountNotFound() {
        when(accountMapper.reduceBalance(eq(99), any())).thenReturn(0);
        when(accountMapper.findBalance(99)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> accountService.withdraw(99, new BigDecimal("100")));
    }

    @Test
    void purchasePassesAmountToMapper() {
        when(accountMapper.reduceBalance(eq(1), amountEqualTo("250.50"))).thenReturn(1);
        accountService.purchase(1, new BigDecimal("250.50"));
        verify(accountMapper).reduceBalance(eq(1), amountEqualTo("250.50"));
    }

    @Test
    void purchaseRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> accountService.purchase(1, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> accountService.purchase(1, new BigDecimal("-5")));
        verifyNoInteractions(accountMapper);
    }

    @Test
    void purchaseThrowsWhenInsufficientFunds() {
        when(accountMapper.reduceBalance(eq(1), any())).thenReturn(0);
        when(accountMapper.findBalance(1)).thenReturn(new BigDecimal("1000.00"));
        assertThrows(IllegalStateException.class, () -> accountService.purchase(1, new BigDecimal("1000.01")));
    }

    @Test
    void purchaseThrowsWhenAccountNotFound() {
        when(accountMapper.reduceBalance(eq(99), any())).thenReturn(0);
        when(accountMapper.findBalance(99)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> accountService.purchase(99, new BigDecimal("100")));
    }

    @Test
    void sellPassesAmountToMapper() {
        when(accountMapper.increaseBalance(eq(1), amountEqualTo("250.50"))).thenReturn(1);
        accountService.sell(1, new BigDecimal("250.50"));
        verify(accountMapper).increaseBalance(eq(1), amountEqualTo("250.50"));
    }

    @Test
    void sellRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> accountService.sell(1, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> accountService.sell(1, new BigDecimal("-5")));
        verifyNoInteractions(accountMapper);
    }

    @Test
    void sellThrowsWhenAccountNotFound() {
        when(accountMapper.increaseBalance(eq(99), any())).thenReturn(0);
        assertThrows(NoSuchElementException.class, () -> accountService.sell(99, new BigDecimal("100")));
    }
}
