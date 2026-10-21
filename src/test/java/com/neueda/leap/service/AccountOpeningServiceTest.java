package com.neueda.leap.service;

import com.neueda.leap.dto.OpenAccountRequestDTO;
import com.neueda.leap.repository.AccountOpeningMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountOpeningServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long CLIENT_ID = 11L;
    private static final Long ACCOUNT_ID = 21L;
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);

    @Mock
    private AccountOpeningMapper accountOpeningMapper;

    @InjectMocks
    private AccountOpeningService accountOpeningService;

    @Test
    void openAccount_createsClientAccountAndLinks() {
        when(accountOpeningMapper.insertClient("Ada", "Lovelace", "ada@example.com", BIRTH_DATE)).thenReturn(CLIENT_ID);
        when(accountOpeningMapper.linkUserToClient(USER_ID, CLIENT_ID)).thenReturn(1);
        when(accountOpeningMapper.insertAccount()).thenReturn(ACCOUNT_ID);

        // surrounding spaces from the form are trimmed
        accountOpeningService.openAccount(USER_ID,
                new OpenAccountRequestDTO(" Ada ", "Lovelace ", " ada@example.com", BIRTH_DATE));

        InOrder order = inOrder(accountOpeningMapper);
        order.verify(accountOpeningMapper).insertClient("Ada", "Lovelace", "ada@example.com", BIRTH_DATE);
        order.verify(accountOpeningMapper).linkUserToClient(USER_ID, CLIENT_ID);
        order.verify(accountOpeningMapper).insertAccount();
        order.verify(accountOpeningMapper).linkClientToAccount(CLIENT_ID, ACCOUNT_ID);
    }

    @Test
    void openAccount_userAlreadyHasClient_throwsAndCreatesNoAccount() {
        when(accountOpeningMapper.insertClient("Ada", "Lovelace", "ada@example.com", BIRTH_DATE)).thenReturn(CLIENT_ID);
        when(accountOpeningMapper.linkUserToClient(USER_ID, CLIENT_ID)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> accountOpeningService.openAccount(USER_ID,
                new OpenAccountRequestDTO("Ada", "Lovelace", "ada@example.com", BIRTH_DATE)));

        verify(accountOpeningMapper, never()).insertAccount();
        verify(accountOpeningMapper, never()).linkClientToAccount(anyLong(), anyLong());
    }
}
