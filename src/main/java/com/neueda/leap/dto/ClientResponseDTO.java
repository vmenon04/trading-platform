package com.neueda.leap.dto;

import java.util.List;
import java.util.UUID;

public record ClientResponseDTO(
        UUID clientId,
        String clientName,
        List<AccountDTO> tradingAccounts
) {}
