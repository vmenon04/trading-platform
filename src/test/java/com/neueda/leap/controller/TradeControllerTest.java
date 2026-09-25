package com.neueda.leap.controller;

import com.neueda.leap.service.OrderManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderManagementService orderManagementService;

    @Test
    @DisplayName("Valid BUY order is accepted and executed")
    void ValidBuyRequestReturnsSuccess() throws Exception {
        when(orderManagementService.placeOrder(any(OrderRequest.class)))
                .thenReturn(5001);

        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrumentId": 1,
                              "side": "BUY",
                              "quantity": 10
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.tradeId").value(5001));
    }

    @Test
    @DisplayName("Valid SELL order is accepted and executed")
    void ValidSellRequestReturnsSuccess() throws Exception {
        when(orderManagementService.placeOrder(any(OrderRequest.class)))
                .thenReturn(5002);

        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrumentId": 1,
                              "side": "SELL",
                              "quantity": 10
                      
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.tradeId").value(5002));
    }

    @Test
    @DisplayName("SELL exceeding held quantity returns 422 Unprocessable Entity")
    void SellMoreThanHeldReturns422() throws Exception {
        when(orderManagementService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new IllegalStateException("Insufficient quantity of instrument 1 in account 1: holding 100.0, order quantity 110"));

        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrumentId": 1,
                              "side": "SELL",
                              "quantity": 110
                            }
                            """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Insufficient holding for the requested trade"));
    }

    @Test
    @DisplayName("BUY with insufficient balance returns 422 Unprocessable Entity")
    void BuyMoreThanHaveReturns422() throws Exception {
        when(orderManagementService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new IllegalStateException("Insufficient funds in account 1: balance 100.0, order cost 500.0"));

        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                             "instrumentId": 1,
                              "side": "BUY",
                              "quantity": 50
                            }
                            """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Insufficient balance for the requested trade"));
    }

    @Test
    @DisplayName("Unknown instrument ID returns 404")
    void UnknownInstrumentIdReturns404() throws Exception {
        when(orderManagementService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new java.util.NoSuchElementException("No instrument with id 999"));

        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrumentId": 999,
                              "side": "SELL",
                              "quantity": 10
                            }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("404"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    @DisplayName("Malformed JSON request returns 400 Bad Request")
    void MalformedJsonRequestReturns400() throws Exception {
        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrumentId": 1,
                              "side": "SELL",
                              "quantity": -5
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    @DisplayName("Missing required fields returns 400 Bad Request")
    void missingFieldsReturns400() throws Exception {
        mockMvc.perform(post("/accounts/1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "instrumentId": 1,
                              "side": "BUY"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }
}

