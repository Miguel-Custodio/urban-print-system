package com.urbanprint.backend.repository;

import com.urbanprint.backend.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Supplier> findByActiveTrueOrderByNameAsc();

    List<Supplier> findAllByOrderByNameAsc();
}