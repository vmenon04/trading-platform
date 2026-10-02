package com.neueda.leap.kafka;

import com.neueda.leap.dto.TradeFinishedDTO;
import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.service.ExecutionService;
import java.util.concurrent.ExecutionException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumes recorded trades, executes them and publishes each outcome to trade.finished.
 */
@Component
public class ExecutionConsumer {

    private static final String TRADE_RECORDED = "trade.recorded";
    private static final String TRADE_FINISHED = "trade.finished";

    private final ExecutionService executionService;
    private final KafkaTemplate<String, TradeFinishedDTO> kafkaTemplate;

    public ExecutionConsumer(ExecutionService executionService, KafkaTemplate<String, TradeFinishedDTO> kafkaTemplate) {
        this.executionService = executionService;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @KafkaListener(topics = TRADE_RECORDED, groupId = "execution-service")
    public void onTradeRecorded(TradeRecordedDTO trade) throws InterruptedException, ExecutionException {
        TradeFinishedDTO outcome = executionService.execute(trade);
        kafkaTemplate.send(TRADE_FINISHED, String.valueOf(trade.tradeId()), outcome).get();
    }
}
