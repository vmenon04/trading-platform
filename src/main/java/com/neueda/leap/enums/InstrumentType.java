package com.neueda.leap.enums;

// match the values stored in instruments.asset_class (MyBatis maps enums by name)
public enum InstrumentType {
    STOCK,
    ETF,
    BOND,
    FOREX,
    CRYPTO
}
