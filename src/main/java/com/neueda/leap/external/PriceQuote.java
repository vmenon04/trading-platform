package com.neueda.leap.external;

import java.math.BigDecimal;

// a price from Fauxnance; stale means it is past Fauxnance's freshness window (the last known price), which is
// fine to show but not to trade at
public record PriceQuote(BigDecimal price, boolean stale) {
}
