package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRequestDTO;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TradeAuthorizationService {

    private final AccountService accountService;
    private final ClientAccountService clientAccountsService;
    private final UserService userService;

    public TradeAuthorizationService(AccountService accountService,
                                     ClientAccountService clientAccountService,
                                     UserService userService) {
        this.accountService = accountService;
        this.clientAccountsService = clientAccountService;
        this.userService = userService;
    }

    public boolean isValidUsername(TradeRequestDTO dto, String username) {
        Long accountId = accountService.getAccountIdByExternalAccountId(dto.accountId());
        List<Long> clientIds = clientAccountsService.getClientIdByAccountId(accountId);

        for (Long clientId : clientIds) {
            String dtoUsername = userService.getUsernameByClientId(clientId);
            if (Objects.equals(dtoUsername, username)) {
                return true;
            }
        }

        return false;
    }
}
