package com.fiipractic.stocks.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HoldingDTO {
    private Long id;
    private String symbol;
    private Integer quantity;
    private BigDecimal purchasePrice;
    private LocalDateTime purchasedAt;

    public HoldingDTO() {
    }

    public HoldingDTO(Long id, String symbol, Integer quantity, BigDecimal purchasePrice, LocalDateTime purchasedAt) {
        this.id = id;
        this.symbol = symbol;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchasedAt = purchasedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(LocalDateTime purchasedAt) { this.purchasedAt = purchasedAt; }
}
