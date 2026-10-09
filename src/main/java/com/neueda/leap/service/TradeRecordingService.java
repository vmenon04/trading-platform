package com.neueda.leap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.dto.TradeValidatedDTO;
import com.neueda.leap.enums.TradeStatus;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.repository.AccountTradeStatusMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;

@Service
public class TradeRecordingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeRecordingService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String tradeRecordedTopic;
    private CountDownLatch latch = new CountDownLatch(1);

    private final AccountTradeMapper accountTradeMapper;
    private final AccountTradeStatusMapper accountTradeStatusMapper;

    public TradeRecordingService(KafkaTemplate<String, Object> kafkaTemplate,
                                 ObjectMapper objectMapper,
                                 AccountTradeMapper accountTradeMapper,
                                 AccountTradeStatusMapper accountTradeStatusMapper,
                                 @Value("${trade.recorded}") String tradeRecordedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.accountTradeMapper = accountTradeMapper;
        this.accountTradeStatusMapper = accountTradeStatusMapper;
        this.tradeRecordedTopic = tradeRecordedTopic;
    }

    @KafkaListener(topics = "${trade.validated}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(TradeValidatedDTO tradeValidatedDTO) {
        LOGGER.info("Received validated trade event. Task ID: {}", tradeValidatedDTO.taskId());

        Long tradeId = accountTradeMapper.insert(tradeValidatedDTO);
        accountTradeStatusMapper.insertTradeStatus(tradeId, TradeStatus.SUBMITTED);
        LOGGER.info("Created database entry for trade with ID: {}", tradeId);

        TradeRecordedDTO tradeRecordedDTO = createTradeRecordedDTO(tradeValidatedDTO, tradeId);
        sendMessage(tradeRecordedDTO);

        latch.countDown();
    }

    public void sendMessage(TradeRecordedDTO tradeRecordedDTO) {
        try {
            kafkaTemplate.send(tradeRecordedTopic, objectMapper.writeValueAsString(tradeRecordedDTO));
            LOGGER.info("Published trade recorded event to {}", tradeRecordedTopic);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize trade recorded event", e);
        }
    }

    public void resetLatch() {
        latch = new CountDownLatch(1);
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    private TradeRecordedDTO createTradeRecordedDTO(TradeValidatedDTO tradeValidatedDTO, long tradeId) {
        return new TradeRecordedDTO(
                tradeValidatedDTO.instrumentId(),
                tradeValidatedDTO.accountId(),
                tradeValidatedDTO.side(),
                tradeValidatedDTO.quantity(),
                tradeValidatedDTO.quote(),
                tradeValidatedDTO.taskId(),
                tradeId);
    }
}
