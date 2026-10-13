package com.neueda.leap.kafka;

import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.dto.TradeValidatedDTO;
import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.service.TradeEventProducer;
import com.neueda.leap.service.TradeRecordingService;
import java.math.BigDecimal;
import java.util.UUID;
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
class RecordingConsumerTest {

    @Mock
    private TradeRecordingService tradeRecordingService;

    @Mock
    private TradeEventProducer tradeEventProducer;

    private RecordingConsumer recordingConsumer;

    private final TradeValidatedDTO trade = new TradeValidatedDTO(
            10L, 1L, TradeSide.BUY, new BigDecimal("5"), new BigDecimal("99"), 3L, UUID.randomUUID());

    private final TradeRecordedDTO recorded = new TradeRecordedDTO(
            10L, 1L, TradeSide.BUY, new BigDecimal("5"), new BigDecimal("99"), 3L, 42L);

    @BeforeEach
    void setUp() {
        recordingConsumer = new RecordingConsumer(tradeRecordingService, tradeEventProducer);
    }

    @Test
    void recordsTradeAndPublishesIt() throws Exception {
        when(tradeRecordingService.record(trade)).thenReturn(recorded);
        when(tradeEventProducer.publishTradeRecorded(recorded)).thenReturn(CompletableFuture.completedFuture(null));

        recordingConsumer.onTradeValidated(trade);

        verify(tradeEventProducer).publishTradeRecorded(recorded);
    }

    @Test
    void failedPublishFailsTheListenerSoTheMessageIsRedelivered() {
        when(tradeRecordingService.record(trade)).thenReturn(recorded);
        when(tradeEventProducer.publishTradeRecorded(recorded))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("Broker unavailable")));

        assertThrows(ExecutionException.class, () -> recordingConsumer.onTradeValidated(trade));
    }

    @Test
    void failedRecordingPropagatesWithoutPublishing() {
        when(tradeRecordingService.record(trade)).thenThrow(new DataAccessResourceFailureException("Database down"));

        assertThrows(DataAccessResourceFailureException.class, () -> recordingConsumer.onTradeValidated(trade));
        verifyNoInteractions(tradeEventProducer);
    }
}
