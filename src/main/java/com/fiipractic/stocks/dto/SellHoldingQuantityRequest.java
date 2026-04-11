package com.fiipractic.stocks.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SellHoldingQuantityRequest {

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be positive")
    @Digits(integer = 15, fraction = 4, message = "Quantity supports up to 4 decimals")
    private BigDecimal quantity;

    public SellHoldingQuantityRequest() {
    }

    public SellHoldingQuantityRequest(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}