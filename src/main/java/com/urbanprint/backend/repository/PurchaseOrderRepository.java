package com.urbanprint.backend.repository;

import com.urbanprint.backend.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    Optional<PurchaseOrder> findByPoNumber(String poNumber);
}