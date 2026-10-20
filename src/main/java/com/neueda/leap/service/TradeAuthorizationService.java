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

    public boolean isValidUserId(TradeRequestDTO dto, Long userId) {
        Long accountId = accountService.getAccountIdByExternalAccountId(dto.accountId());
        return ownsAccount(userId, accountId);
    }

    // true if the logged-in user is one of the clients linked to the account (accountId is the internal ID)
    public boolean ownsAccount(Long userId, Long accountId) {
        List<Long> clientIds = clientAccountsService.getClientIdByAccountId(accountId);

        for (Long clientId : clientIds) {
            Long dtoUserId = userService.getUserIdByClientId(clientId);
            if (Objects.equals(dtoUserId, userId)) {
                return true;
            }
        }

        return false;
    }
}
