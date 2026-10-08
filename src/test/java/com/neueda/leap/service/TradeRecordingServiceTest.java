package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRecordedDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"test-topic"})
@TestPropertySource(properties = {
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=test-group",
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class TradeRecordingServiceTest {

    @Autowired
    private TradeRecordingService tradeRecordingService;

    @Autowired
    private KafkaTemplate<String, TradeRecordedDTO> kafkaTemplate;

    @BeforeEach
    void setUp() {
//        tradeRecordingService = new TradeRecordingService(kafkaTemplate, );
    }

    @Test
    void consume() {
    }

    @Test
    void sendMessage() {
    }
}