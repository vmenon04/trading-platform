package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequestDTO;
import com.neueda.leap.dto.TradeSubmittedDTO;
import com.neueda.leap.events.OrderCreatedEvent;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;


import java.math.BigDecimal;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RecipientService {

    private static final AtomicLong JOB_ID_GENERATOR = new AtomicLong(1);
    private final TradeEventProducer tradeEventProducer;

    public RecipientService(TradeEventProducer tradeEventProducer) {
        this.tradeEventProducer = tradeEventProducer;
    }
    public ResponseEntity<Void> publishOrder(@Valid OrderRequestDTO dto) {

        long taskId = JOB_ID_GENERATOR.getAndIncrement();

        TradeSubmittedDTO tradeSubmitted = new TradeSubmittedDTO(dto.instrumentId(), dto.accountId(), dto.side(), dto.quantity(), new BigDecimal(0), taskId); //FIXME - quote
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(dto.accountId(), java.util.UUID.randomUUID(), java.time.Instant.now(), tradeSubmitted);
        tradeEventProducer.publishOrderCreated(orderCreatedEvent);
//
//
//        try (KafkaProducer<Integer, TradeSubmittedDTO> producer = new KafkaProducer<>(props)) {
//            ProducerRecord<Integer, TradeSubmittedDTO> record = new ProducerRecord<>("trades.submitted", tradeSubmitted.getAccountId(), tradeSubmitted);
//            producer.send(record).get();
//
//        }
//        catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new IllegalStateException("Kafka publish interrupted");
//        } catch (ExecutionException e) {
//            throw new IllegalStateException("Kafka publish failed");
//        }

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/tasks/{taskId}")
                .buildAndExpand(taskId)
                .toUri();

        return ResponseEntity
                .accepted()
                .location(location).build();
    }
}
