package com.neueda.leap.controller;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.service.RecipientService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TradeControllerTest {

    @Mock
    private RecipientService recipientService;

    @InjectMocks
    private TradeController tradeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tradeController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        // @AuthenticationPrincipal reads the logged-in user from the security context; user 1 here
        SecurityContextHolder.getContext().setAuthentication(authentication());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitOrder_malformedJson_returns400_andDoesNotCallService() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", accountId)
                        .contextPath("/api")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "not-a-uuid",
                                  "instrumentId": 10,
                                  "side": "BUY",
                                  "quantity": 25.5
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recipientService);
    }

    @Test
    void submitOrder_validRequest_passesDtoAndUserIdToService_andReturns202WithTradeLocation() throws Exception {
        UUID accountId = UUID.randomUUID();
        URI location = URI.create("/api/accounts/%s/trades/%s".formatted(accountId, UUID.randomUUID()));
        when(recipientService.publishOrder(any(TradeRequestDTO.class), eq(1L)))
                .thenReturn(ResponseEntity.accepted().location(location).build());

        mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", accountId)
                        .contextPath("/api")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "instrumentId": 10,
                                  "side": "BUY",
                                  "quantity": 25.5
                                }
                                """.formatted(accountId)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", location.toString()));

        // the user ID comes from the token's subject, not from the request body
        verify(recipientService).publishOrder(any(TradeRequestDTO.class), eq(1L));
    }

    private static JwtAuthenticationToken authentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "1")
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
