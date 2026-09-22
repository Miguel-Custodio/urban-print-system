package com.urbanprint.backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String quoteNumber; // e.g. QU-1001

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private LocalDate issueDate = LocalDate.now();

    private LocalDate expiryDate = LocalDate.now().plusDays(30);

    // DRAFT, SENT, ACCEPTED, REJECTED, CONVERTED
    private String status = "DRAFT";

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    // British Columbia combined GST (5%) + PST (7%) = 12% by default
    @Column(precision = 5, scale = 2)
    private BigDecimal taxRate = new BigDecimal("12.00");

    @Column(precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<QuoteItem> items = new ArrayList<>();

    public Quote() {
    }

    public void addItem(QuoteItem item) {
        items.add(item);
        item.setQuote(this);
        calculateTotals();
    }

    public void removeItem(QuoteItem item) {
        items.remove(item);
        item.setQuote(null);
        calculateTotals();
    }

    @PrePersist
    @PreUpdate
    public void calculateTotals() {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (QuoteItem item : items) {
                if (item.getTotalPrice() != null) {
                    sum = sum.add(item.getTotalPrice());
                }
            }
        }
        this.subtotal = sum;

        if (this.taxRate != null && this.taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = this.subtotal.multiply(this.taxRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            this.taxAmount = BigDecimal.ZERO;
        }

        this.totalAmount = this.subtotal.add(this.taxAmount);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getQuoteNumber() {
        return quoteNumber;
    }

    public void setQuoteNumber(String quoteNumber) {
        this.quoteNumber = quoteNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<QuoteItem> getItems() {
        return items;
    }

    public void setItems(List<QuoteItem> items) {
        this.items = items;
        if (items != null) {
            for (QuoteItem item : items) {
                item.setQuote(this);
            }
        }
        calculateTotals();
    }
}