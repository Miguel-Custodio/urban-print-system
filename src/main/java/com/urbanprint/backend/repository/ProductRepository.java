package com.urbanprint.backend.repository;

import com.urbanprint.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findFirstByProductNameIgnoreCase(String productName);

    // Removes legacy rows that reference the product directly through the
    // old product_id column (structure before ProductSize existed).
    @Modifying
    @Query(
        value = "DELETE FROM product_price_scales WHERE product_id = :productId",
        nativeQuery = true
    )
    int deleteLegacyPriceScalesByProductId(@Param("productId") Long productId);
}