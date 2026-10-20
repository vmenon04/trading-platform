package com.neueda.leap.controller;

import com.neueda.leap.dto.ClientResponseDTO;
import com.neueda.leap.entity.Client;
import com.neueda.leap.service.ClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
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
}
