package com.urbanprint.backend.controller;

import com.urbanprint.backend.model.Equipment;
import com.urbanprint.backend.model.EquipmentType;
import com.urbanprint.backend.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@CrossOrigin(origins = "*")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public List<Equipment> findAll() {
        return equipmentService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Equipment> findById(
            @PathVariable Long id
    ) {
        return equipmentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Equipment>> findByType(
            @PathVariable EquipmentType type
    ) {
        return ResponseEntity.ok(
                equipmentService.findByType(type)
        );
    }

    @GetMapping("/active/{active}")
    public ResponseEntity<List<Equipment>> findByActive(
            @PathVariable boolean active
    ) {
        return ResponseEntity.ok(
                equipmentService.findByActive(active)
        );
    }

    @PostMapping
    public ResponseEntity<Equipment> create(
            @Valid @RequestBody Equipment equipment
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(equipmentService.create(equipment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Equipment> update(
            @PathVariable Long id,
            @Valid @RequestBody Equipment equipment
    ) {
        return equipmentService.update(id, equipment)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Equipment> updateActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return equipmentService.updateActiveStatus(id, active)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        if (!equipmentService.delete(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}