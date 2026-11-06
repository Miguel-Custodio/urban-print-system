package com.urbanprint.backend.controller;

import com.urbanprint.backend.model.Customer;
import com.urbanprint.backend.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<Customer> findAll() {
        return customerService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> findById(@PathVariable Long id) {
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody Customer customer) {
        try {
            return ResponseEntity.ok(customerService.save(customer));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(
            @PathVariable Long id,
            @RequestBody Customer customer
    ) {
        Optional<Customer> existingOpt = customerService.findById(id);

        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Customer existingCustomer = existingOpt.get();

        existingCustomer.setName(customer.getName());
        existingCustomer.setCompanyName(customer.getCompanyName());
        existingCustomer.setContactPosition(customer.getContactPosition());
        existingCustomer.setWebsite(customer.getWebsite());
        existingCustomer.setEmail(customer.getEmail());
        existingCustomer.setPhone(customer.getPhone());
        existingCustomer.setAddress(customer.getAddress());
        existingCustomer.setCity(customer.getCity());
        existingCustomer.setPostalCode(customer.getPostalCode());
        existingCustomer.setGstApplicable(customer.isGstApplicable());
        existingCustomer.setPstApplicable(customer.isPstApplicable());
        existingCustomer.setActive(customer.isActive());

        try {
            return ResponseEntity.ok(customerService.save(existingCustomer));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        if (customerService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            customerService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/duplicates")
    public List<Map<String, Object>> findDuplicates() {
        return customerService.findDuplicateGroups();
    }

    @PostMapping("/merge-duplicates")
    public ResponseEntity<List<Map<String, Object>>> mergeDuplicates() {
        return ResponseEntity.ok(customerService.mergeDuplicates());
    }
}