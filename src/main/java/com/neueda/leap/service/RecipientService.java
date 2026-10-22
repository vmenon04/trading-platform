package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.dto.TradeSubmittedDTO;
import com.neueda.leap.external.MarketDataClient;
import com.neueda.leap.repository.AccountTradeMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@Service
@Validated
public class RecipientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipientService.class);

    private final TradeEventProducer tradeEventProducer;
    private final InstrumentService instrumentService;
    private final MarketDataClient marketDataClient;
    private final TradeAuthorizationService tradeAuthorizationService;
    private final AccountTradeMapper accountTradeMapper;

    public RecipientService(TradeEventProducer tradeEventProducer,
                            InstrumentService instrumentService,
                            MarketDataClient marketDataClient,
                            TradeAuthorizationService tradeAuthorizationService,
                            AccountTradeMapper accountTradeMapper) {
        this.tradeEventProducer = tradeEventProducer;
        this.instrumentService = instrumentService;
        this.marketDataClient = marketDataClient;
        this.tradeAuthorizationService = tradeAuthorizationService;
        this.accountTradeMapper = accountTradeMapper;
    }

    public ResponseEntity<Void> publishOrder(@Valid TradeRequestDTO dto, Long userId) throws AccessDeniedException {
        if (!tradeAuthorizationService.isValidUserId(dto, userId)) {
            throw new AccessDeniedException("You are not authorized to perform this action.");
        }

        UUID externalTradeId = UUID.randomUUID();
        while(checkTradeIdExists(externalTradeId)){
             LOGGER.info("Trade ID {} already exists in the database, regenerating.", externalTradeId);
             externalTradeId = UUID.randomUUID();
        }

        String ticker = instrumentService.getInstrumentById(dto.instrumentId()).getTicker();
        BigDecimal quote = marketDataClient.getPrice(ticker);
        if (quote == null || quote.compareTo(BigDecimal.ZERO) <= 0) {
            LOGGER.error("No valid market price for {}, requested by user {}", ticker, userId);
            throw new IllegalStateException("No valid market price for " + ticker);
        }

        TradeSubmittedDTO tradeSubmitted = new TradeSubmittedDTO(
                dto.instrumentId(),
                dto.accountId(),
                dto.side(),
                dto.quantity(),
                externalTradeId
        );

        tradeEventProducer.publishTradeSubmitted(tradeSubmitted);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{externalTradeId}")
                .buildAndExpand(externalTradeId)
                .toUri();

        return ResponseEntity
                .accepted()
                .location(location)
                .build();
    }

    private boolean checkTradeIdExists(UUID tradeId) {
        return accountTradeMapper.findTradeIdByExternalTradeId(tradeId) != null;
    }
}
