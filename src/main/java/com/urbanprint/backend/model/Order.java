package com.urbanprint.backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderPriority priority = OrderPriority.NORMAL;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "gst_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal gstRate = new BigDecimal("5.00");

    @Column(name = "gst_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "pst_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal pstRate = new BigDecimal("7.00");

    @Column(name = "pst_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal pstAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Turnaround herdado da quote de origem: "3-5", "5-7", "7-10" ou "RUSH"
    @Column(name = "turnaround_option", length = 20)
    private String turnaroundOption;

    // Rush fee: FIXED = valor fixo em $; PERCENT = % do subtotal (antes dos impostos)
    @Column(name = "rush_fee_type", length = 10)
    private String rushFeeType = "FIXED";

    // Percentual usado quando rushFeeType = PERCENT
    @Column(name = "rush_fee_percent", precision = 5, scale = 2)
    private BigDecimal rushFeePercent;

    // Rush fee em $ (fixo digitado ou calculado a partir do percentual);
    // entra na base taxavel (GST/PST)
    @Column(name = "rush_fee", precision = 10, scale = 2)
    private BigDecimal rushFee = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Fotos da instalação: exigidas antes de mover o pedido/itens de INSTALLING para COMPLETED.
    // URLs separadas por quebra de linha; podem ser servidas pelo próprio backend
    // (/uploads/installation-photos/...) ou links externos.
    @Column(name = "installation_photos", columnDefinition = "TEXT")
    private String installationPhotos;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.orderDate == null) {
            this.orderDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Order() {}

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public OrderPriority getPriority() { return priority; }
    public void setPriority(OrderPriority priority) { this.priority = priority; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getPstRate() { return pstRate; }
    public void setPstRate(BigDecimal pstRate) { this.pstRate = pstRate; }

    public BigDecimal getPstAmount() { return pstAmount; }
    public void setPstAmount(BigDecimal pstAmount) { this.pstAmount = pstAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getTurnaroundOption() { return turnaroundOption; }
    public void setTurnaroundOption(String turnaroundOption) { this.turnaroundOption = turnaroundOption; }

    public String getRushFeeType() { return rushFeeType; }
    public void setRushFeeType(String rushFeeType) { this.rushFeeType = rushFeeType; }

    public BigDecimal getRushFeePercent() { return rushFeePercent; }
    public void setRushFeePercent(BigDecimal rushFeePercent) { this.rushFeePercent = rushFeePercent; }

    public BigDecimal getRushFee() { return rushFee; }
    public void setRushFee(BigDecimal rushFee) { this.rushFee = rushFee; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getInstallationPhotos() { return installationPhotos; }
    public void setInstallationPhotos(String installationPhotos) { this.installationPhotos = installationPhotos; }

    // Lista das fotos (ignora linhas vazias)
    public List<String> getInstallationPhotoList() {
        List<String> result = new ArrayList<>();
        if (installationPhotos != null && !installationPhotos.isBlank()) {
            for (String url : installationPhotos.split("\\R")) {
                if (url != null && !url.isBlank()) {
                    result.add(url.trim());
                }
            }
        }
        return result;
    }

    public void setInstallationPhotoList(List<String> urls) {
        this.installationPhotos = (urls == null || urls.isEmpty()) ? null : String.join("\n", urls);
    }

    public boolean hasInstallationPhotos() {
        return !getInstallationPhotoList().isEmpty();
    }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}