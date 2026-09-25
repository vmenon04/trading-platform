package com.neueda.leap.controller;

import com.neueda.leap.entity.AccountTrade;
import com.neueda.leap.service.OrderManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<TradeResponse> placeTrade(
            @PathVariable int accountId,
            @Valid @RequestBody OrderRequestDTO orderRequestDTO) {

        long tradeId = orderManagementService.placeOrder(orderRequestDTO);
        
        TradeResponse response = new TradeResponse();
        response.setStatus("ACCEPTED");
        response.setTradeId(tradeId);
        
        return ResponseEntity.ok(response);
    }
}