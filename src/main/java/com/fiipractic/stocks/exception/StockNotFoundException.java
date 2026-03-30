package com.fiipractic.stocks.exception;

public class StockNotFoundException extends RuntimeException {
  public StockNotFoundException(String message) {
    super(message);
  }
}
