package com.urbanprint.backend.repository;

import com.urbanprint.backend.model.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {

    List<PurchaseOrderLine> findByPurchaseOrderIdOrderByIdAsc(Long purchaseOrderId);
}