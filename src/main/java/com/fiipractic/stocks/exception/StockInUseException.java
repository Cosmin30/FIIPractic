package com.fiipractic.stocks.exception;

public class StockInUseException extends RuntimeException {
    public StockInUseException(String message) {
        super(message);
    }
}
