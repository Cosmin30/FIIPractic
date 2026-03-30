package com.fiipractic.stocks.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "portfolio_holdings")
@EntityListeners(AuditingEntityListener.class)
public class PortfolioHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal purchasePrice;

    @CreatedDate
    private LocalDateTime purchasedAt;

    public PortfolioHolding() {
    }

    public PortfolioHolding(Long id, Portfolio portfolio, Stock stock,
                            Integer quantity, BigDecimal purchasePrice,
                            LocalDateTime purchasedAt) {
        this.id = id;
        this.portfolio = portfolio;
        this.stock = stock;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchasedAt = purchasedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Portfolio getPortfolio() { return portfolio; }
    public void setPortfolio(Portfolio portfolio) { this.portfolio = portfolio; }

    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(LocalDateTime purchasedAt) { this.purchasedAt = purchasedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioHolding that = (PortfolioHolding) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Builder {
        private Long id;
        private Portfolio portfolio;
        private Stock stock;
        private Integer quantity;
        private BigDecimal purchasePrice;
        private LocalDateTime purchasedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder portfolio(Portfolio portfolio) { this.portfolio = portfolio; return this; }
        public Builder stock(Stock stock) { this.stock = stock; return this; }
        public Builder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public Builder purchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; return this; }
        public Builder purchasedAt(LocalDateTime purchasedAt) { this.purchasedAt = purchasedAt; return this; }

        public PortfolioHolding build() {
            return new PortfolioHolding(id, portfolio, stock, quantity, purchasePrice, purchasedAt);
        }
    }
}
