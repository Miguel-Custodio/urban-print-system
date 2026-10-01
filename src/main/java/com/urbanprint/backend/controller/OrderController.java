package com.urbanprint.backend.controller;

import com.urbanprint.backend.model.Order;
import com.urbanprint.backend.model.OrderItem;
import com.urbanprint.backend.model.OrderStatus;
import com.urbanprint.backend.model.Quote;
import com.urbanprint.backend.repository.QuoteRepository;
import com.urbanprint.backend.service.EmailService;
import com.urbanprint.backend.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final QuoteRepository quoteRepository;

    public OrderController(OrderService orderService, QuoteRepository quoteRepository) {
        this.orderService = orderService;
        this.quoteRepository = quoteRepository;
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public List<Order> getOrdersByStatus(@PathVariable OrderStatus status) {
        return orderService.getOrdersByStatus(status);
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }

    @PostMapping("/from-quote/{quoteId}")
    public ResponseEntity<?> createOrderFromQuote(@PathVariable Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found with id " + quoteId));

        try {
            Order order = orderService.createOrderFromQuote(quote);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public Order updateOrder(@PathVariable Long id, @RequestBody Order order) {
        return orderService.updateOrder(id, order);
    }

    @PatchMapping("/{id}/status")
    public Order updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return orderService.updateOrderStatus(id, status);
    }

    @PatchMapping("/{id}/notes")
    public Order updateOrderNotes(@PathVariable Long id, @RequestBody(required = false) Map<String, String> payload) {
        String notes = payload != null ? payload.get("notes") : "";
        return orderService.updateOrderNotes(id, notes);
    }

    // ---- Item management (DocketManager-style flow: order first, items after) ----

    @PostMapping("/{id}/items")
    public ResponseEntity<?> addOrderItem(@PathVariable Long id, @RequestBody OrderItem newItem) {
        try {
            Order updatedOrder = orderService.addItemToOrder(id, newItem);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<?> updateOrderItem(@PathVariable Long id, @PathVariable Long itemId,
                                             @RequestBody OrderItem itemData) {
        try {
            Order updatedOrder = orderService.updateOrderItem(id, itemId, itemData);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<?> removeOrderItem(@PathVariable Long id, @PathVariable Long itemId) {
        try {
            Order updatedOrder = orderService.removeOrderItem(id, itemId);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ---- End of item management ----

    @PostMapping("/{id}/to-quote")
    public ResponseEntity<?> convertToQuote(@PathVariable Long id) {
        try {
            Quote quote = orderService.convertOrderToQuote(id);
            return ResponseEntity.ok(quote);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reorder")
    public ResponseEntity<?> reorderOrder(@PathVariable Long id) {
        try {
            Order newOrder = orderService.reorderOrder(id);
            return ResponseEntity.ok(newOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @Autowired
    private EmailService emailService;

    @PostMapping("/{id}/send-invoice")
    public ResponseEntity<?> sendInvoice(@PathVariable Long id) {
        try {
            Order order = orderService.getOrderById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found with id " + id));
            emailService.sendInvoiceEmail(order);
            return ResponseEntity.ok(Map.of("message", "Invoice sent successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}