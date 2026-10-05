package com.neueda.leap.kafka;

import com.neueda.leap.dto.TradeFinishedDTO;
import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.service.ExecutionService;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumes recorded trades, executes them and publishes each outcome to trade.finished.
 */
@Component
public class ExecutionConsumer {

    private final ExecutionService executionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String tradeFinishedTopic;

    public ExecutionConsumer(ExecutionService executionService, KafkaTemplate<String, Object> kafkaTemplate,
                             @Value("${trade.finished:trade.finished}") String tradeFinishedTopic) {
        this.executionService = executionService;
        this.kafkaTemplate = kafkaTemplate;
        this.tradeFinishedTopic = tradeFinishedTopic;
    }
    
    @KafkaListener(topics = "${trade.recorded:trade.recorded}", groupId = "execution-service")
    public void onTradeRecorded(TradeRecordedDTO trade) throws InterruptedException, ExecutionException {
        TradeFinishedDTO outcome = executionService.execute(trade);
        kafkaTemplate.send(tradeFinishedTopic, String.valueOf(trade.tradeId()), outcome).get();
    }
}
