package com.urbanprint.backend.repository;

import com.urbanprint.backend.model.InventoryCategory;
import com.urbanprint.backend.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<InventoryItem> findByCategoryOrderByNameAsc(InventoryCategory category);

    List<InventoryItem> findByActiveOrderByNameAsc(boolean active);

    List<InventoryItem> findAllByOrderByNameAsc();
}