package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.dto.TradeSubmittedDTO;
import com.neueda.leap.exception.InvalidTradeException;
import com.neueda.leap.external.MarketDataClient;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@Service
public class RecipientService {

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

    public ResponseEntity<Void> publishOrder(@Valid TradeRequestDTO dto, Long userId) throws AccessDeniedException {
        if (!tradeAuthorizationService.isValidUserId(dto, userId)) {
            throw new AccessDeniedException("You are not authorized to perform this action.");
        }
        UUID externalTradeId = UUID.randomUUID();

        String ticker = instrumentService.getInstrumentById(dto.instrumentId()).getTicker();
        BigDecimal quote = marketDataClient.getPrice(ticker);
        if (quote == null || quote.compareTo(BigDecimal.ZERO) <= 0) {
            // a 400 with this message (GlobalExceptionHandler), so the user sees why instead of a generic 500
            throw new InvalidTradeException("No current market price for " + ticker + ". Try again in a moment.");
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
}
