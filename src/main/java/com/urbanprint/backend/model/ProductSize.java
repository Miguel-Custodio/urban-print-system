package com.urbanprint.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_sizes")
public class ProductSize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "4x6", "8.5x11 Tri-Fold", "3.5x2 Soft Touch", "11x17 - 8 Pages"
    @Column(nullable = false)
    private String label;

    // Dimensions in inches
    @Column(precision = 8, scale = 2)
    private BigDecimal width;

    @Column(precision = 8, scale = 2)
    private BigDecimal height;

    // Optional overrides for this size
    private String stock;     // e.g. 14pt Coated Cover, 13oz Vinyl
    private String sidesInks; // e.g. 4/0, 4/4

    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonBackReference
    private Product product;

    @OneToMany(mappedBy = "size", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ProductPriceScale> priceScales = new ArrayList<>();

    public ProductSize() {
    }

    // Area in square feet, used by PER_SQFT pricing
    public BigDecimal getSquareFeet() {
        if (width == null || height == null) {
            return null;
        }
        return width.multiply(height)
                .divide(BigDecimal.valueOf(144), 4, RoundingMode.HALF_UP);
    }

    public void addPriceScale(ProductPriceScale scale) {
        priceScales.add(scale);
        scale.setSize(this);
    }

    public void removePriceScale(ProductPriceScale scale) {
        priceScales.remove(scale);
        scale.setSize(null);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public void setWidth(BigDecimal width) {
        this.width = width;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public String getSidesInks() {
        return sidesInks;
    }

    public void setSidesInks(String sidesInks) {
        this.sidesInks = sidesInks;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public List<ProductPriceScale> getPriceScales() {
        return priceScales;
    }

    public void setPriceScales(List<ProductPriceScale> priceScales) {
        this.priceScales = priceScales;
        if (priceScales != null) {
            for (ProductPriceScale scale : priceScales) {
                scale.setSize(this);
            }
        }
    }
}