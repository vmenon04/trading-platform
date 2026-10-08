package com.neueda.leap.entity;

import com.neueda.leap.enums.TradeStatus;

import java.time.OffsetDateTime;

public class AccountTradeStatus {

    private TradeStatus tradeStatus;
    private OffsetDateTime tradeTime;

    public TradeStatus getTradeStatus() {
        return tradeStatus;
    }

    public void setTradeStatus(TradeStatus tradeStatus) {
        this.tradeStatus = tradeStatus;
        this.tradeTime = OffsetDateTime.now();
    }

    public OffsetDateTime getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(OffsetDateTime tradeTime) {
        this.tradeTime = tradeTime;
    }
}
