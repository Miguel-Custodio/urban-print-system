package com.urbanprint.backend.service;

import com.urbanprint.backend.model.Order;
import com.urbanprint.backend.model.OrderItem;
import com.urbanprint.backend.model.OrderStatus;
import com.urbanprint.backend.model.Quote;
import com.urbanprint.backend.model.QuoteItem;
import com.urbanprint.backend.model.Product;
import com.urbanprint.backend.model.ProductPriceScale;
import com.urbanprint.backend.model.ProductSize;
import com.urbanprint.backend.repository.OrderRepository;
import com.urbanprint.backend.repository.OrderItemRepository;
import com.urbanprint.backend.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final QuoteRepository quoteRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(OrderRepository orderRepository, QuoteRepository quoteRepository,
                         OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.quoteRepository = quoteRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByDueDateAsc(status);
    }

    @Transactional
    public Order createOrder(Order order) {
        if (order.getOrderNumber() == null || order.getOrderNumber().trim().isEmpty()) {
            order.setOrderNumber(generateOrderNumber());
        }

        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }

        if (order.getGstRate() == null) {
            order.setGstRate(new BigDecimal("5.00"));
        }

        if (order.getPstRate() == null) {
            order.setPstRate(new BigDecimal("7.00"));
        }

        if (order.getRushFee() == null) {
            order.setRushFee(BigDecimal.ZERO);
        }

        calculateTotals(order);

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(order);
            }
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrder(Long id, Order updatedOrder) {
        return orderRepository.findById(id).map(existing -> {
            existing.setCustomer(updatedOrder.getCustomer());
            existing.setOrderDate(updatedOrder.getOrderDate());
            existing.setDueDate(updatedOrder.getDueDate());
            existing.setStatus(updatedOrder.getStatus());
            existing.setPriority(updatedOrder.getPriority());
            existing.setNotes(updatedOrder.getNotes());
            existing.setGstRate(updatedOrder.getGstRate() != null ? updatedOrder.getGstRate() : new BigDecimal("5.00"));
            existing.setPstRate(updatedOrder.getPstRate() != null ? updatedOrder.getPstRate() : new BigDecimal("7.00"));

            // Turnaround/rush: atualiza apenas se o front enviou o campo.
            // Preserva o valor herdado da quote quando a tela de pedido
            // salva sem esses campos (evita apagar o rush em todo save).
            if (updatedOrder.getTurnaroundOption() != null) {
                existing.setTurnaroundOption(updatedOrder.getTurnaroundOption());
            }
            if (updatedOrder.getRushFee() != null) {
                existing.setRushFee(updatedOrder.getRushFee());
            }

            existing.getItems().clear();
            if (updatedOrder.getItems() != null) {
                for (OrderItem item : updatedOrder.getItems()) {
                    item.setOrder(existing);
                    existing.getItems().add(item);
                }
            }

            calculateTotals(existing);
            return orderRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Order not found with id " + id));
    }

    @Transactional
    public Order updateOrderStatus(Long id, OrderStatus status) {
        return orderRepository.findById(id).map(existing -> {
            existing.setStatus(status);
            return orderRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Order not found with id " + id));
    }

    @Transactional
    public Order updateOrderNotes(Long orderId, String notes) {
        return orderRepository.findById(orderId).map(order -> {
            order.setNotes(notes);
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Order not found with id " + orderId));
    }

    // ---- Item management (DocketManager-style flow) ----

    @Transactional
    public Order addItemToOrder(Long orderId, OrderItem newItem) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id " + orderId));

        newItem.setOrder(order);
        order.getItems().add(newItem);

        calculateTotals(order);

        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrderItem(Long orderId, Long itemId, OrderItem itemData) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id " + orderId));

        OrderItem existingItem = order.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order item not found with id " + itemId));

        if (itemData.getProduct() != null && itemData.getProduct().getId() != null) {
            existingItem.setProduct(itemData.getProduct());
        }

        existingItem.setItemDescription(itemData.getItemDescription());
        existingItem.setQuantity(itemData.getQuantity());
        existingItem.setUnitPrice(itemData.getUnitPrice());
        existingItem.setPrintSpecsSummary(itemData.getPrintSpecsSummary());

        calculateTotals(order);

        return orderRepository.save(order);
    }

    @Transactional
    public Order removeOrderItem(Long orderId, Long itemId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id " + orderId));

        OrderItem itemToRemove = order.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order item not found with id " + itemId));

        order.removeItem(itemToRemove);

        calculateTotals(order);

        return orderRepository.save(order);
    }

    // ---- End of item management ----

    @Transactional
    public Order reorderOrder(Long orderId) {
        Order original = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id " + orderId));

        Order newOrder = new Order();
        newOrder.setCustomer(original.getCustomer());
        newOrder.setOrderDate(LocalDate.now());
        newOrder.setDueDate(null);
        newOrder.setPriority(original.getPriority());
        newOrder.setStatus(OrderStatus.PENDING_PAYMENT);
        newOrder.setGstRate(original.getGstRate());
        newOrder.setPstRate(original.getPstRate());
        newOrder.setTurnaroundOption(original.getTurnaroundOption());
        newOrder.setRushFee(original.getRushFee() != null ? original.getRushFee() : BigDecimal.ZERO);
        newOrder.setNotes("Re-order of " + original.getOrderNumber());
        newOrder.setOrderNumber(generateOrderNumber());

        List<OrderItem> newItems = original.getItems().stream().map(oldItem -> {
            OrderItem newItem = new OrderItem();
            newItem.setProduct(oldItem.getProduct());
            newItem.setItemDescription(oldItem.getItemDescription());
            newItem.setQuantity(oldItem.getQuantity());
            newItem.setUnitPrice(oldItem.getUnitPrice());
            newItem.setTotalPrice(oldItem.getTotalPrice());
            newItem.setPrintSpecsSummary(oldItem.getPrintSpecsSummary());
            newItem.setProductionStatus(OrderStatus.PENDING_PAYMENT);
            newItem.setOrder(newOrder);
            return newItem;
        }).collect(java.util.stream.Collectors.toList());

        newOrder.setItems(newItems);

        calculateTotals(newOrder);

        return orderRepository.save(newOrder);
    }

    @Transactional
    public OrderItem updateItemProductionStatus(Long itemId, OrderStatus status) {
        OrderItem savedItem = orderItemRepository.findById(itemId).map(item -> {
            item.setProductionStatus(status);
            return orderItemRepository.save(item);
        }).orElseThrow(() -> new RuntimeException("Order item not found with id " + itemId));

        Order order = savedItem.getOrder();
        if (order != null && order.getItems() != null && !order.getItems().isEmpty()) {
            OrderStatus calculatedStatus = recalculateOrderStatus(order.getItems());
            if (calculatedStatus != null && calculatedStatus != order.getStatus()) {
                order.setStatus(calculatedStatus);
                orderRepository.save(order);
            }
        }

        return savedItem;
    }

    private OrderStatus recalculateOrderStatus(List<OrderItem> items) {
        if (items.isEmpty()) return null;

        boolean allCompleted = items.stream().allMatch(i -> i.getProductionStatus() == OrderStatus.COMPLETED);
        if (allCompleted) return OrderStatus.COMPLETED;

        boolean allReadyOrDone = items.stream().allMatch(i ->
                i.getProductionStatus() == OrderStatus.READY_FOR_PICKUP ||
                i.getProductionStatus() == OrderStatus.SHIPPING ||
                i.getProductionStatus() == OrderStatus.COMPLETED);
        if (allReadyOrDone) return OrderStatus.READY_FOR_PICKUP;

        boolean anyInFinishing = items.stream().anyMatch(i -> i.getProductionStatus() == OrderStatus.FINISHING);
        if (anyInFinishing) return OrderStatus.FINISHING;

        boolean anyPrinting = items.stream().anyMatch(i ->
                i.getProductionStatus() == OrderStatus.SF_PRINTING ||
                i.getProductionStatus() == OrderStatus.WF_PRINTING);
        if (anyPrinting) {
            boolean hasWf = items.stream().anyMatch(i -> i.getProductionStatus() == OrderStatus.WF_PRINTING);
            return hasWf ? OrderStatus.WF_PRINTING : OrderStatus.SF_PRINTING;
        }

        boolean anyQueue = items.stream().anyMatch(i ->
                i.getProductionStatus() == OrderStatus.SF_QUEUE ||
                i.getProductionStatus() == OrderStatus.WF_QUEUE);
        if (anyQueue) {
            boolean hasWf = items.stream().anyMatch(i -> i.getProductionStatus() == OrderStatus.WF_QUEUE);
            return hasWf ? OrderStatus.WF_QUEUE : OrderStatus.SF_QUEUE;
        }

        boolean anyGraphics = items.stream().anyMatch(i -> i.getProductionStatus() == OrderStatus.GRAPHICS);
        if (anyGraphics) return OrderStatus.GRAPHICS;

        return null;
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    @Transactional
    public OrderItem updateItemProductionNotes(Long itemId, String notes) {
        return orderItemRepository.findById(itemId).map(item -> {
            item.setProductionNotes(notes);
            return orderItemRepository.save(item);
        }).orElseThrow(() -> new RuntimeException("Order item not found with id " + itemId));
    }

    @Transactional
    public Quote convertOrderToQuote(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id " + orderId));

        Quote quote = new Quote();
        quote.setCustomer(order.getCustomer());
        quote.setIssueDate(LocalDate.now());
        quote.setExpiryDate(LocalDate.now().plusDays(30));

        // Sem DRAFT: a quote nasce aberta (status nulo)
        quote.setStatus(null);

        quote.setGstRate(order.getGstRate());
        quote.setPstRate(order.getPstRate());

        // Herda o turnaround e o rush fee do pedido de origem
        quote.setTurnaroundOption(order.getTurnaroundOption());
        quote.setRushFee(order.getRushFee() != null ? order.getRushFee() : BigDecimal.ZERO);

        quote.setNotes("Converted from order " + order.getOrderNumber() + " with updated pricing.");
        quote.setQuoteNumber(generateQuoteNumberForConversion());

        List<QuoteItem> quoteItems = order.getItems().stream().map(oldItem -> {
            QuoteItem newItem = new QuoteItem();
            newItem.setProduct(oldItem.getProduct());
            newItem.setItemDescription(oldItem.getItemDescription());
            newItem.setQuantity(oldItem.getQuantity());
            newItem.setPrintSpecsSummary(oldItem.getPrintSpecsSummary());

            BigDecimal unitPrice = findCurrentUnitPrice(oldItem.getProduct(), oldItem.getQuantity());
            BigDecimal totalPrice = unitPrice
                    .multiply(BigDecimal.valueOf(oldItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            newItem.setUnitPrice(unitPrice);
            newItem.setTotalPrice(totalPrice);
            newItem.setQuote(quote);
            return newItem;
        }).collect(Collectors.toList());

        quote.setItems(quoteItems);

        calculateQuoteTotals(quote);

        return quoteRepository.save(quote);
    }

    private BigDecimal findCurrentUnitPrice(Product product, Integer quantity) {
        if (product == null || product.getSizes() == null || product.getSizes().isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<ProductPriceScale> allScales = new ArrayList<>();
        for (ProductSize size : product.getSizes()) {
            if (size.isActive() && size.getPriceScales() != null) {
                allScales.addAll(size.getPriceScales());
            }
        }

        if (allScales.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Optional<ProductPriceScale> exact = allScales.stream()
                .filter(scale -> scale.getQuantity().equals(quantity))
                .findFirst();

        if (exact.isPresent()) {
            return exact.get().getPricePerPiece();
        }

        ProductPriceScale closest = allScales.stream()
                .min(Comparator.comparingInt(scale -> Math.abs(scale.getQuantity() - quantity)))
                .orElse(null);

        return closest != null ? closest.getPricePerPiece() : BigDecimal.ZERO;
    }

    private void calculateQuoteTotals(Quote quote) {
        BigDecimal subtotal = BigDecimal.ZERO;
        if (quote.getItems() != null) {
            for (QuoteItem item : quote.getItems()) {
                if (item.getTotalPrice() != null) {
                    subtotal = subtotal.add(item.getTotalPrice());
                }
            }
        }
        quote.setSubtotal(subtotal);

        // Rush fee entra na base taxavel, igual ao calculo da QuoteService
        BigDecimal rush = quote.getRushFee() != null ? quote.getRushFee() : BigDecimal.ZERO;
        quote.setRushFee(rush);

        BigDecimal taxable = subtotal.add(rush);

        BigDecimal gstAmount = taxable.multiply(quote.getGstRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        quote.setGstAmount(gstAmount);

        BigDecimal pstAmount = taxable.multiply(quote.getPstRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        quote.setPstAmount(pstAmount);

        quote.setTotalAmount(taxable.add(gstAmount).add(pstAmount));
    }

    private String generateQuoteNumberForConversion() {
        // Maior numero existente + 1 (nao usa count(), que duplicava
        // numeros depois de apagar quotes — mesmo bug do QU-1003)
        long maxSeq = 1000;

        for (Quote q : quoteRepository.findAll()) {
            String num = q.getQuoteNumber();

            if (num != null && num.startsWith("QU-")) {
                try {
                    long seq = Long.parseLong(num.substring(3));

                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException ignored) {
                    // ignora quote numbers fora do padrao QU-XXXX
                }
            }
        }

        return String.format("QU-%04d", maxSeq + 1);
    }

    @Transactional
    public Order createOrderFromQuote(Quote quote) {

        if ("CONVERTED".equalsIgnoreCase(quote.getStatus())) {
            throw new IllegalStateException(
                    "This quote has already been converted into an order.");
        }

        Order order = new Order();
        order.setQuote(quote);
        order.setCustomer(quote.getCustomer());
        order.setOrderNumber(generateOrderNumber());
        order.setOrderDate(LocalDate.now());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setGstRate(quote.getGstRate() != null ? quote.getGstRate() : new BigDecimal("5.00"));
        order.setPstRate(quote.getPstRate() != null ? quote.getPstRate() : new BigDecimal("7.00"));

        // Herda o turnaround e o rush fee da quote
        order.setTurnaroundOption(quote.getTurnaroundOption());
        order.setRushFee(quote.getRushFee() != null ? quote.getRushFee() : BigDecimal.ZERO);

        String notes = "Converted from quote " + quote.getQuoteNumber();
        if (quote.getNotes() != null && !quote.getNotes().trim().isEmpty()) {
            notes += "\n" + quote.getNotes();
        }
        order.setNotes(notes);

        if (quote.getItems() != null) {
            for (QuoteItem qi : quote.getItems()) {
                OrderItem item = new OrderItem();

                item.setProduct(qi.getProduct());

                String description = qi.getItemDescription();
                if (description == null || description.trim().isEmpty()) {
                    description = "Custom item";
                }

                item.setItemDescription(description);
                item.setQuantity(qi.getQuantity());
                item.setUnitPrice(qi.getUnitPrice());
                item.setPrintSpecsSummary(qi.getPrintSpecsSummary());
                item.setProductionStatus(OrderStatus.PENDING_FILE);

                order.addItem(item);
            }
        }

        calculateTotals(order);

        Order savedOrder = orderRepository.save(order);

        quote.setStatus("CONVERTED");
        quoteRepository.save(quote);

        return savedOrder;
    }

    private void calculateTotals(Order order) {
        BigDecimal subtotal = BigDecimal.ZERO;

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    BigDecimal itemTotal = item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP);
                    item.setTotalPrice(itemTotal);
                    subtotal = subtotal.add(itemTotal);
                }
            }
        }

        order.setSubtotal(subtotal);

        // Rush fee herdado da quote entra na base taxavel, igual a QuoteService
        BigDecimal rushFee = order.getRushFee() != null ? order.getRushFee() : BigDecimal.ZERO;
        order.setRushFee(rushFee);

        BigDecimal taxable = subtotal.add(rushFee);

        // GST
        BigDecimal gstAmount = taxable.multiply(order.getGstRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        order.setGstAmount(gstAmount);

        // PST
        BigDecimal pstAmount = taxable.multiply(order.getPstRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        order.setPstAmount(pstAmount);

        // Total = Subtotal + Rush fee + GST + PST
        order.setTotalAmount(taxable.add(gstAmount).add(pstAmount));
    }

    private String generateOrderNumber() {
        // Maior numero existente + 1 (nao usa count(), que duplicaria
        // numeros depois de apagar pedidos — mesmo bug do QU-1003)
        long maxSeq = 1000;

        for (Order o : orderRepository.findAll()) {
            String num = o.getOrderNumber();

            if (num != null && num.startsWith("OR-")) {
                try {
                    long seq = Long.parseLong(num.substring(3));

                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException ignored) {
                    // ignora order numbers fora do padrao OR-XXXX
                }
            }
        }

        return String.format("OR-%04d", maxSeq + 1);
    }
}