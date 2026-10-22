package com.neueda.leap.security;

import com.neueda.leap.controller.AccountController;
import com.neueda.leap.controller.ClientController;
import com.neueda.leap.entity.Client;
import com.neueda.leap.exception.GlobalExceptionHandler;
import com.neueda.leap.repository.AccountHoldingMapper;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.service.AccountService;
import com.neueda.leap.service.ClientService;
import com.neueda.leap.service.InstrumentService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The @PreAuthorize ownership checks on the controllers. They only run when Spring wraps the controller,
 * so this starts real Spring Security (with method security) instead of the plain MockMvc setup the
 * controller tests use. AccessControlService is mocked: its own rules are in AccessControlServiceTest.
 */
@SpringJUnitWebConfig(OwnershipAuthorizationTest.TestConfig.class)
@TestPropertySource(properties = "jwt.shared-secret=" + OwnershipAuthorizationTest.SECRET)
class OwnershipAuthorizationTest {

    static final String SECRET = "test-secret-at-least-32-characters-long";
    private static final String USER_ID = "42";

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import({SecurityConfig.class, AccountController.class, ClientController.class, GlobalExceptionHandler.class})
    static class TestConfig {
        // the name the @PreAuthorize expressions use: @accessControlService
        @Bean
        AccessControlService accessControlService() {
            return mock(AccessControlService.class);
        }

        @Bean
        AccountService accountService() {
            return mock(AccountService.class);
        }

        @Bean
        ClientService clientService() {
            return mock(ClientService.class);
        }

        @Bean
        AccountHoldingMapper accountHoldingMapper() {
            return mock(AccountHoldingMapper.class);
        }

        @Bean
        AccountTradeMapper accountTradeMapper() {
            return mock(AccountTradeMapper.class);
        }

        @Bean
        InstrumentService instrumentService() {
            return mock(InstrumentService.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ClientService clientService;

    private MockMvc mockMvc;

    private final UUID accountId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // the mocks are beans shared by every test, so start each one clean
        reset(accessControlService, accountService, clientService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private ResultActions postDeposit() throws Exception {
        return mockMvc.perform(post("/v1/accounts/{accountId}/deposits", accountId)
                .header("Authorization", "Bearer " + clientToken())
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "amount": 250.50
                        }
                        """));
    }

    @Test
    void deposit_accountOwnedBySomeoneElse_returns403AndDepositsNothing() throws Exception {
        when(accessControlService.userOwnsAccount(accountId, USER_ID)).thenReturn(false);

        postDeposit().andExpect(status().isForbidden());

        verify(accountService, never()).deposit(any(), any());
    }

    @Test
    void deposit_ownedAccount_returnsNoContentAndCreditsIt() throws Exception {
        when(accessControlService.userOwnsAccount(accountId, USER_ID)).thenReturn(true);
        when(accountService.getAccountIdByExternalAccountId(accountId)).thenReturn(5L);

        postDeposit().andExpect(status().isNoContent());

        verify(accountService).deposit(5L, new BigDecimal("250.50"));
    }

    @Test
    void getClient_someoneElsesClient_returns403() throws Exception {
        when(accessControlService.canAccessClientData(clientId, USER_ID)).thenReturn(false);

        mockMvc.perform(get("/v1/clients/{clientId}", clientId).header("Authorization", "Bearer " + clientToken()))
                .andExpect(status().isForbidden());

        verify(clientService, never()).getClientByExternalId(any());
    }

    @Test
    void getClient_ownClient_returns200() throws Exception {
        when(accessControlService.canAccessClientData(clientId, USER_ID)).thenReturn(true);
        when(clientService.getClientByExternalId(clientId))
                .thenReturn(new Client("Ada", "Lovelace", "ada@example.com", null));
        when(clientService.getTradingAccounts(clientId)).thenReturn(List.of());

        mockMvc.perform(get("/v1/clients/{clientId}", clientId).header("Authorization", "Bearer " + clientToken()))
                .andExpect(status().isOk());
    }

    // signed the way auth-service signs tokens: HS256 with the shared secret, roles in "roles"
    private static String clientToken() throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(USER_ID)
                .claim("roles", List.of("CLIENT"))
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET.getBytes(UTF_8)));
        return jwt.serialize();
    }
}
