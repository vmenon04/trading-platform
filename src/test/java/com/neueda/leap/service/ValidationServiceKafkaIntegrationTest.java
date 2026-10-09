package com.neueda.leap.service;

import com.neueda.leap.dto.TradeSubmittedDTO;
import com.neueda.leap.dto.TradeValidatedDTO;
import com.neueda.leap.entity.Instrument;
import com.neueda.leap.enums.InstrumentType;
import com.neueda.leap.enums.TradeSide;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ValidationServiceKafkaIntegrationTest.KafkaTestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {"trade.submitted", "trade.validated"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=validation-it-group"
})
class ValidationServiceKafkaIntegrationTest {

    private static final UUID EXTERNAL_ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Long ACCOUNT_ID = 1L;
    private static final Long INSTRUMENT_ID = 10L;
    private static final BigDecimal PRICE = new BigDecimal("100.50");
    private static final long TASK_ID = 88L;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    @Qualifier("kafkaTemplate")
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private InstrumentService instrumentService;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AccountHoldingService accountHoldingService;

    private Consumer<String, TradeValidatedDTO> validatedConsumer;

    @BeforeEach
    void setUp() {
        when(instrumentService.getInstrumentById(INSTRUMENT_ID))
                .thenReturn(new Instrument("Apple Inc", "AAPL", InstrumentType.STOCK));
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID)).thenReturn(PRICE);
        when(accountService.getAccountIdByExternalAccountId(EXTERNAL_ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
        when(accountService.getBalance(ACCOUNT_ID)).thenReturn(new BigDecimal("1000.00"));
        when(accountHoldingService.getQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(new BigDecimal("10"));

        Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils.consumerProps(
                "validation-it-assert-group",
                "true",
                embeddedKafkaBroker));
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.neueda.leap.dto,com.neueda.leap.enums,java.util,java.math");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TradeValidatedDTO.class.getName());
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        validatedConsumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(TradeValidatedDTO.class)
        ).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(validatedConsumer, "trade.validated");
    }

    @AfterEach
    void tearDown() {
        if (validatedConsumer != null) {
            validatedConsumer.close(Duration.ofSeconds(1));
        }
    }

    @Test
    void submittedTrade_isValidated_andPublishedToValidatedTopic() {
        TradeSubmittedDTO submitted = new TradeSubmittedDTO(
                INSTRUMENT_ID,
                EXTERNAL_ACCOUNT_ID,
                TradeSide.BUY,
                new BigDecimal("5.25"),
                TASK_ID
        );

        kafkaTemplate.send("trade.submitted", EXTERNAL_ACCOUNT_ID.toString(), submitted);
        kafkaTemplate.flush();

        ConsumerRecord<String, TradeValidatedDTO> record =
                KafkaTestUtils.getSingleRecord(validatedConsumer, "trade.validated");
        TradeValidatedDTO validated = record.value();

        assertAll(
                () -> assertEquals(INSTRUMENT_ID, validated.instrumentId()),
                () -> assertEquals(ACCOUNT_ID, validated.accountId()),
                () -> assertEquals(TradeSide.BUY, validated.side()),
                () -> assertEquals(new BigDecimal("5.25"), validated.quantity()),
                () -> assertEquals(PRICE, validated.quote()),
                () -> assertEquals(TASK_ID, validated.taskId())
        );
    }

    @Configuration
    @EnableKafka
    static class KafkaTestConfig {

        @Bean
        ValidationService validationService(InstrumentService instrumentService,
                                            AccountService accountService,
                                            AccountHoldingService accountHoldingService,
                                            TradeEventProducer tradeEventProducer) {
            return new ValidationService(instrumentService, accountService, accountHoldingService, tradeEventProducer);
        }

        @Bean
        TradeEventProducer tradeEventProducer(@Qualifier("kafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
            return new TradeEventProducer(kafkaTemplate);
        }

        @Bean(name = "kafkaTemplate")
        KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        ProducerFactory<String, Object> producerFactory(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            Map<String, Object> config = new HashMap<>();
            config.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            config.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            return new DefaultKafkaProducerFactory<>(config);
        }

        @Bean
        ConsumerFactory<String, TradeSubmittedDTO> consumerFactory(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                @Value("${spring.kafka.consumer.group-id}") String groupId) {
            Map<String, Object> config = new HashMap<>();
            config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.neueda.leap.dto,com.neueda.leap.enums,java.util,java.math");
            config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TradeSubmittedDTO.class.getName());
            config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
            return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), new JsonDeserializer<>(TradeSubmittedDTO.class));
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<String, TradeSubmittedDTO> kafkaListenerContainerFactory(
                ConsumerFactory<String, TradeSubmittedDTO> consumerFactory) {
            ConcurrentKafkaListenerContainerFactory<String, TradeSubmittedDTO> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory);
            return factory;
        }
    }
}


