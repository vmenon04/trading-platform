package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.dto.TradeSubmittedDTO;
import com.neueda.leap.external.MarketDataClient;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RecipientService {

    private static final AtomicLong JOB_ID_GENERATOR = new AtomicLong(1);
    private final TradeEventProducer tradeEventProducer;
    private final InstrumentService instrumentService;
    private final MarketDataClient marketDataClient;
    private final TradeAuthorizationService tradeAuthorizationService;

    public RecipientService(TradeEventProducer tradeEventProducer,
                            InstrumentService instrumentService,
                            MarketDataClient marketDataClient,
                            TradeAuthorizationService tradeAuthorizationService) {
        this.tradeEventProducer = tradeEventProducer;
        this.instrumentService = instrumentService;
        this.marketDataClient = marketDataClient;
        this.tradeAuthorizationService = tradeAuthorizationService;
    }

    public ResponseEntity<Void> publishOrder(@Valid TradeRequestDTO dto, String username) throws AccessDeniedException {
        if (!tradeAuthorizationService.isValidUsername(dto, username)) {
            throw new AccessDeniedException("You are not authorized to perform this action.");
        }

        long taskId = JOB_ID_GENERATOR.getAndIncrement();

        String ticker = instrumentService.getInstrumentById(dto.instrumentId()).getTicker();
        BigDecimal quote = marketDataClient.getPrice(ticker);
        if (quote == null || quote.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No valid market price for " + ticker);
        }

        TradeSubmittedDTO tradeSubmitted = new TradeSubmittedDTO(
                dto.instrumentId(),
                dto.accountId(),
                dto.side(),
                dto.quantity(),
                taskId
        );
        tradeEventProducer.publishTradeSubmitted(tradeSubmitted);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/tasks/{taskId}")
                .buildAndExpand(taskId)
                .toUri();

        return ResponseEntity
                .accepted()
                .location(location)
                .build();
    }
}
