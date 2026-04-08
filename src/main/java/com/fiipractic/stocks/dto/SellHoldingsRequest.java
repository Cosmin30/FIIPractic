package com.fiipractic.stocks.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class SellHoldingsRequest {

    @NotEmpty(message = "holdingIds must not be empty")
    private List<@NotNull(message = "holdingId must not be null") Long> holdingIds;

    public SellHoldingsRequest() {
    }

    public SellHoldingsRequest(List<Long> holdingIds) {
        this.holdingIds = holdingIds;
    }

    public List<Long> getHoldingIds() {
        return holdingIds;
    }
}

