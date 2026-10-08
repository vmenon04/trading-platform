package com.neueda.leap.service;

import com.neueda.leap.dto.TradeFinishedDTO;
import com.neueda.leap.events.OrderCreatedEvent;
import com.neueda.leap.events.TradeValidatedEvent;
import com.neueda.leap.kafka.KafkaTopics;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

// questions:
// 1.   KafkaProducerConfig imports Jackson's com.fasterxml.jackson.databind.JsonSerializer 
//      should we switch to org.springframework.kafka.support.serializer.JsonSerializer?
// 2.   Right now, the callers of the publish methods kafkaTemplate.send() 
//      have no way to know if the message was successfully sent to the broker.
//      AI suggestion is that the publish methods should ideally return the 
//      CompletableFuture from kafkaTemplate.send() so that the caller can handle success or failure.
// 3.   trade.validated and trade.recorded are keyed by tradeId, shouldn't we consider keying by accountId?
// 4.   Shouldn't we use the topic's DTO (TradeSubmittedDTO, TradeValidatedDTO, TradeRecordedDTO) 
//      instead of separate key and Object so we can let each method pick its own key and stop the
//      wrong thing from going into a topic.
// 5.   publishOrderCreated publishes to order.created, but we replaced it with trade.submitted in the pipeline.
//      (just a small naming difference)

@Service
public class TradeEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TradeEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, event.accountId().toString(), event);
    }

    public void publishTradeValidated(TradeValidatedEvent event) {
        kafkaTemplate.send(KafkaTopics.TRADE_VALIDATED, event.tradeId().toString(), event);
    }

    public void publishTradeRecorded(String tradeId, Object event) {
        kafkaTemplate.send(KafkaTopics.TRADE_RECORDED, tradeId, event);
    }

    public CompletableFuture<SendResult<String, Object>> publishTradeFinished(TradeFinishedDTO event) {
        return kafkaTemplate.send(KafkaTopics.TRADE_FINISHED, event.tradeId().toString(), event);
    }
}