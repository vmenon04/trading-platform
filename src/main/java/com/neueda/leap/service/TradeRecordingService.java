package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.dto.TradeValidatedDTO;
import com.neueda.leap.enums.TradeStatus;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.repository.AccountTradePriceMapper;
import com.neueda.leap.repository.AccountTradeStatusMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeRecordingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeRecordingService.class);

    private final KafkaTemplate<String, TradeRecordedDTO> kafkaTemplate;
    private final String tradeRecordedTopic;

    private AccountTradeMapper accountTradeMapper;
    private AccountTradeStatusMapper accountTradeStatusMapper;
    private AccountTradePriceMapper accountTradePriceMapper;

    public TradeRecordingService(KafkaTemplate<String, TradeRecordedDTO> kafkaTemplate,
                                 @Value("${trade.recorded}") String tradeRecordedTopic) {
        this.kafkaTemplate = kafkaTemplate;
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

    }

    public void sendMessage(TradeRecordedDTO tradeRecordedDTO) {
        kafkaTemplate.send(tradeRecordedTopic,tradeRecordedDTO);
        LOGGER.info("Published trade recorded event to {}", tradeRecordedTopic);
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
