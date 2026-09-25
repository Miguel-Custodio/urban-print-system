package com.urbanprint.backend.service;

import com.urbanprint.backend.model.Order;
import com.urbanprint.backend.model.OrderItem;
import com.urbanprint.backend.model.OrderStatus;
import com.urbanprint.backend.model.Quote;
import com.urbanprint.backend.model.QuoteItem;
import com.urbanprint.backend.repository.OrderRepository;
import com.urbanprint.backend.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final QuoteRepository quoteRepository;

    public OrderService(OrderRepository orderRepository, QuoteRepository quoteRepository) {
        this.orderRepository = orderRepository;
        this.quoteRepository = quoteRepository;
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

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
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
        order.setGstRate(quote.getGstRate());
        order.setPstRate(quote.getPstRate());
        order.setNotes(quote.getNotes());

        if (quote.getItems() != null) {
            for (QuoteItem qi : quote.getItems()) {
                OrderItem item = new OrderItem();

                item.setProduct(qi.getProduct());
                item.setItemDescription(qi.getItemDescription());
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

        // GST (5%)
        BigDecimal gstAmount = subtotal.multiply(order.getGstRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        order.setGstAmount(gstAmount);

        // PST (7%)
        BigDecimal pstAmount = subtotal.multiply(order.getPstRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        order.setPstAmount(pstAmount);

        // Total = Subtotal + GST + PST
        order.setTotalAmount(subtotal.add(gstAmount).add(pstAmount));
    }

    private String generateOrderNumber() {
        long count = orderRepository.count();
        return String.format("OR-%04d", count + 1001);
    }
}