package com.neueda.leap.service;

import com.neueda.leap.dto.AccountDTO;
import com.neueda.leap.entity.Client;
import com.neueda.leap.repository.ClientMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Provides client lookups using the external client identifier exposed by the API.
 */
@Service
public class ClientService {

    private final ClientMapper clientMapper;

    public ClientService(ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

    /**
     * Resolves the database client id from an external client id.
     *
     * @param externalClientId public client identifier
     * @return internal database id, or {@code null} when no client exists
     */
    public Long getInternalClientId(UUID externalClientId) {
        return clientMapper.findInternalIdByExternalClientId(externalClientId);
    }

    /**
     * Retrieves a client using its public external identifier.
     *
     * @param externalClientId public client identifier
     * @return matching client, or {@code null} when no client exists
     */
    public Client getClientByExternalId(UUID externalClientId) {
        Long internalClientId = getInternalClientId(externalClientId);
        return internalClientId == null ? null : clientMapper.findById(internalClientId);
    }

    /**
     * Finds the client linked to a logged-in user.
     *
     * @param userId user id from the login token
     * @return the client's external id, or {@code null} when the user has no client (e.g. only registered a login)
     */
    public UUID getExternalClientIdForUser(Long userId) {
        String externalClientId = clientMapper.findExternalClientIdByUserId(userId);
        return externalClientId == null ? null : UUID.fromString(externalClientId);
    }

    public List<AccountDTO> getTradingAccounts(UUID externalClientId) {
        Long internalClientId = getInternalClientId(externalClientId);
        if (internalClientId == null) {
            throw new NoSuchElementException("No client with external client id " + externalClientId);
        }
        return clientMapper.findExternalAccountIdsByClientId(internalClientId).stream()
                .map(AccountDTO::new)
                .toList();
    }
}
