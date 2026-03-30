package com.fiipractic.stocks.exception;

public class PortfolioLimitException extends RuntimeException {
    public PortfolioLimitException(String message) {
        super(message);
    }
}

