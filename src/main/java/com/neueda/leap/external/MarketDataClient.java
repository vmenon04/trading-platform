package com.neueda.leap.external;

import java.math.BigDecimal;

// wraps our external market data API
public interface MarketDataClient {

    BigDecimal getPrice(String ticker);
}
