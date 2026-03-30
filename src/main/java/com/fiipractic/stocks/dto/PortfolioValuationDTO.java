package com.fiipractic.stocks.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PortfolioValuationDTO(
        Long portfolioId,
        String portfolioName,
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal profitLoss,
        BigDecimal profitLossPercent,
        List<PositionSummaryDTO> positions,
        LocalDateTime lastUpdated
) {}