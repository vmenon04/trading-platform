package com.neueda.leap.controller;

import com.neueda.leap.dto.ClientResponseDTO;
import com.neueda.leap.entity.Client;
import com.neueda.leap.security.AccessControlService;
import com.neueda.leap.service.ClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("@accessControlService.canAccessClientData(#externalClientId, #jwt.getSubject())")
    @GetMapping("/{clientId}")
    public ResponseEntity<ClientResponseDTO> getClient(@PathVariable("clientId") UUID externalClientId,
                                                       @AuthenticationPrincipal Jwt jwt) {
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
