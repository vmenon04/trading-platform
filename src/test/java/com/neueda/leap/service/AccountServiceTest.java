package com.neueda.leap.service;

import com.neueda.leap.repository.AccountMapper;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
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
        when(accountMapper.findBalance(1L)).thenReturn(new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("1000.00"), accountService.getBalance(1L));
    }

    @Test
    void getBalanceThrowsWhenAccountNotFound() {
        when(accountMapper.findBalance(99L)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> accountService.getBalance(99L));
    }

    @Test
    void depositPassesAmountToMapper() {
        when(accountMapper.increaseBalance(eq(1L), amountEqualTo("250.50"))).thenReturn(1);
        accountService.deposit(1L, new BigDecimal("250.50"));
        verify(accountMapper).increaseBalance(eq(1L), amountEqualTo("250.50"));
    }

    @Test
    void depositRejectsNonPositiveAmount() {
        BigDecimal negative = new BigDecimal("-5");
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(1L, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(1L, negative));
        verifyNoInteractions(accountMapper);
    }

    @Test
    void depositThrowsWhenAccountNotFound() {
        when(accountMapper.increaseBalance(eq(99L), any())).thenReturn(0);
        BigDecimal amount = new BigDecimal("100");
        assertThrows(NoSuchElementException.class, () -> accountService.deposit(99L, amount));
    }

    @Test
    void withdrawPassesAmountToMapper() {
        when(accountMapper.reduceBalance(eq(1L), amountEqualTo("250.50"))).thenReturn(1);
        accountService.withdraw(1L, new BigDecimal("250.50"));
        verify(accountMapper).reduceBalance(eq(1L), amountEqualTo("250.50"));
    }

    @Test
    void withdrawRejectsNonPositiveAmount() {
        BigDecimal negative = new BigDecimal("-5");
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(1L, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(1L, negative));
        verifyNoInteractions(accountMapper);
    }

    @Test
    void withdrawThrowsWhenInsufficientFunds() {
        when(accountMapper.reduceBalance(eq(1L), any())).thenReturn(0);
        when(accountMapper.findBalance(1L)).thenReturn(new BigDecimal("1000.00"));
        BigDecimal amount = new BigDecimal("1000.01");
        assertThrows(IllegalStateException.class, () -> accountService.withdraw(1L, amount));
    }

    @Test
    void withdrawThrowsWhenAccountNotFound() {
        when(accountMapper.reduceBalance(eq(99L), any())).thenReturn(0);
        when(accountMapper.findBalance(99L)).thenReturn(null);
        BigDecimal amount = new BigDecimal("100");
        assertThrows(NoSuchElementException.class, () -> accountService.withdraw(99L, amount));
    }

    @Test
    void purchasePassesAmountToMapper() {
        when(accountMapper.reduceBalance(eq(1L), amountEqualTo("250.50"))).thenReturn(1);
        accountService.purchase(1L, new BigDecimal("250.50"));
        verify(accountMapper).reduceBalance(eq(1L), amountEqualTo("250.50"));
    }

    @Test
    void purchaseRejectsNonPositiveAmount() {
        BigDecimal negative = new BigDecimal("-5");
        assertThrows(IllegalArgumentException.class, () -> accountService.purchase(1L, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> accountService.purchase(1L, negative));
        verifyNoInteractions(accountMapper);
    }

    @Test
    void purchaseThrowsWhenInsufficientFunds() {
        when(accountMapper.reduceBalance(eq(1L), any())).thenReturn(0);
        when(accountMapper.findBalance(1L)).thenReturn(new BigDecimal("1000.00"));
        BigDecimal amount = new BigDecimal("1000.01");
        assertThrows(IllegalStateException.class, () -> accountService.purchase(1L, amount));
    }

    @Test
    void purchaseThrowsWhenAccountNotFound() {
        when(accountMapper.reduceBalance(eq(99L), any())).thenReturn(0);
        when(accountMapper.findBalance(99L)).thenReturn(null);
        BigDecimal amount = new BigDecimal("100");
        assertThrows(NoSuchElementException.class, () -> accountService.purchase(99L, amount));
    }

    @Test
    void sellPassesAmountToMapper() {
        when(accountMapper.increaseBalance(eq(1L), amountEqualTo("250.50"))).thenReturn(1);
        accountService.sell(1L, new BigDecimal("250.50"));
        verify(accountMapper).increaseBalance(eq(1L), amountEqualTo("250.50"));
    }

    @Test
    void sellRejectsNonPositiveAmount() {
        BigDecimal negative = new BigDecimal("-5");
        assertThrows(IllegalArgumentException.class, () -> accountService.sell(1L, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> accountService.sell(1L, negative));
        verifyNoInteractions(accountMapper);
    }

    @Test
    void sellThrowsWhenAccountNotFound() {
        when(accountMapper.increaseBalance(eq(99L), any())).thenReturn(0);
        BigDecimal amount = new BigDecimal("100");
        assertThrows(NoSuchElementException.class, () -> accountService.sell(99L, amount));
    }

    @Test
    void getAccountIdByExternalAccountIdReturnsIdFromMapper() {
        UUID externalAccountId = UUID.randomUUID();
        when(accountMapper.findAccountIdByExternalAccountId(externalAccountId)).thenReturn(1L);
        assertEquals(1L, accountService.getAccountIdByExternalAccountId(externalAccountId));
    }

    @Test
    void getAccountIdByExternalAccountIdThrowsWhenAccountNotFound() {
        UUID externalAccountId = UUID.randomUUID();
        when(accountMapper.findAccountIdByExternalAccountId(externalAccountId)).thenReturn(null);
        assertThrows(NoSuchElementException.class,
                () -> accountService.getAccountIdByExternalAccountId(externalAccountId));
    }

    @Test
    void rejectsNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(1L, null));
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(1L, null));
        verifyNoInteractions(accountMapper);
    }
}
