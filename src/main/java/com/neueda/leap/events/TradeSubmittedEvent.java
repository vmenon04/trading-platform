package com.neueda.leap.events;

import com.neueda.leap.dto.TradeSubmittedDTO;

import java.time.Instant;
import java.util.UUID;

public record TradeSubmittedEvent(
        UUID accountId,
        UUID eventId,
        Instant timestamp,
        TradeSubmittedDTO dto) {
}
