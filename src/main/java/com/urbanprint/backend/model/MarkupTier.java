package com.urbanprint.backend.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "markup_tiers")
public class MarkupTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(precision = 12, scale = 2)
    public BigDecimal minCost;

    @Column(precision = 12, scale = 2)
    public BigDecimal maxCost;      // null = no upper limit

    @Column(precision = 8, scale = 4)
    public BigDecimal multiplier;

    public MarkupTier() {}
}