package com.neueda.leap.controller;

import com.neueda.leap.dto.OrderRequestDTO;
import com.neueda.leap.dto.OrderSubmittedDTO;
import com.neueda.leap.service.RecipientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;


@RestController
@RequestMapping("/accounts/{accountId}/trades")
public class TradeController {

    private final RecipientService recipientService;

    public TradeController(RecipientService recipientService) {
        this.recipientService = recipientService;
    }

    @PostMapping
    public ResponseEntity<Void> submitOrder(
            @PathVariable int accountId,
            @Valid @RequestBody OrderRequestDTO dto) throws ExecutionException, InterruptedException {
        
        
        return recipientService.publishOrder(dto);
    }

}
