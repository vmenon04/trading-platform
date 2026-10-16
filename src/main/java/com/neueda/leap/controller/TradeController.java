package com.neueda.leap.controller;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.service.RecipientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/accounts/{accountId}/trades")
public class TradeController {

    private final RecipientService recipientService;

    public TradeController(RecipientService recipientService) {
        this.recipientService = recipientService;
    }

    @PostMapping
    public ResponseEntity<Void> submitOrder(
        @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TradeRequestDTO dto) {

        String userIdString = jwt.getSubject();
        Long userId = Long.parseLong(userIdString);
        return recipientService.publishOrder(dto, userId);
    }

}
