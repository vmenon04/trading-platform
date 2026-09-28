package com.neueda.leap.controller;

import com.neueda.leap.dto.OrderRequestDTO;
import com.neueda.leap.dto.OrderResponseDTO;
import com.neueda.leap.entity.AccountTrade;
import com.neueda.leap.service.OrderManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/accounts/{accountId}/trades")
public class TradeController {

    private final OrderManagementService orderManagementService;

    @Autowired
    public TradeController(OrderManagementService orderManagementService) {
        this.orderManagementService = orderManagementService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> placeTrade(
            @PathVariable int accountId,
            @Valid @RequestBody OrderRequestDTO orderRequestDTO) {

        int tradeId = orderManagementService.placeOrder(orderRequestDTO);
        
        OrderResponseDTO response = new OrderResponseDTO(tradeId, "EXECUTED", BigDecimal.ZERO, BigDecimal.ZERO, "");
        response.setStatus("EXECUTED");
        response.setTradeId(tradeId);
        
        return ResponseEntity.ok(response);
    }
}