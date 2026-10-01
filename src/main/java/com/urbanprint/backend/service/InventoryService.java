package com.urbanprint.backend.service;

import com.urbanprint.backend.model.InventoryCategory;
import com.urbanprint.backend.model.InventoryItem;
import com.urbanprint.backend.model.InventoryMovement;
import com.urbanprint.backend.model.MovementType;
import com.urbanprint.backend.model.UnitOfMeasure;
import com.urbanprint.backend.repository.InventoryItemRepository;
import com.urbanprint.backend.repository.InventoryMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    public InventoryService(
            InventoryItemRepository inventoryItemRepository,
            InventoryMovementRepository inventoryMovementRepository
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> findAll() {
        return inventoryItemRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<InventoryItem> findById(Long id) {
        return inventoryItemRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> findByCategory(InventoryCategory category) {
        return inventoryItemRepository.findByCategoryOrderByNameAsc(category);
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> findByActive(boolean active) {
        return inventoryItemRepository.findByActiveOrderByNameAsc(active);
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> findLowStock() {
        return inventoryItemRepository.findAllByOrderByNameAsc()
                .stream()
                .filter(item -> item.getMinimumStock() != null
                        && item.getMinimumStock()
                        .compareTo(BigDecimal.ZERO) > 0
                        && item.getQuantity() != null
                        && item.getQuantity()
                        .compareTo(item.getMinimumStock()) <= 0)
                .toList();
    }

    @Transactional
    public InventoryItem create(InventoryItem item) {
        validateNameIsAvailable(item.getName(), null);

        if (item.getCategory() == null) {
            item.setCategory(InventoryCategory.OTHER);
        }

        if (item.getUnit() == null) {
            item.setUnit(UnitOfMeasure.SHEET);
        }

        if (item.getQuantity() == null) {
            item.setQuantity(BigDecimal.ZERO);
        }

        InventoryItem savedItem = inventoryItemRepository.save(item);

        if (savedItem.getQuantity()
                .compareTo(BigDecimal.ZERO) > 0) {
            registerMovement(
                    savedItem.getId(),
                    MovementType.IN,
                    savedItem.getQuantity(),
                    "Initial stock",
                    null
            );
        }

        return savedItem;
    }

    @Transactional
    public Optional<InventoryItem> update(Long id, InventoryItem updatedItem) {
        return inventoryItemRepository.findById(id)
                .map(existingItem -> {
                    validateNameIsAvailable(
                            updatedItem.getName(),
                            existingItem.getId()
                    );

                    existingItem.setName(updatedItem.getName());
                    existingItem.setCode(updatedItem.getCode());
                    existingItem.setCategory(
                            updatedItem.getCategory() != null
                                    ? updatedItem.getCategory()
                                    : InventoryCategory.OTHER
                    );
                    existingItem.setBrand(updatedItem.getBrand());
                    existingItem.setSupplier(updatedItem.getSupplier());
                    existingItem.setDescription(updatedItem.getDescription());
                    existingItem.setUnit(
                            updatedItem.getUnit() != null
                                    ? updatedItem.getUnit()
                                    : UnitOfMeasure.SHEET
                    );
                    existingItem.setMinimumStock(
                            updatedItem.getMinimumStock() != null
                                    ? updatedItem.getMinimumStock()
                                    : BigDecimal.ZERO
                    );
                    existingItem.setUnitCost(updatedItem.getUnitCost());
                    existingItem.setLocation(updatedItem.getLocation());
                    existingItem.setActive(updatedItem.isActive());
                    existingItem.setNotes(updatedItem.getNotes());

                    return inventoryItemRepository.save(existingItem);
                });
    }

    @Transactional
    public Optional<InventoryItem> updateActiveStatus(
            Long id,
            boolean active
    ) {
        return inventoryItemRepository.findById(id)
                .map(item -> {
                    item.setActive(active);
                    return inventoryItemRepository.save(item);
                });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!inventoryItemRepository.existsById(id)) {
            return false;
        }

        if (inventoryMovementRepository.existsByItemId(id)) {
            throw new IllegalArgumentException(
                    "Cannot delete an inventory item with recorded movements. Deactivate it instead."
            );
        }

        inventoryItemRepository.deleteById(id);
        return true;
    }

    @Transactional
    public InventoryMovement registerMovement(
            Long itemId,
            MovementType type,
            BigDecimal quantity,
            String reason,
            String notes
    ) {
        if (type == null) {
            throw new IllegalArgumentException("Movement type is required");
        }

        if (quantity == null
                || quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(
                    "Quantity must be different from zero"
            );
        }

        InventoryItem item = inventoryItemRepository
                .findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory item not found with id " + itemId
                ));

        BigDecimal currentQuantity =
                item.getQuantity() != null
                        ? item.getQuantity()
                        : BigDecimal.ZERO;

        BigDecimal signedQuantity = switch (type) {
            case IN, RETURN -> quantity.abs();
            case OUT, LOSS -> quantity.abs().negate();
            case ADJUSTMENT -> quantity;
        };

        BigDecimal newQuantity = currentQuantity.add(signedQuantity);

        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Current quantity: "
                            + currentQuantity
            );
        }

        item.setQuantity(newQuantity);
        inventoryItemRepository.save(item);

        InventoryMovement movement = new InventoryMovement();
        movement.setItem(item);
        movement.setType(type);
        movement.setQuantity(signedQuantity);
        movement.setQuantityAfter(newQuantity);
        movement.setReason(reason);
        movement.setNotes(notes);

        return inventoryMovementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public List<InventoryMovement> findMovements(Long itemId) {
        return inventoryMovementRepository
                .findByItemIdOrderByCreatedAtDesc(itemId);
    }

    private void validateNameIsAvailable(
            String name,
            Long currentItemId
    ) {
        inventoryItemRepository.findByNameIgnoreCase(name)
                .ifPresent(existingItem -> {
                    boolean belongsToCurrentRecord =
                            currentItemId != null
                                    && existingItem.getId()
                                    .equals(currentItemId);

                    if (!belongsToCurrentRecord) {
                        throw new IllegalArgumentException(
                                "An inventory item with this name already exists"
                        );
                    }
                });
    }
}