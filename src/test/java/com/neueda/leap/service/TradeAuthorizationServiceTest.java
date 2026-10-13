package com.neueda.leap.service;
import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.enums.TradeSide;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class TradeAuthorizationServiceTest {
    @Mock
    private AccountService accountService;
    @Mock
    private ClientAccountService clientAccountService;
    @Mock
    private UserService userService;
    @InjectMocks
    private TradeAuthorizationService tradeAuthorizationService;
    @Test
    void returnsTrueWhenUsernameMatchesAnyLinkedClient() {
        TradeRequestDTO dto = new TradeRequestDTO(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                10L,
                TradeSide.BUY,
                new BigDecimal("5")
        );
        when(accountService.getAccountIdByExternalAccountId(dto.accountId())).thenReturn(1L);
        when(clientAccountService.getClientIdByAccountId(1L)).thenReturn(List.of(100L, 200L));
        when(userService.getUsernameByClientId(100L)).thenReturn("someone-else");
        when(userService.getUsernameByClientId(200L)).thenReturn("alice");
        assertTrue(tradeAuthorizationService.isValidUsername(dto, "alice"));
    }
    @Test
    void returnsFalseWhenUsernameDoesNotMatchAnyLinkedClient() {
        TradeRequestDTO dto = new TradeRequestDTO(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                10L,
                TradeSide.BUY,
                new BigDecimal("5")
        );
        when(accountService.getAccountIdByExternalAccountId(dto.accountId())).thenReturn(1L);
        when(clientAccountService.getClientIdByAccountId(1L)).thenReturn(List.of(100L));
        when(userService.getUsernameByClientId(100L)).thenReturn("bob");
        assertFalse(tradeAuthorizationService.isValidUsername(dto, "alice"));
    }
}
