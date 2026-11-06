package com.urbanprint.backend.repository;

import com.urbanprint.backend.model.MarkupTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarkupTierRepository extends JpaRepository<MarkupTier, Long> {
    List<MarkupTier> findAllByOrderByMinCostAsc();
}