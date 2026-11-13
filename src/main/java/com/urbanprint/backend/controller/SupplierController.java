package com.urbanprint.backend.controller;

import com.urbanprint.backend.model.Supplier;
import com.urbanprint.backend.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<Supplier> findAll() {
        return supplierService.findAll();
    }

    @GetMapping("/active")
    public List<Supplier> findActive() {
        return supplierService.findActive();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Supplier> findById(@PathVariable Long id) {
        return supplierService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Supplier supplier) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(supplierService.create(supplier));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Supplier data) {
        try {
            return ResponseEntity.ok(supplierService.update(id, data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/active")
    public ResponseEntity<?> setActive(@PathVariable Long id, @RequestParam boolean active) {
        try {
            return ResponseEntity.ok(supplierService.setActive(id, active));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}