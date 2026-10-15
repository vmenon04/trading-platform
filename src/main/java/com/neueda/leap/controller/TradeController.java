package com.neueda.leap.controller;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.service.RecipientService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


// off when trade.stage.api.enabled=false, so a container can run only some stages; on when unset
@ConditionalOnProperty(name = "trade.stage.api.enabled", havingValue = "true", matchIfMissing = true)
@RestController
@RequestMapping("/accounts/{accountId}/trades")
public class TradeController {

    private final RecipientService recipientService;

    public TradeController(RecipientService recipientService) {
        this.recipientService = recipientService;
    }

    @PostMapping
    public ResponseEntity<Void> submitOrder(
            @PathVariable UUID accountId,
            @Valid @RequestBody TradeRequestDTO dto) {
        
        
        return recipientService.publishOrder(dto);
    }

}
