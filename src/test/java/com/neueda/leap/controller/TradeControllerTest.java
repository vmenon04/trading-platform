package com.neueda.leap.controller;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.service.RecipientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;

import java.net.URI;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    }

    @Test
    void submitOrder_malformedJson_returns400_andDoesNotCallService() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(post("/api/accounts/{accountId}/trades", accountId)
                        .contextPath("/api")
                        .principal(authentication("user"))
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

    }

    @Test
    void submitOrder_validRequest_passesDtoToService_andReturns202WithTaskLocation() throws Exception {
        UUID accountId = UUID.randomUUID();
        URI location = URI.create("/api/accounts/%s/trades/tasks/42".formatted(accountId));
        TradeRequestDTO dto = new TradeRequestDTO(
                accountId,
                10L,
                com.neueda.leap.enums.TradeSide.BUY,
                new java.math.BigDecimal("25.5")
        );
        when(recipientService.publishOrder(dto, "user"))
                .thenReturn(ResponseEntity.accepted().location(location).build());

        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user");

        ResponseEntity<Void> response = tradeController.submitOrder(jwt, accountId, dto);

        org.junit.jupiter.api.Assertions.assertEquals(202, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertEquals(location, response.getHeaders().getLocation());
    }

    private static JwtAuthenticationToken authentication(String username) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", username)
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
