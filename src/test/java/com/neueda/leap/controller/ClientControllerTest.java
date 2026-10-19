package com.neueda.leap.controller;

import com.neueda.leap.entity.Client;
import com.neueda.leap.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clientController).build();
    }

    @Test
    void getClient_existingClient_returns200WithFullName() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(clientService.getClientByExternalId(clientId))
                .thenReturn(new Client("Ada", "Lovelace", "ada@example.com", null));

        mockMvc.perform(get("/api/v1/clients/{clientId}", clientId).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.clientName").value("Ada Lovelace"));
    }

    @Test
    void getClient_unknownClient_returns404() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(clientService.getClientByExternalId(clientId)).thenReturn(null);

        mockMvc.perform(get("/api/v1/clients/{clientId}", clientId).contextPath("/api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getClient_malformedId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}", "not-a-uuid").contextPath("/api"))
                .andExpect(status().isBadRequest());
    }
}
