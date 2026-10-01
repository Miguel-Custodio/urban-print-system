package com.urbanprint.backend.repository;

import com.urbanprint.backend.model.Equipment;
import com.urbanprint.backend.model.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Equipment> findByTypeOrderByNameAsc(EquipmentType type);

    List<Equipment> findByActiveOrderByNameAsc(boolean active);

    List<Equipment> findAllByOrderByNameAsc();
}