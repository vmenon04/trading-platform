package com.neueda.leap.service;

import com.neueda.leap.entity.Client;
import com.neueda.leap.repository.ClientMapper;
import org.springframework.stereotype.Service;

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
}
