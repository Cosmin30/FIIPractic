package com.fiipractic.stocks.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HoldingDTO(
        Long id,
        String symbol,
        BigDecimal quantity,
        BigDecimal purchasePrice,
        LocalDateTime purchasedAt
) {}
