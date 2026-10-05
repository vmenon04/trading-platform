package com.neueda.leap.events;

import com.neueda.leap.dto.TradeSubmittedDTO;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID accountId,
        UUID eventId,
        Instant timestamp,
        TradeSubmittedDTO dto) {
}
