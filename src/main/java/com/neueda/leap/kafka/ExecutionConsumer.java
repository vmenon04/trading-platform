package com.neueda.leap.kafka;

import com.neueda.leap.dto.TradeFinishedDTO;
import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.service.ExecutionService;
import com.neueda.leap.service.TradeEventProducer;
import java.util.concurrent.ExecutionException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes recorded trades, executes them and publishes each outcome to trade.finished.
 */
@Component
public class ExecutionConsumer {

    private final ExecutionService executionService;
    private final TradeEventProducer tradeEventProducer;

    public ExecutionConsumer(ExecutionService executionService, TradeEventProducer tradeEventProducer) {
        this.executionService = executionService;
        this.tradeEventProducer = tradeEventProducer;
    }
    
    @KafkaListener(topics = "${trade.recorded}", groupId = "execution-service")
    public void onTradeRecorded(TradeRecordedDTO trade) throws InterruptedException, ExecutionException {
        TradeFinishedDTO outcome = executionService.execute(trade);
        tradeEventProducer.publishTradeFinished(outcome).get();
    }
}
