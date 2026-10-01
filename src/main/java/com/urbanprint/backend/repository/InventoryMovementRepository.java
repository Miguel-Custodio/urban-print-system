package com.urbanprint.backend.repository;

import com.urbanprint.backend.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByItemIdOrderByCreatedAtDesc(Long itemId);

    boolean existsByItemId(Long itemId);
}