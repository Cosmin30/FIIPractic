package com.fiipractic.stocks.dto;

import java.util.List;

public record RefreshResponseDTO(
        String portfolioId,
        List<String> symbolsQueued,
        int totalSymbols,
        String message
) {}

