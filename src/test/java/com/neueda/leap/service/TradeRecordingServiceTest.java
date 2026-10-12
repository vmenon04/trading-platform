package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.dto.TradeValidatedDTO;
import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.enums.TradeStatus;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.repository.AccountTradeStatusMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeRecordingServiceTest {

    private static final UUID EXTERNAL_TRADE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

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
            3L,
            EXTERNAL_TRADE_ID);

    @BeforeEach
    void setUp() {
        tradeRecordingService = new TradeRecordingService(
                accountTradeMapper,
                accountTradeStatusMapper);
    }

    @Test
    void recordsTradeAsPendingAndReturnsIt() {
        when(accountTradeMapper.insert(validatedDTO)).thenReturn(4L);

        TradeRecordedDTO tradeRecordedDTO = tradeRecordingService.record(validatedDTO);

        verify(accountTradeMapper).insert(validatedDTO);
        verify(accountTradeStatusMapper).insertTradeStatus(4L, TradeStatus.PENDING);
        assertEquals(1L, tradeRecordedDTO.instrumentId());
        assertEquals(2L, tradeRecordedDTO.accountId());
        assertEquals(TradeSide.BUY, tradeRecordedDTO.side());
        assertEquals(new BigDecimal("100"), tradeRecordedDTO.quantity());
        assertEquals(new BigDecimal("10.0"), tradeRecordedDTO.quote());
        assertEquals(3L, tradeRecordedDTO.taskId());
        assertEquals(4L, tradeRecordedDTO.tradeId());
    }

    @Test
    void alreadyRecordedTradeReturnsExistingTradeWithoutRecordingAgain() {
        when(accountTradeMapper.insert(validatedDTO)).thenReturn(null);
        when(accountTradeMapper.findTradeIdByExternalTradeId(EXTERNAL_TRADE_ID)).thenReturn(4L);

        TradeRecordedDTO tradeRecordedDTO = tradeRecordingService.record(validatedDTO);

        verify(accountTradeStatusMapper, never()).insertTradeStatus(any(), any());
        assertEquals(4L, tradeRecordedDTO.tradeId());
    }
}
