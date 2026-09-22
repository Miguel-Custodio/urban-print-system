package com.urbanprint.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "product_price_scales")
public class ProductPriceScale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity; // e.g. 100, 250, 500, 1000

    @Column(precision = 10, scale = 2)
    private BigDecimal totalCost; // Internal cost for this quantity (e.g. $18.00)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice; // Selling price to customer (e.g. $45.00)

    @Column(precision = 10, scale = 4)
    private BigDecimal pricePerPiece; // Auto-calculated (e.g. 45 / 250 = $0.1800)

    @Column(precision = 10, scale = 2)
    private BigDecimal markupPercentage; // Markup %

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference
    private Product product;

    public ProductPriceScale() {
    }

    public ProductPriceScale(Integer quantity, BigDecimal totalCost, BigDecimal totalPrice) {
        this.quantity = quantity;
        this.totalCost = totalCost;
        this.totalPrice = totalPrice;
        this.calculateDerivedFields();
    }

    @PrePersist
    @PreUpdate
    public void calculateDerivedFields() {
        if (this.quantity != null && this.quantity > 0 && this.totalPrice != null) {
            this.pricePerPiece = this.totalPrice.divide(BigDecimal.valueOf(this.quantity), 4, RoundingMode.HALF_UP);
        }
        if (this.totalCost != null && this.totalCost.compareTo(BigDecimal.ZERO) > 0 && this.totalPrice != null) {
            BigDecimal profit = this.totalPrice.subtract(this.totalCost);
            this.markupPercentage = profit.divide(this.totalCost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getPricePerPiece() {
        return pricePerPiece;
    }

    public void setPricePerPiece(BigDecimal pricePerPiece) {
        this.pricePerPiece = pricePerPiece;
    }

    public BigDecimal getMarkupPercentage() {
        return markupPercentage;
    }

    public void setMarkupPercentage(BigDecimal markupPercentage) {
        this.markupPercentage = markupPercentage;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}