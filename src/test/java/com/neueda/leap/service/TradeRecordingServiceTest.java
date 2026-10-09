package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.dto.TradeValidatedDTO;
import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.enums.TradeStatus;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.repository.AccountTradeStatusMapper;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeRecordingServiceTest {

    @Mock
    private TradeEventProducer tradeEventProducer;

    @Mock
    private AccountTradeMapper accountTradeMapper;

    @Mock
    private AccountTradeStatusMapper accountTradeStatusMapper;

    private TradeRecordingService tradeRecordingService;

    private final TradeValidatedDTO validatedDTO = new TradeValidatedDTO(
            1L,
            2L,
            TradeSide.BUY,
            new BigDecimal("100"),
            new BigDecimal("10.0"),
            3L);

    @BeforeEach
    void setUp() {
        tradeRecordingService = new TradeRecordingService(
                tradeEventProducer,
                accountTradeMapper,
                accountTradeStatusMapper);
    }

    @Test
    void recordsTradeAsPendingAndPublishesIt() throws Exception {
        when(accountTradeMapper.insert(validatedDTO)).thenReturn(4L);
        when(tradeEventProducer.publishTradeRecorded(any())).thenReturn(CompletableFuture.completedFuture(null));

        tradeRecordingService.consume(validatedDTO);

        verify(accountTradeMapper).insert(validatedDTO);
        verify(accountTradeStatusMapper).insertTradeStatus(4L, TradeStatus.PENDING);

        ArgumentCaptor<TradeRecordedDTO> captor = ArgumentCaptor.forClass(TradeRecordedDTO.class);
        verify(tradeEventProducer).publishTradeRecorded(captor.capture());
        TradeRecordedDTO tradeRecordedDTO = captor.getValue();
        assertEquals(1L, tradeRecordedDTO.instrumentId());
        assertEquals(2L, tradeRecordedDTO.accountId());
        assertEquals(TradeSide.BUY, tradeRecordedDTO.side());
        assertEquals(new BigDecimal("100"), tradeRecordedDTO.quantity());
        assertEquals(new BigDecimal("10.0"), tradeRecordedDTO.quote());
        assertEquals(3L, tradeRecordedDTO.taskId());
        assertEquals(4L, tradeRecordedDTO.tradeId());
    }

    @Test
    void failedPublishFailsTheListenerSoTheMessageIsRedelivered() {
        when(accountTradeMapper.insert(validatedDTO)).thenReturn(4L);
        when(tradeEventProducer.publishTradeRecorded(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        assertThrows(ExecutionException.class, () -> tradeRecordingService.consume(validatedDTO));
    }
}
