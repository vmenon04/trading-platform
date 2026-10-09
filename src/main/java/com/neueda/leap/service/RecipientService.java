package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.dto.TradeSubmittedDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RecipientService {

    private static final AtomicLong JOB_ID_GENERATOR = new AtomicLong(1);
    private final TradeEventProducer tradeEventProducer;

    public RecipientService(TradeEventProducer tradeEventProducer) {
        this.tradeEventProducer = tradeEventProducer;
    }

    public ResponseEntity<Void> publishOrder(@Valid TradeRequestDTO dto) {
        long taskId = JOB_ID_GENERATOR.getAndIncrement();

        TradeSubmittedDTO tradeSubmitted = new TradeSubmittedDTO(
                dto.instrumentId(),
                dto.accountId(),
                dto.side(),
                dto.quantity(),
                taskId
        );

        tradeEventProducer.publishTradeSubmitted(tradeSubmitted);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/tasks/{taskId}")
                .buildAndExpand(taskId)
                .toUri();

        return ResponseEntity
                .accepted()
                .location(location)
                .build();
    }
}
