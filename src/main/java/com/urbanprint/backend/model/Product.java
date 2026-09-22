package com.urbanprint.backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. General Info
    @Column(nullable = false)
    private String productName;

    private String itemCode;

    private String category;

    private String description;

    // 2. Pricing Method (DocketManager Style)
    // "QUANTITY_SCALE" (Business cards, flyers), "PER_SQFT" (Banners, vinyl), "PER_UNIT" (T-shirts, signs), "HOURLY"
    private String pricingMethod = "QUANTITY_SCALE";

    private String unit = "Each"; // Unit of measurement

    // Fallback/Base pricing for Per SqFt or Per Unit products
    @Column(precision = 10, scale = 2)
    private BigDecimal baseCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal minimumOrderFee;

    // 3. Print Specs
    @Column(precision = 8, scale = 2)
    private BigDecimal defaultWidth;

    @Column(precision = 8, scale = 2)
    private BigDecimal defaultHeight;

    private String defaultStock; // e.g. 14pt Coated Cover, 13oz Vinyl

    private String sidesInks; // e.g. 4/0, 4/4, 1/0, 1/1

    // 4. Production Workflow
    private String productionDepartment; // Digital Press, Wide Format, Bindery

    private Integer turnaroundDays; // Business days

    private boolean active = true;

    // Relationship to Pricing Scales (Tiers)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ProductPriceScale> priceScales = new ArrayList<>();

    public Product() {
    }

    public void addPriceScale(ProductPriceScale scale) {
        priceScales.add(scale);
        scale.setProduct(this);
    }

    public void removePriceScale(ProductPriceScale scale) {
        priceScales.remove(scale);
        scale.setProduct(null);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPricingMethod() {
        return pricingMethod;
    }

    public void setPricingMethod(String pricingMethod) {
        this.pricingMethod = pricingMethod;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getMinimumOrderFee() {
        return minimumOrderFee;
    }

    public void setMinimumOrderFee(BigDecimal minimumOrderFee) {
        this.minimumOrderFee = minimumOrderFee;
    }

    public BigDecimal getDefaultWidth() {
        return defaultWidth;
    }

    public void setDefaultWidth(BigDecimal defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    public BigDecimal getDefaultHeight() {
        return defaultHeight;
    }

    public void setDefaultHeight(BigDecimal defaultHeight) {
        this.defaultHeight = defaultHeight;
    }

    public String getDefaultStock() {
        return defaultStock;
    }

    public void setDefaultStock(String defaultStock) {
        this.defaultStock = defaultStock;
    }

    public String getSidesInks() {
        return sidesInks;
    }

    public void setSidesInks(String sidesInks) {
        this.sidesInks = sidesInks;
    }

    public String getProductionDepartment() {
        return productionDepartment;
    }

    public void setProductionDepartment(String productionDepartment) {
        this.productionDepartment = productionDepartment;
    }

    public Integer getTurnaroundDays() {
        return turnaroundDays;
    }

    public void setTurnaroundDays(Integer turnaroundDays) {
        this.turnaroundDays = turnaroundDays;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<ProductPriceScale> getPriceScales() {
        return priceScales;
    }

    public void setPriceScales(List<ProductPriceScale> priceScales) {
        this.priceScales = priceScales;
        if (priceScales != null) {
            for (ProductPriceScale scale : priceScales) {
                scale.setProduct(this);
            }
        }
    }
}