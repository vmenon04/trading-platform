package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.dto.TradeSubmittedDTO;
import com.neueda.leap.entity.Instrument;
import com.neueda.leap.events.TradeSubmittedEvent;
import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.external.MarketDataClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipientServiceTest {

    @Mock
    private TradeEventProducer tradeEventProducer;

    @Mock
    private InstrumentService instrumentService;

    @Mock
    private MarketDataClient marketDataClient;

    @InjectMocks
    private RecipientService recipientService;

    @BeforeEach
    void setUp() throws Exception {
        resetJobIdGenerator();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/accounts/123/trades");
        request.setScheme("http");
        request.setServerName("localhost");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publishOrder_returnsAcceptedWithTaskLocation() {
        TradeRequestDTO dto = new TradeRequestDTO(
                UUID.randomUUID(),
                10,
                TradeSide.BUY,
                new BigDecimal("25.5")
        );

        Instrument instrument = new Instrument();
        instrument.setTicker("AAPL");
        when(instrumentService.getInstrumentById(10L)).thenReturn(instrument);
        when(marketDataClient.getPrice("AAPL")).thenReturn(new BigDecimal("101.25"));

        ResponseEntity<Void> response = recipientService.publishOrder(dto);

        assertEquals(202, response.getStatusCode().value());
        assertNotNull(response.getHeaders().getLocation());
        assertEquals("/api/accounts/123/trades/tasks/1", response.getHeaders().getLocation().getPath());
    }

    @Test
    void publishOrder_publishesTradeSubmittedEventWithExpectedPayload() {
        UUID accountId = UUID.randomUUID();
        TradeRequestDTO dto = new TradeRequestDTO(
                accountId,
                44,
                TradeSide.SELL,
                new BigDecimal("7")
        );

        Instrument instrument = new Instrument();
        instrument.setTicker("MSFT");
        when(instrumentService.getInstrumentById(44L)).thenReturn(instrument);
        when(marketDataClient.getPrice("MSFT")).thenReturn(new BigDecimal("91.10"));

        recipientService.publishOrder(dto);

        ArgumentCaptor<TradeSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(TradeSubmittedEvent.class);
        verify(tradeEventProducer).publishTradeSubmitted(eventCaptor.capture());

        TradeSubmittedEvent event = eventCaptor.getValue();
        assertEquals(accountId, event.accountId());
        assertNotNull(event.eventId());
        assertNotNull(event.timestamp());
        assertNotNull(event.dto());
        assertEquals(TradeSubmittedDTO.class, event.dto().getClass());
        assertEquals(44, event.dto().instrumentId());
        assertEquals(accountId, event.dto().accountId());
        assertEquals(TradeSide.SELL, event.dto().side());
        assertEquals(0, event.dto().quantity().compareTo(new BigDecimal("7")));
        assertEquals(0, event.dto().quote().compareTo(new BigDecimal("91.10")));
        assertEquals(1L, event.dto().taskId());
    }

    private static void resetJobIdGenerator() throws Exception {
        Field field = RecipientService.class.getDeclaredField("JOB_ID_GENERATOR");
        field.setAccessible(true);
        AtomicLong generator = (AtomicLong) field.get(null);
        generator.set(1L);
    }
}
