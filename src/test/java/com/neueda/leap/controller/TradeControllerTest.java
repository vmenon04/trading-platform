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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
        mockMvc = MockMvcBuilders.standaloneSetup(tradeController).build();
    }

    @Test
    void submitOrder_malformedJson_returns400_andDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/accounts/{account_id}/trades", 123)
                        .contextPath("/api")
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
        URI location = URI.create("/api/accounts/123/trades/tasks/42");
        when(recipientService.publishOrder(any(TradeRequestDTO.class)))
                .thenReturn(ResponseEntity.accepted().location(location).build());

        mockMvc.perform(post("/api/accounts/{account_id}/trades", 123)
                        .contextPath("/api")
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
    }
}
