package com.urbanprint.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "quote_items")
public class QuoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    @JsonBackReference
    private Quote quote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String itemDescription; // Pre-filled from product + customized for this quote

    private Integer quantity; // e.g. 250

    @Column(precision = 10, scale = 4)
    private BigDecimal unitPrice; // e.g. $0.1800

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice; // e.g. $45.00

    private String printSpecsSummary; // e.g. "3.5x2, 14pt Coated Cover, 4/4"

    public QuoteItem() {
    }

    public QuoteItem(Product product, String itemDescription, Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice, String printSpecsSummary) {
        this.product = product;
        this.itemDescription = itemDescription;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.printSpecsSummary = printSpecsSummary;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Quote getQuote() {
        return quote;
    }

    public void setQuote(Quote quote) {
        this.quote = quote;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getPrintSpecsSummary() {
        return printSpecsSummary;
    }

    public void setPrintSpecsSummary(String printSpecsSummary) {
        this.printSpecsSummary = printSpecsSummary;
    }
}