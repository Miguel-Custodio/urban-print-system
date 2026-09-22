package com.urbanprint.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "item_description", nullable = false)
    private String itemDescription;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 12, scale = 4, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "print_specs_summary", columnDefinition = "TEXT")
    private String printSpecsSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "production_status", nullable = false, length = 30)
    private OrderStatus productionStatus = OrderStatus.PENDING;

    @Column(name = "production_notes", columnDefinition = "TEXT")
    private String productionNotes;

    public OrderItem() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getPrintSpecsSummary() { return printSpecsSummary; }
    public void setPrintSpecsSummary(String printSpecsSummary) { this.printSpecsSummary = printSpecsSummary; }

    public OrderStatus getProductionStatus() { return productionStatus; }
    public void setProductionStatus(OrderStatus productionStatus) { this.productionStatus = productionStatus; }

    public String getProductionNotes() { return productionNotes; }
    public void setProductionNotes(String productionNotes) { this.productionNotes = productionNotes; }
}