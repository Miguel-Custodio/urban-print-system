package com.urbanprint.backend.controller;

import com.urbanprint.backend.model.Order;
import com.urbanprint.backend.model.OrderItem;
import com.urbanprint.backend.model.OrderStatus;
import com.urbanprint.backend.model.Quote;
import com.urbanprint.backend.repository.QuoteRepository;
import com.urbanprint.backend.repository.OrderRepository;
import com.urbanprint.backend.service.EmailService;
import com.urbanprint.backend.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final QuoteRepository quoteRepository;
    private final OrderRepository orderRepository;

    // Folder where installation photos are saved (same value as app.upload-dir in application.properties)
    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public OrderController(OrderService orderService, QuoteRepository quoteRepository, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.quoteRepository = quoteRepository;
        this.orderRepository = orderRepository;
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
        try {
            Quote quote = quoteRepository.findById(quoteId)
                    .orElseThrow(() -> new RuntimeException("Quote not found with id " + quoteId));

            Order order = orderService.createOrderFromQuote(quote);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", message));
        }
    }

    @PutMapping("/{id}")
    public Order updateOrder(@PathVariable Long id, @RequestBody Order order) {
        return orderService.updateOrder(id, order);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        Order order = orderService.getOrderById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Rule: INSTALLING -> COMPLETED requires installation photos attached to the order
        if (status == OrderStatus.COMPLETED
                && order.getStatus() == OrderStatus.INSTALLING
                && !order.hasInstallationPhotos()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Installation photos are required before moving this order to Completed."));
        }

        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
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
            Order order = orderService.getOrderById(id).orElse(null);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found with id " + id));
            }

            OrderItem currentItem = order.getItems().stream()
                    .filter(i -> itemId.equals(i.getId()))
                    .findFirst()
                    .orElse(null);

            // Rule: item INSTALLING -> COMPLETED requires installation photos on the order
            if (currentItem != null
                    && itemData.getProductionStatus() == OrderStatus.COMPLETED
                    && currentItem.getProductionStatus() == OrderStatus.INSTALLING
                    && !order.hasInstallationPhotos()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Installation photos are required before moving this item to Completed."));
            }

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

    // ---- Installation photos (required before INSTALLING -> COMPLETED) ----

    @PostMapping("/{id}/installation-photos")
    public ResponseEntity<?> addInstallationPhotos(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found with id " + id));
        }

        List<String> newUrls = parsePhotoUrls(payload);
        if (newUrls.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No photo URL was provided."));
        }

        List<String> current = new ArrayList<>(order.getInstallationPhotoList());
        current.addAll(newUrls);
        order.setInstallationPhotoList(current);
        orderRepository.save(order);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/installation-photos/upload")
    public ResponseEntity<?> uploadInstallationPhotos(@PathVariable Long id,
                                                      @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found with id " + id));
        }
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Select at least one photo to upload."));
        }

        try {
            Path folder = Paths.get(uploadDir).toAbsolutePath().resolve("installation-photos");
            Files.createDirectories(folder);

            List<String> current = new ArrayList<>(order.getInstallationPhotoList());
            int previousSize = current.size();
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

            int index = 0;
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo";
                String ext = "";
                int dot = original.lastIndexOf('.');
                if (dot >= 0) {
                    ext = original.substring(dot).toLowerCase().replaceAll("[^a-z0-9.]", "");
                }
                if (ext.isBlank() || ext.equals(".")) {
                    ext = ".jpg";
                }

                String fileName = "order-" + id + "-" + stamp + "-" + index + ext;
                file.transferTo(folder.resolve(fileName).toFile());
                current.add("/uploads/installation-photos/" + fileName);
                index++;
            }

            if (current.size() == previousSize) {
                return ResponseEntity.badRequest().body(Map.of("error", "No valid photo file was received."));
            }

            order.setInstallationPhotoList(current);
            orderRepository.save(order);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not save the uploaded photos: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/installation-photos/{index}")
    public ResponseEntity<?> removeInstallationPhoto(@PathVariable Long id, @PathVariable int index) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found with id " + id));
        }

        List<String> current = new ArrayList<>(order.getInstallationPhotoList());
        if (index < 0 || index >= current.size()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid photo index."));
        }

        current.remove(index);
        order.setInstallationPhotoList(current);
        orderRepository.save(order);
        return ResponseEntity.ok(order);
    }

    @SuppressWarnings("unchecked")
    private List<String> parsePhotoUrls(Map<String, Object> payload) {
        List<String> urls = new ArrayList<>();
        if (payload == null) {
            return urls;
        }
        Object photos = payload.get("photos");
        Object photoUrl = payload.get("photoUrl");
        if (photos instanceof List) {
            for (Object o : (List<Object>) photos) {
                if (o != null && !o.toString().isBlank()) {
                    urls.add(o.toString().trim());
                }
            }
        } else if (photos instanceof String && !((String) photos).isBlank()) {
            for (String s : ((String) photos).split("[,\\n]")) {
                if (!s.isBlank()) {
                    urls.add(s.trim());
                }
            }
        } else if (photoUrl instanceof String && !((String) photoUrl).isBlank()) {
            urls.add(((String) photoUrl).trim());
        }
        return urls;
    }

    // ---- End of installation photos ----

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