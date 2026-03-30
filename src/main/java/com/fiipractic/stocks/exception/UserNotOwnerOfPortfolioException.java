package com.fiipractic.stocks.exception;

public class UserNotOwnerOfPortfolioException extends RuntimeException {
    public UserNotOwnerOfPortfolioException(String message) {
        super(message);
    }
}

