package com.neueda.leap.controller;

import com.neueda.leap.dto.ClientResponseDTO;
import com.neueda.leap.dto.ErrorResponseDTO;
import com.neueda.leap.dto.OpenAccountRequestDTO;
import com.neueda.leap.entity.Client;
import com.neueda.leap.service.AccountOpeningService;
import com.neueda.leap.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/clients")
public class ClientController {

    private final ClientService clientService;
    private final AccountOpeningService accountOpeningService;

    public ClientController(ClientService clientService, AccountOpeningService accountOpeningService) {
        this.clientService = clientService;
        this.accountOpeningService = accountOpeningService;
    }

    // the logged-in user's client and trading accounts, found from the user in the token
    @GetMapping("/me")
    public ResponseEntity<ClientResponseDTO> getLoggedInClient(@AuthenticationPrincipal Jwt jwt) {
        UUID externalClientId = clientService.getExternalClientIdForUser(Long.parseLong(jwt.getSubject()));
        if (externalClientId == null) {
            // a user who only registered a login has no client or accounts yet
            return ResponseEntity.notFound().build();
        }
        return getClient(externalClientId);
    }

    // opens the logged-in user's first trading account (with no cash) from their details
    @PostMapping("/me")
    public ResponseEntity<?> openAccount(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody OpenAccountRequestDTO request) {
        try {
            accountOpeningService.openAccount(Long.parseLong(jwt.getSubject()), request);
        } catch (IllegalStateException e) {
            return conflict("You already have a trading account");
        } catch (DuplicateKeyException e) {
            return conflict("That email is already used by another client");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(getLoggedInClient(jwt).getBody());
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ClientResponseDTO> getClient(@PathVariable("clientId") UUID externalClientId) {
        Client client = clientService.getClientByExternalId(externalClientId);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ClientResponseDTO(
            externalClientId,
            client.getFirstName() + " " + client.getLastName(),
            clientService.getTradingAccounts(externalClientId)));
    }

    private static ResponseEntity<ErrorResponseDTO> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDTO.of(HttpStatus.CONFLICT, "Conflict", message));
    }
}
