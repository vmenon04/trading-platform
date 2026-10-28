package com.neueda.leap.exception;

public class PriceUnavailableException extends IllegalStateException {

    public PriceUnavailableException(String message) {
        super(message);
    }
}
