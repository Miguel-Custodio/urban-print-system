package com.urbanprint.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_options")
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    // FLAT, PER_UNIT, PER_LINEAR_FT, PER_SQFT
    public String costType = "FLAT";

    @Column(precision = 10, scale = 4)
    public BigDecimal cost;

    public Integer spacingInches;
    public Integer minQty;
    public boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonBackReference("productOptions")
    public Product product;

    public ProductOption() {}
}
