package com.neueda.leap.kafka;

import com.neueda.leap.dto.TradeFinishedDTO;
import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.service.ExecutionService;
import com.neueda.leap.service.TradeEventProducer;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionConsumerTest {

    @Mock
    private ExecutionService executionService;

    @Mock
    private TradeEventProducer tradeEventProducer;

    private ExecutionConsumer executionConsumer;

    private final TradeRecordedDTO trade = new TradeRecordedDTO(
            10L, 1L, TradeSide.BUY, new BigDecimal("5"), new BigDecimal("99"), 42L);

    private final TradeFinishedDTO outcome = new TradeFinishedDTO(
            42L, 1L, 10L, TradeSide.BUY, new BigDecimal("5"), new BigDecimal("100"), null);

    @BeforeEach
    void setUp() {
        executionConsumer = new ExecutionConsumer(executionService, tradeEventProducer);
    }

    @Test
    void executesTradeAndPublishesOutcome() throws Exception {
        when(executionService.execute(trade)).thenReturn(outcome);
        when(tradeEventProducer.publishTradeFinished(outcome)).thenReturn(CompletableFuture.completedFuture(null));

        executionConsumer.onTradeRecorded(trade);

        verify(tradeEventProducer).publishTradeFinished(outcome);
    }

    // the exception stops the offset being committed, so Kafka redelivers the trade
    @Test
    void failedPublishFailsTheListenerSoTheMessageIsRedelivered() {
        when(executionService.execute(trade)).thenReturn(outcome);
        when(tradeEventProducer.publishTradeFinished(outcome))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("Broker unavailable")));

        assertThrows(ExecutionException.class, () -> executionConsumer.onTradeRecorded(trade));
    }

    @Test
    void failedExecutionPropagatesWithoutPublishing() {
        when(executionService.execute(trade)).thenThrow(new DataAccessResourceFailureException("Database down"));

        assertThrows(DataAccessResourceFailureException.class, () -> executionConsumer.onTradeRecorded(trade));
        verifyNoInteractions(tradeEventProducer);
    }
}
