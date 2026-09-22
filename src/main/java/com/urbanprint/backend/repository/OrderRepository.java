package com.urbanprint.backend.repository;

import com.urbanprint.backend.model.Order;
import com.urbanprint.backend.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    List<Order> findByStatusOrderByDueDateAsc(OrderStatus status);

    List<Order> findAllByOrderByCreatedAtDesc();
}