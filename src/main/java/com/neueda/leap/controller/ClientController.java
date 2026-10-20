package com.neueda.leap.controller;

import com.neueda.leap.dto.ClientResponseDTO;
import com.neueda.leap.entity.Client;
import com.neueda.leap.service.ClientService;
import org.springframework.http.ResponseEntity;
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
