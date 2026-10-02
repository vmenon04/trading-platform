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

@Service
public class RecipientService {

    public ResponseEntity<Void> publishOrder(@Valid OrderRequestDTO dto) throws ExecutionException, InterruptedException {

        Properties props = new Properties();

        // FIXME - IP address, value serializer
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.JsonSerializer");

        try (KafkaProducer<Integer, OrderRequestDTO> producer = new KafkaProducer<>(props)) {
            ProducerRecord<Integer, OrderRequestDTO> record = new ProducerRecord<>("trades.submitted", dto.accountId(), dto);
           producer.send(record).get();

        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka publish interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Kafka publish failed", e);
        }

        int jobId = UUID.randomUUID().hashCode();
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/job/{jobId}")
                .buildAndExpand(jobId)
                .toUri();

        OrderSubmittedDTO response = new OrderSubmittedDTO(
                jobId,
                "SUBMITTED"
        );

        return ResponseEntity
                .accepted()
                .location(location).build();
    }
}
