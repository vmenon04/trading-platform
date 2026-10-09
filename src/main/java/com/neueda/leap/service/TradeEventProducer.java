package com.neueda.leap.service;

import com.neueda.leap.dto.TradeSubmittedDTO;
import com.neueda.leap.events.TradeValidatedEvent;
import com.neueda.leap.kafka.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TradeEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TradeEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTradeSubmitted(TradeSubmittedDTO dto) {
        kafkaTemplate.send(KafkaTopics.TRADE_SUBMITTED, dto.accountId().toString(), dto);
    }

    public void publishTradeValidated(TradeValidatedEvent event) {
        kafkaTemplate.send(KafkaTopics.TRADE_VALIDATED, event.tradeId().toString(), event);
    }

    public void publishTradeRecorded(String tradeId, Object event) {
        kafkaTemplate.send(KafkaTopics.TRADE_RECORDED, tradeId, event);
    }

    public void publishTradeFinished(String tradeId, Object event) {
        kafkaTemplate.send(KafkaTopics.TRADE_FINISHED, tradeId, event);
    }
}