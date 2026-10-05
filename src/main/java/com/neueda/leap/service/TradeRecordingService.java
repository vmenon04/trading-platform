package com.neueda.leap.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TradeRecordingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeRecordingService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String tradeRecordedTopic;

    public TradeRecordingService(KafkaTemplate<String, String> kafkaTemplate,
                                 @Value("${trade.recorded}") String tradeRecordedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.tradeRecordedTopic = tradeRecordedTopic;
    }

    @KafkaListener(topics = "${trade.validated}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        //TODO: Add DB Entry

        //TODO: Add call to Producer

        LOGGER.info("Received validated trade event: {}", message);
        // TODO: Persist the validated trade payload and publish the recorded trade identifier.
    }

    public void sendMessage(String message) {
        kafkaTemplate.send(tradeRecordedTopic, message);
        LOGGER.info("Published trade recorded event to {}", tradeRecordedTopic);
    }
}
