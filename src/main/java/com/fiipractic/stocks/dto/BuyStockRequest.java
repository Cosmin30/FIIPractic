package com.fiipractic.stocks.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public class BuyStockRequest {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be positive")
    @Digits(integer = 15, fraction = 4, message = "Quantity supports up to 4 decimals")
    private BigDecimal quantity;

    @DecimalMin(value = "0.01", message = "Purchase price must be positive")
    private BigDecimal purchasePrice;

    public BuyStockRequest() {
    }

    public BuyStockRequest(String symbol, BigDecimal quantity, BigDecimal purchasePrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }
}


