package com.neueda.leap.config;

import tools.jackson.databind.ObjectMapper;
import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.enums.TradeSide;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class KafkaConsumerConfigTest {

    private final RecordMessageConverter converter = new KafkaConsumerConfig().messageConverter();

    private static ConsumerRecord<String, String> consumerRecord(String json) {
        return new ConsumerRecord<>("trade.recorded", 0, 0L, "1", json);
    }

    @Test
    void convertsJsonToTheListenerParameterType() {
        String json = "{\"instrumentId\":10,\"accountId\":1,\"side\":\"BUY\",\"quantity\":5,"
                + "\"quote\":100.25,\"tradeId\":42}";

        Message<?> message = converter.toMessage(consumerRecord(json), null, null, TradeRecordedDTO.class);

        TradeRecordedDTO trade = (TradeRecordedDTO) message.getPayload();
        assertEquals(42L, trade.tradeId());
        assertEquals(1L, trade.accountId());
        assertEquals(10L, trade.instrumentId());
        assertEquals(TradeSide.BUY, trade.side());
        assertEquals(0, trade.quantity().compareTo(new BigDecimal("5")));
        assertEquals(0, trade.quote().compareTo(new BigDecimal("100.25")));
    }

    // what KafkaProducerConfig's JsonSerializer writes must convert back for the listener on the other side
    @Test
    void readsWhatTheProducerWrites() {
        TradeRecordedDTO sent = new TradeRecordedDTO(10L, 1L, TradeSide.BUY, new BigDecimal("5"),
                new BigDecimal("100.25"), 42L);

        Message<?> message = converter.toMessage(consumerRecord(produce(sent)), null, null, TradeRecordedDTO.class);

        assertEquals(sent, message.getPayload());
    }

    // a DTO turned into a JSON string before sending is encoded twice and can't be read back
    @Test
    void rejectsADoubleEncodedMessage() throws Exception {
        TradeRecordedDTO sent = new TradeRecordedDTO(10L, 1L, TradeSide.BUY, new BigDecimal("5"),
                new BigDecimal("100.25"), 42L);
        ConsumerRecord<String, String> doubleEncoded = consumerRecord(produce(new ObjectMapper().writeValueAsString(sent)));

        assertThrows(ConversionException.class,
                () -> converter.toMessage(doubleEncoded, null, null, TradeRecordedDTO.class));
    }

    private static String produce(Object value) {
        try (JacksonJsonSerializer<Object> serializer = new JacksonJsonSerializer<>()) {
            byte[] bytes = serializer.serialize("trade.recorded", value);
            return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
        }
    }

    @Test
    void rejectsMessagesThatAreNotJson() {
        ConsumerRecord<String, String> notJson = consumerRecord("not json");
        assertThrows(ConversionException.class,
                () -> converter.toMessage(notJson, null, null, TradeRecordedDTO.class));
    }

    // --- the error handler: handleOne returns true once the record is given up on (logged and skipped) ----------

    private final DefaultErrorHandler errorHandler = new KafkaConsumerConfig().kafkaErrorHandler();
    private final ConsumerRecord<String, String> failing = consumerRecord("{}");
    private final Consumer<?, ?> consumer = mock(Consumer.class);
    // a mock container isn't running, so the 2 s wait between attempts is skipped
    private final MessageListenerContainer container = mock(MessageListenerContainer.class);

    // the listener's exception reaches the handler wrapped, as it does from a real container
    private boolean fails(Exception cause) {
        return errorHandler.handleOne(new ListenerExecutionFailedException("listener failed", cause),
                failing, consumer, container);
    }

    @Test
    void errorHandler_otherFailure_isRetriedTwiceThenSkipped() {
        IllegalStateException fauxnanceDown = new IllegalStateException("Fauxnance returned 500 for AAPL");

        assertFalse(fails(fauxnanceDown));
        assertFalse(fails(fauxnanceDown));
        assertTrue(fails(fauxnanceDown));
    }

    @Test
    void errorHandler_invalidOrder_isSkippedWithoutARetry() {
        assertTrue(fails(new IllegalArgumentException("Quantity must be positive")));
    }

    @Test
    void errorHandler_unknownAccountOrInstrument_isSkippedWithoutARetry() {
        assertTrue(fails(new NoSuchElementException("No instrument with id 99")));
    }
}
