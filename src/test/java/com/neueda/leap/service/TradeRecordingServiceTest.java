package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.dto.TradeValidatedDTO;
import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.enums.TradeStatus;
import com.neueda.leap.kafka.KafkaTopics;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.repository.AccountTradeStatusMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeRecordingServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private AccountTradeMapper accountTradeMapper;

    @Mock
    private AccountTradeStatusMapper accountTradeStatusMapper;

    private TradeRecordingService tradeRecordingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tradeRecordingService = new TradeRecordingService(
                kafkaTemplate,
                objectMapper,
                accountTradeMapper,
                accountTradeStatusMapper);
    }

    @Test
    void testMessageProduction() throws Exception {
        TradeValidatedDTO validatedDTO = new TradeValidatedDTO(
                1L,
                2L,
                TradeSide.BUY,
                new BigDecimal("100"),
                new BigDecimal("10.0"),
                3L);

        when(accountTradeMapper.insert(validatedDTO)).thenReturn(4L);
        doNothing().when(accountTradeStatusMapper).insertTradeStatus(4L, TradeStatus.SUBMITTED);

        tradeRecordingService.consume(validatedDTO);

        assertTrue(tradeRecordingService.getLatch().await(1, TimeUnit.SECONDS));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.TRADE_RECORDED), payloadCaptor.capture());

        TradeRecordedDTO tradeRecordedDTO = objectMapper.readValue(payloadCaptor.getValue(), TradeRecordedDTO.class);
        assertEquals(1L, tradeRecordedDTO.instrumentId());
        assertEquals(2L, tradeRecordedDTO.accountId());
        assertEquals(TradeSide.BUY, tradeRecordedDTO.side());
        assertEquals(new BigDecimal("100"), tradeRecordedDTO.quantity());
        assertEquals(new BigDecimal("10.0"), tradeRecordedDTO.quote());
        assertEquals(3L, tradeRecordedDTO.taskId());
        assertEquals(4L, tradeRecordedDTO.tradeId());

        verify(accountTradeMapper).insert(validatedDTO);
        verify(accountTradeStatusMapper).insertTradeStatus(4L, TradeStatus.SUBMITTED);
    }
}