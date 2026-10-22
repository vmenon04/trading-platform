package com.neueda.leap.service;

import com.neueda.leap.dto.OpenAccountRequestDTO;
import com.neueda.leap.repository.AccountOpeningMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Opens a trading account for a user who has only registered a login with auth-service.
 */
@Service
public class AccountOpeningService {

    private final AccountOpeningMapper accountOpeningMapper;

    public AccountOpeningService(AccountOpeningMapper accountOpeningMapper) {
        this.accountOpeningMapper = accountOpeningMapper;
    }

    /**
     * Creates a client from the user's details, an account with no cash, and links the user, client and account.
     * All of it commits together, or none of it does.
     *
     * @param userId logged-in user id from the token
     * @param request the user's client details
     * @throws IllegalStateException if the user already has a client (and so already has an account)
     * @throws org.springframework.dao.DuplicateKeyException if another client already uses the email
     */
    @Transactional
    public void openAccount(Long userId, OpenAccountRequestDTO request) {
        // checked first, so a user who already has an account isn't told their own email is taken
        if (accountOpeningMapper.findClientIdByUserId(userId) != null) {
            throw new IllegalStateException("User " + userId + " already has an account");
        }
        Long clientId = accountOpeningMapper.insertClient(request.firstName().trim(), request.lastName().trim(),
                request.email().trim(), request.birthDate());
        if (accountOpeningMapper.linkUserToClient(userId, clientId) == 0) {
            // another request linked the user meanwhile; rolls back the client inserted above
            throw new IllegalStateException("User " + userId + " already has an account");
        }
        Long accountId = accountOpeningMapper.insertAccount();
        accountOpeningMapper.linkClientToAccount(clientId, accountId);
    }

    /**
     * Opens another account, with no cash, for a user who already has a client.
     *
     * @param userId logged-in user id from the token
     * @throws java.util.NoSuchElementException if the user has no client yet (they open their first account instead)
     */
    @Transactional
    public void openAdditionalAccount(Long userId) {
        Long clientId = accountOpeningMapper.findClientIdByUserId(userId);
        if (clientId == null) {
            throw new NoSuchElementException("User " + userId + " has no client yet");
        }
        Long accountId = accountOpeningMapper.insertAccount();
        accountOpeningMapper.linkClientToAccount(clientId, accountId);
    }
}
