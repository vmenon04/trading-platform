package com.neueda.leap.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.enums.TradeSide;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaConsumerConfigTest {

    private final RecordMessageConverter converter = new KafkaConsumerConfig().messageConverter();

    private static ConsumerRecord<String, String> record(String json) {
        return new ConsumerRecord<>("trade.recorded", 0, 0L, "1", json);
    }

    @Test
    void convertsJsonToTheListenerParameterType() {
        String json = "{\"instrumentId\":10,\"accountId\":1,\"side\":\"BUY\",\"quantity\":5,"
                + "\"quote\":100.25,\"taskId\":3,\"tradeId\":42}";

        Message<?> message = converter.toMessage(record(json), null, null, TradeRecordedDTO.class);

        TradeRecordedDTO trade = (TradeRecordedDTO) message.getPayload();
        assertEquals(42L, trade.tradeId());
        assertEquals(1L, trade.accountId());
        assertEquals(10L, trade.instrumentId());
        assertEquals(TradeSide.BUY, trade.side());
        assertEquals(0, trade.quantity().compareTo(new BigDecimal("5")));
        assertEquals(0, trade.quote().compareTo(new BigDecimal("100.25")));
        assertEquals(3L, trade.taskId());
    }

    // what KafkaProducerConfig's JsonSerializer writes must convert back for the listener on the other side
    @Test
    void readsWhatTheProducerWrites() {
        TradeRecordedDTO sent = new TradeRecordedDTO(10L, 1L, TradeSide.BUY, new BigDecimal("5"),
                new BigDecimal("100.25"), 3L, 42L);

        Message<?> message = converter.toMessage(record(produce(sent)), null, null, TradeRecordedDTO.class);

        assertEquals(sent, message.getPayload());
    }

    // a DTO turned into a JSON string before sending is encoded twice and can't be read back
    @Test
    void rejectsADoubleEncodedMessage() throws Exception {
        TradeRecordedDTO sent = new TradeRecordedDTO(10L, 1L, TradeSide.BUY, new BigDecimal("5"),
                new BigDecimal("100.25"), 3L, 42L);
        String doubleEncoded = produce(new ObjectMapper().writeValueAsString(sent));

        assertThrows(ConversionException.class,
                () -> converter.toMessage(record(doubleEncoded), null, null, TradeRecordedDTO.class));
    }

    private static String produce(Object value) {
        try (JsonSerializer<Object> serializer = new JsonSerializer<>()) {
            return new String(serializer.serialize("trade.recorded", value), StandardCharsets.UTF_8);
        }
    }

    @Test
    void rejectsMessagesThatAreNotJson() {
        assertThrows(ConversionException.class,
                () -> converter.toMessage(record("not json"), null, null, TradeRecordedDTO.class));
    }
}
