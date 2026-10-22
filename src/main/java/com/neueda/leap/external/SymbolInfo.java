package com.neueda.leap.external;

// what Fauxnance knows about a symbol (GET /symbols/{symbol}); type is equity, etf, fx, crypto or bond
public record SymbolInfo(String symbol, String name, String type) {
}
