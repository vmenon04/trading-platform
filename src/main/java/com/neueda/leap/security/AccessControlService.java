package com.neueda.leap.security;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.service.AccountService;
import com.neueda.leap.service.ClientAccountService;
import com.neueda.leap.service.ClientService;
import com.neueda.leap.service.UserService;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@EnableMethodSecurity(prePostEnabled = true)
public class AccessControlService {

    private final AccountService accountService;
    private final ClientService clientService;
    private final ClientAccountService clientAccountsService;
    private final UserService userService;

    public AccessControlService(AccountService accountService,
                                ClientService clientService,
                                ClientAccountService clientAccountsService,
                                UserService userService) {
        this.accountService = accountService;
        this.clientService = clientService;
        this.clientAccountsService = clientAccountsService;
        this.userService = userService;
    }

    public boolean canAccessClientData(UUID externalClientId, String userId) {
        Long clientId = clientService.getInternalClientId(externalClientId);
        Long clientUserId = userService.getUserIdByClientId(clientId);
        return Objects.equals(clientUserId, Long.valueOf(userId));
    }

    public boolean userOwnsAccount(UUID externalAccountId, String userId) {
        Long accountId = accountService.getAccountIdByExternalAccountId(externalAccountId);
        List<Long> clientIds = clientAccountsService.getClientIdByAccountId(accountId);
        if (clientIds.isEmpty()) {
            return false;
        }
        for (Long clientId : clientIds) {
            Long clientUserId = userService.getUserIdByClientId(clientId);
            if (Objects.equals(clientUserId, Long.valueOf(userId))) {
                return true;
            }
        }
        return false;
    }
}
