package com.fiipractic.stocks.dto;

import java.math.BigDecimal;

public record PositionSummaryDTO(
        String symbol,
        BigDecimal totalQuantity,
        BigDecimal averagePurchasePrice,
        BigDecimal currentPrice,       // nullable if not yet refreshed
        BigDecimal invested,
        BigDecimal currentValue,
        BigDecimal profitLoss,
        BigDecimal profitLossPercent
) {}