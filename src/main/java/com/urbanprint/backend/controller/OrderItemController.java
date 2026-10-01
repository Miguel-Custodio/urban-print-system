package com.urbanprint.backend.controller;

import com.urbanprint.backend.model.OrderItem;
import com.urbanprint.backend.model.OrderStatus;
import com.urbanprint.backend.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/order-items")
@CrossOrigin(origins = "*")
public class OrderItemController {

    private final OrderService orderService;

    public OrderItemController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PatchMapping("/{id}/status")
    public OrderItem updateItemStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return orderService.updateItemProductionStatus(id, status);
    }

    @PatchMapping("/{id}/notes")
    public OrderItem updateItemNotes(@PathVariable Long id, @RequestBody(required = false) Map<String, String> payload) {
        String notes = payload != null ? payload.get("productionNotes") : "";
        return orderService.updateItemProductionNotes(id, notes);
    }
}