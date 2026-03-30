package com.fiipractic.stocks.dto;

public record StockDTO(Long id, String symbol, java.math.BigDecimal currentPrice,
                       java.time.LocalDateTime lastPriceUpdate) {
}