package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequestDTO;
import com.neueda.leap.dto.OrderSubmittedDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;


import java.net.URI;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RecipientService {

    private static final AtomicLong JOB_ID_GENERATOR = new AtomicLong(1);
    public ResponseEntity<Void> publishOrder(@Valid OrderRequestDTO dto) throws ExecutionException, InterruptedException {

        long taskId = JOB_ID_GENERATOR.getAndIncrement();

        TradeSubmittedDTO tradeSubmitted = new TradeSubmittedDTO(dto.getInstrumentId(), dto.getAccountId(), dto.getSide(), dto.getQuantity(), dto.getQuote(), taskId);

        Properties props = new Properties();

        // FIXME - IP address, value serializer
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.JsonSerializer");

        try (KafkaProducer<Integer, TradeSubmittedDTO> producer = new KafkaProducer<>(props)) {
            ProducerRecord<Integer, TradeSubmittedDTO> record = new ProducerRecord<>("trades.submitted", tradeSubmitted.accountId(), tradeSubmitted);
            producer.send(record).get();

        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka publish interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Kafka publish failed", e);
        }

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/job/{jobId}")
                .buildAndExpand(taskId)
                .toUri();

        return ResponseEntity
                .accepted()
                .location(location).build();
    }
}
