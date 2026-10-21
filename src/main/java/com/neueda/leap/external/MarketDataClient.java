package com.neueda.leap.external;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

// wraps our external market data API
public interface MarketDataClient {

    BigDecimal getPrice(String ticker);

    // prices for several tickers at once; a ticker with no price right now is left out of the map
    Map<String, BigDecimal> getPrices(Collection<String> tickers);

    // name and type of a symbol, or null if the market data API doesn't know it
    SymbolInfo getSymbol(String symbol);
}
