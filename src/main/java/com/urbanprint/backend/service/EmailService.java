package com.urbanprint.backend.service;

import com.urbanprint.backend.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendInvoiceEmail(Order order) {
        if (order.getCustomer() == null || order.getCustomer().getEmail() == null
                || order.getCustomer().getEmail().trim().isEmpty()) {
            throw new RuntimeException("Customer has no email on file.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(order.getCustomer().getEmail());
        message.setSubject("Invoice for Order " + order.getOrderNumber() + " — Urban Print Shop");
        message.setText(
            "Hello " + safeName(order.getCustomer().getName()) + ",\n\n" +
            "Please find the details of your invoice below.\n\n" +
            "Order Number: " + order.getOrderNumber() + "\n" +
            "Order Date: " + order.getOrderDate() + "\n" +
            "Total Amount: $" + order.getTotalAmount() + " CAD\n\n" +
            "Thank you for your business.\n\n" +
            "Urban Print Shop\n" +
            "7993 Enterprise St, Burnaby, BC\n" +
            "(604) 420-3298 | sales@urbanprintshop.ca"
    );

        mailSender.send(message);
    }

    private String safeName(String name) {
        return name != null ? name : "Customer";
    }
}