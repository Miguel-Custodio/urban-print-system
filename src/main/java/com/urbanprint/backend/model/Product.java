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
    // "QUANTITY_SCALE" (Business cards, flyers), "PER_SQFT" (Banners, vinyl),
    // "PER_UNIT" (T-shirts, signs), "HOURLY" (services)
    private String pricingMethod = "QUANTITY_SCALE";

    private String unit = "Each";

    // Base pricing for Per SqFt / Per Unit products
    @Column(precision = 10, scale = 2)
    private BigDecimal baseCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal minimumOrderFee;

    // 3. Print Specs (defaults for the product; each size can override)
    private String defaultStock;
    private String sidesInks; // e.g. 4/0, 4/4, 1/0, 1/1

    // 4. Production Workflow
    private String productionDepartment; // Digital Press, Wide Format, Bindery...
    private Integer turnaroundDays;      // Business days

    private boolean active = true;

    // 5. Sizes / Variations (each size has its own price scales)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ProductSize> sizes = new ArrayList<>();

    public Product() {
    }

    public void addSize(ProductSize size) {
        sizes.add(size);
        size.setProduct(this);
    }

    public void removeSize(ProductSize size) {
        sizes.remove(size);
        size.setProduct(null);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<ProductSize> getSizes() {
        return sizes;
    }

    public void setSizes(List<ProductSize> sizes) {
        this.sizes = sizes;
        if (sizes != null) {
            for (ProductSize size : sizes) {
                size.setProduct(this);
            }
        }
    }
}