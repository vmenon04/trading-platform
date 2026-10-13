package com.neueda.leap.service;

import com.neueda.leap.repository.ClientAccountMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientAccountService {

    private final ClientAccountMapper clientAccountMapper;

    public ClientAccountService(ClientAccountMapper clientAccountMapper) {
        this.clientAccountMapper = clientAccountMapper;
    }

    public List<Long> getClientIdByAccountId(Long accountId) {
        return clientAccountMapper.getClientIdByAccountId(accountId);
    }
}
