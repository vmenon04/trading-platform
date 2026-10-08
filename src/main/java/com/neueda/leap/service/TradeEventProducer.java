package com.neueda.leap.service;

import com.neueda.leap.dto.TradeFinishedDTO;
import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.dto.TradeSubmittedDTO;
import com.neueda.leap.dto.TradeValidatedDTO;
import com.neueda.leap.kafka.KafkaTopics;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
public class TradeEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TradeEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, Object>> publishTradeSubmitted(TradeSubmittedDTO event) {
        return kafkaTemplate.send(KafkaTopics.TRADE_SUBMITTED, event.accountId().toString(), event);
    }

    public CompletableFuture<SendResult<String, Object>> publishTradeValidated(TradeValidatedDTO event) {
        return kafkaTemplate.send(KafkaTopics.TRADE_VALIDATED, event.accountId().toString(), event);
    }

    public CompletableFuture<SendResult<String, Object>> publishTradeRecorded(TradeRecordedDTO event) {
        return kafkaTemplate.send(KafkaTopics.TRADE_RECORDED, event.accountId().toString(), event);
    }

    public CompletableFuture<SendResult<String, Object>> publishTradeFinished(TradeFinishedDTO event) {
        return kafkaTemplate.send(KafkaTopics.TRADE_FINISHED, event.tradeId().toString(), event);
    }
}