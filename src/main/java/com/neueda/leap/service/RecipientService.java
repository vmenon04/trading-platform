package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequestDTO;
import com.neueda.leap.dto.OrderSubmittedDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Service
public class RecipientService {

    public ResponseEntity<OrderSubmittedDTO> publishOrder(@Valid OrderRequestDTO dto) {

        int jobId = UUID.randomUUID().hashCode();
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/job/{jobId}")
                .buildAndExpand(jobId)
                .toUri();

        OrderSubmittedDTO response = new OrderSubmittedDTO(
                jobId,
                "SUBMITTED",
                "Order accepted for processing."
        );

        return ResponseEntity
                .accepted()
                .location(location)
                .body(response);
    }
}
