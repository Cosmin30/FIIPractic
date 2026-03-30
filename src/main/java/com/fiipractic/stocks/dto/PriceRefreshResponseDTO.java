package com.fiipractic.stocks.dto;

public record PriceRefreshResponseDTO(
        String status,
        String symbol,
        String message
) {}