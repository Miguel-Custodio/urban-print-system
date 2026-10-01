package com.urbanprint.backend.controller;

import com.urbanprint.backend.model.InventoryCategory;
import com.urbanprint.backend.model.InventoryItem;
import com.urbanprint.backend.model.InventoryMovement;
import com.urbanprint.backend.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryItem> findAll() {
        return inventoryService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> findById(
            @PathVariable Long id
    ) {
        return inventoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<InventoryItem>> findByCategory(
            @PathVariable InventoryCategory category
    ) {
        return ResponseEntity.ok(
                inventoryService.findByCategory(category)
        );
    }

    @GetMapping("/active/{active}")
    public ResponseEntity<List<InventoryItem>> findByActive(
            @PathVariable boolean active
    ) {
        return ResponseEntity.ok(
                inventoryService.findByActive(active)
        );
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryItem>> findLowStock() {
        return ResponseEntity.ok(
                inventoryService.findLowStock()
        );
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody InventoryItem item
    ) {
        try {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(inventoryService.create(item));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody InventoryItem item
    ) {
        try {
            return inventoryService.update(id, item)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<InventoryItem> updateActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return inventoryService.updateActiveStatus(id, active)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            if (!inventoryService.delete(id)) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/movements")
    public ResponseEntity<?> registerMovement(
            @PathVariable Long id,
            @RequestBody InventoryMovement movement
    ) {
        try {
            InventoryMovement saved = inventoryService.registerMovement(
                    id,
                    movement.getType(),
                    movement.getQuantity(),
                    movement.getReason(),
                    movement.getNotes()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<List<InventoryMovement>> findMovements(
            @PathVariable Long id
    ) {
        if (!inventoryService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                inventoryService.findMovements(id)
        );
    }
}