package com.neueda.leap.service;

import com.neueda.leap.entity.Client;
import com.neueda.leap.repository.ClientMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientService clientService;

    private final UUID externalClientId = UUID.randomUUID();

    @Test
    void getInternalClientIdReturnsIdFromMapper() {
        when(clientMapper.findInternalIdByExternalClientId(externalClientId)).thenReturn(7L);
        assertEquals(7L, clientService.getInternalClientId(externalClientId));
    }

    @Test
    void getInternalClientIdReturnsNullWhenNotFound() {
        when(clientMapper.findInternalIdByExternalClientId(externalClientId)).thenReturn(null);
        assertNull(clientService.getInternalClientId(externalClientId));
    }

    @Test
    void getClientByExternalIdReturnsClientFromMapper() {
        Client client = new Client("Ada", "Lovelace", "ada@example.com", null);
        when(clientMapper.findInternalIdByExternalClientId(externalClientId)).thenReturn(7L);
        when(clientMapper.findById(7L)).thenReturn(client);
        assertSame(client, clientService.getClientByExternalId(externalClientId));
    }

    @Test
    void getClientByExternalIdReturnsNullWithoutLookupWhenIdNotFound() {
        when(clientMapper.findInternalIdByExternalClientId(externalClientId)).thenReturn(null);
        assertNull(clientService.getClientByExternalId(externalClientId));
        verify(clientMapper, never()).findById(any());
    }
}
