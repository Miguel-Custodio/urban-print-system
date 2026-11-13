package com.urbanprint.backend.service;

import com.urbanprint.backend.model.*;
import com.urbanprint.backend.repository.InventoryItemRepository;
import com.urbanprint.backend.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.urbanprint.backend.repository.SupplierRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;
    private final SupplierRepository supplierRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            InventoryItemRepository inventoryItemRepository,
            InventoryService inventoryService,
            SupplierRepository supplierRepository
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryService = inventoryService;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> findAll() {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<PurchaseOrder> findById(Long id) {
        return purchaseOrderRepository.findById(id);
    }

    @Transactional
    public PurchaseOrder create(PurchaseOrder order) {
        resolveSupplier(order);
        if (order.getLines() == null || order.getLines().isEmpty()) {
            throw new IllegalArgumentException("Add at least one material to the purchase order.");
        }

        order.setId(null);
        order.setPoNumber(generatePoNumber());
        order.setStatus(PurchaseOrderStatus.PENDING);

        for (PurchaseOrderLine line : order.getLines()) {
            line.setPurchaseOrder(order);
            line.setId(null);
            line.setReceivedQuantity(BigDecimal.ZERO);

            Long itemId = line.getInventoryItem() == null
                    ? null : line.getInventoryItem().getId();

            if (itemId == null) {
                throw new IllegalArgumentException("Every purchase order line must reference an inventory material.");
            }

            InventoryItem item = inventoryItemRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Inventory material not found: " + itemId
                    ));

            line.setInventoryItem(item);
            validateLine(line);
        }

        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder receive(Long orderId, List<ReceiptLineRequest> receipts) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found."));

        if (order.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("A cancelled purchase order cannot be received.");
        }
        if (receipts == null || receipts.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one received quantity.");
        }

        for (ReceiptLineRequest receipt : receipts) {
            if (receipt.getLineId() == null) {
                throw new IllegalArgumentException("Receipt line ID is required.");
            }
            if (receipt.getQuantity() == null
                    || receipt.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Received quantity must be greater than zero.");
            }

            PurchaseOrderLine line = order.getLines().stream()
                    .filter(candidate -> candidate.getId().equals(receipt.getLineId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Purchase order line not found: " + receipt.getLineId()
                    ));

            BigDecimal newReceived = line.getReceivedQuantity().add(receipt.getQuantity());
            if (newReceived.compareTo(line.getOrderedQuantity()) > 0) {
                throw new IllegalArgumentException(
                        "Received quantity cannot exceed the remaining quantity for "
                                + line.getInventoryItem().getName()
                );
            }

            line.setReceivedQuantity(newReceived);

            String reason = "Purchase order " + order.getPoNumber();
            String notes = "Supplier: " + order.getSupplier()
                    + (receipt.getNotes() == null || receipt.getNotes().isBlank()
                    ? "" : " | " + receipt.getNotes());

            inventoryService.registerMovement(
                    line.getInventoryItem().getId(),
                    MovementType.IN,
                    receipt.getQuantity(),
                    reason,
                    notes
            );

            line.getInventoryItem().setUnitCost(line.getUnitCost());
            inventoryItemRepository.save(line.getInventoryItem());
        }

        boolean allReceived = order.getLines().stream()
                .allMatch(line -> line.getReceivedQuantity()
                        .compareTo(line.getOrderedQuantity()) >= 0);

        boolean anyReceived = order.getLines().stream()
                .anyMatch(line -> line.getReceivedQuantity()
                        .compareTo(BigDecimal.ZERO) > 0);

        if (allReceived) {
            order.setStatus(PurchaseOrderStatus.RECEIVED);
        } else if (anyReceived) {
            order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        } else {
            order.setStatus(PurchaseOrderStatus.PENDING);
        }

        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder cancel(Long orderId) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found."));

        if (order.getLines().stream().anyMatch(line ->
                line.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0)) {
            throw new IllegalArgumentException(
                    "A purchase order with received materials cannot be cancelled."
            );
        }

        order.setStatus(PurchaseOrderStatus.CANCELLED);
        return purchaseOrderRepository.save(order);
    }

    private void resolveSupplier(PurchaseOrder order) {
        Long supplierId = order.getSupplierRef() == null
                ? null : order.getSupplierRef().getId();

        if (supplierId != null) {
            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new IllegalArgumentException(
                    "Supplier not found: " + supplierId
                    ));

            order.setSupplierRef(supplier);

            if (order.getSupplier() == null || order.getSupplier().isBlank()) {
                order.setSupplier(supplier.getName());
            }
        } else if (order.getSupplier() == null || order.getSupplier().isBlank()) {
            throw new IllegalArgumentException("Supplier is required.");
        }
    }

    private void validateLine(PurchaseOrderLine line) {
        if (line.getOrderedQuantity() == null
                || line.getOrderedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ordered quantity must be greater than zero.");
        }
        if (line.getUnitCost() == null
                || line.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit cost cannot be negative.");
        }
    }

    private String generatePoNumber() {
        long next = purchaseOrderRepository.count() + 1;
        String candidate;

        do {
            candidate = String.format("PO-%04d", next++);
        } while (purchaseOrderRepository.findByPoNumber(candidate).isPresent());

        return candidate;
    }

    public static class ReceiptLineRequest {
        private Long lineId;
        private BigDecimal quantity;
        private String notes;

        public Long getLineId() {
            return lineId;
        }

        public void setLineId(Long lineId) {
            this.lineId = lineId;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}