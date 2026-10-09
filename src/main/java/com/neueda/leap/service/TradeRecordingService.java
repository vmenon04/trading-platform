package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.dto.TradeValidatedDTO;
import com.neueda.leap.enums.TradeStatus;
import com.neueda.leap.kafka.KafkaTopics;
import com.neueda.leap.repository.AccountTradeMapper;
import com.neueda.leap.repository.AccountTradeStatusMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;

@Service
public class TradeRecordingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeRecordingService.class);

    private final TradeEventProducer tradeEventProducer;
    private final AccountTradeMapper accountTradeMapper;
    private final AccountTradeStatusMapper accountTradeStatusMapper;

    public TradeRecordingService(TradeEventProducer tradeEventProducer,
                                 AccountTradeMapper accountTradeMapper,
                                 AccountTradeStatusMapper accountTradeStatusMapper) {
        this.tradeEventProducer = tradeEventProducer;
        this.accountTradeMapper = accountTradeMapper;
        this.accountTradeStatusMapper = accountTradeStatusMapper;
    }

    // waits for the send so a failed publish rolls back the inserts and the message is redelivered
    // (rollbackFor: by default @Transactional would commit on the checked exceptions .get() throws)
    @KafkaListener(topics = KafkaTopics.TRADE_VALIDATED, groupId = "recording-service")
    @Transactional(rollbackFor = Exception.class)
    public void consume(TradeValidatedDTO tradeValidatedDTO) throws InterruptedException, ExecutionException {
        LOGGER.info("Received validated trade event. Task ID: {}", tradeValidatedDTO.taskId());

        Long tradeId = accountTradeMapper.insert(tradeValidatedDTO);
        accountTradeStatusMapper.insertTradeStatus(tradeId, TradeStatus.PENDING);
        LOGGER.info("Created database entry for trade with ID: {}", tradeId);

        TradeRecordedDTO tradeRecordedDTO = createTradeRecordedDTO(tradeValidatedDTO, tradeId);
        tradeEventProducer.publishTradeRecorded(tradeRecordedDTO).get();
        LOGGER.info("Published trade recorded event to {}", KafkaTopics.TRADE_RECORDED);
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
