package com.neueda.leap.controller;

import com.neueda.leap.entity.Client;
import com.neueda.leap.dto.AccountDTO;
import com.neueda.leap.dto.OpenAccountRequestDTO;
import com.neueda.leap.service.AccountOpeningService;
import com.neueda.leap.service.ClientService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private ClientService clientService;

    @Mock
    private AccountOpeningService accountOpeningService;

    @InjectMocks
    private ClientController clientController;

    private MockMvc mockMvc;

    private static final Long USER_ID = 7L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clientController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        // @AuthenticationPrincipal reads the logged-in user from the security context
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", USER_ID.toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getLoggedInClient_userWithClient_returnsClientAndAccounts() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(clientService.getExternalClientIdForUser(USER_ID)).thenReturn(clientId);
        when(clientService.getClientByExternalId(clientId))
                .thenReturn(new Client("Ada", "Lovelace", "ada@example.com", null));
        when(clientService.getTradingAccounts(clientId))
                .thenReturn(List.of(new AccountDTO(accountId)));

        mockMvc.perform(get("/api/v1/clients/me").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.clientName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.tradingAccounts[0].accountId").value(accountId.toString()));
    }

    @Test
    void openAccount_newUser_returns201WithTheNewAccount() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(clientService.getExternalClientIdForUser(USER_ID)).thenReturn(clientId);
        when(clientService.getClientByExternalId(clientId))
                .thenReturn(new Client("Ada", "Lovelace", "ada@example.com", null));
        when(clientService.getTradingAccounts(clientId)).thenReturn(List.of(new AccountDTO(accountId)));

        postOpenAccount(validAccountJson())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.tradingAccounts[0].accountId").value(accountId.toString()));
        verify(accountOpeningService).openAccount(USER_ID, new OpenAccountRequestDTO(
                "Ada", "Lovelace", "ada@example.com", LocalDate.of(1990, 1, 1)));
    }

    @Test
    void openAccount_userAlreadyHasAccount_returns409() throws Exception {
        doThrow(new IllegalStateException("already has an account"))
                .when(accountOpeningService).openAccount(eq(USER_ID), any());

        postOpenAccount(validAccountJson())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have a trading account"));
    }

    @Test
    void openAccount_emailAlreadyUsed_returns409() throws Exception {
        doThrow(new DuplicateKeyException("clients_email_key"))
                .when(accountOpeningService).openAccount(eq(USER_ID), any());

        postOpenAccount(validAccountJson())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("That email is already used by another client"));
    }

    @Test
    void openAccount_missingDetails_returns400AndOpensNothing() throws Exception {
        postOpenAccount("""
                {"firstName": "", "lastName": "Lovelace", "email": "not-an-email", "birthDate": "1990-01-01"}
                """)
                .andExpect(status().isBadRequest());
        verify(accountOpeningService, never()).openAccount(any(), any());
    }

    @Test
    void openAdditionalAccount_existingClient_returns201WithAllAccounts() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(clientService.getExternalClientIdForUser(USER_ID)).thenReturn(clientId);
        when(clientService.getClientByExternalId(clientId))
                .thenReturn(new Client("Ada", "Lovelace", "ada@example.com", null));
        when(clientService.getTradingAccounts(clientId)).thenReturn(List.of(new AccountDTO(first), new AccountDTO(second)));

        mockMvc.perform(post("/api/v1/clients/me/accounts").contextPath("/api"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tradingAccounts", hasSize(2)));
        verify(accountOpeningService).openAdditionalAccount(USER_ID);
    }

    @Test
    void openAdditionalAccount_userWithoutClient_returns404() throws Exception {
        doThrow(new java.util.NoSuchElementException("no client"))
                .when(accountOpeningService).openAdditionalAccount(USER_ID);

        mockMvc.perform(post("/api/v1/clients/me/accounts").contextPath("/api"))
                .andExpect(status().isNotFound());
    }

    private ResultActions postOpenAccount(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/clients/me").contextPath("/api")
                .contentType(APPLICATION_JSON)
                .content(json));
    }

    private static String validAccountJson() {
        return """
                {"firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com", "birthDate": "1990-01-01"}
                """;
    }

    @Test
    void getLoggedInClient_userWithoutClient_returns404() throws Exception {
        when(clientService.getExternalClientIdForUser(USER_ID)).thenReturn(null);

        mockMvc.perform(get("/api/v1/clients/me").contextPath("/api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getClient_existingClient_returns200WithFullName() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(clientService.getClientByExternalId(clientId))
                .thenReturn(new Client("Ada", "Lovelace", "ada@example.com", null));
        when(clientService.getTradingAccounts(clientId))
            .thenReturn(List.of(new AccountDTO(accountId)));

        mockMvc.perform(get("/api/v1/clients/{clientId}", clientId).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.clientName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.tradingAccounts[0].accountId").value(accountId.toString()));
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
