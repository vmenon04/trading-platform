package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.dto.TradeSubmittedDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Service
public class RecipientService {

    private final TradeEventProducer tradeEventProducer;

    public RecipientService(TradeEventProducer tradeEventProducer) {
        this.tradeEventProducer = tradeEventProducer;
    }

    // the externalTradeId is the trade's ID for the client: it's returned in the Location header
    // and stored with the trade when it's recorded
    public ResponseEntity<Void> publishOrder(@Valid TradeRequestDTO dto) {
        UUID externalTradeId = UUID.randomUUID();

        TradeSubmittedDTO tradeSubmitted = new TradeSubmittedDTO(
                dto.instrumentId(),
                dto.accountId(),
                dto.side(),
                dto.quantity(),
                externalTradeId
        );

        tradeEventProducer.publishTradeSubmitted(tradeSubmitted);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{externalTradeId}")
                .buildAndExpand(externalTradeId)
                .toUri();

        return ResponseEntity
                .accepted()
                .location(location)
                .build();
    }
}
