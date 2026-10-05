package com.urbanprint.backend.controller;

import com.urbanprint.backend.model.Customer;
import com.urbanprint.backend.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Customer create(@RequestBody Customer customer) {
        return customerService.save(customer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> update(
            @PathVariable Long id,
            @RequestBody Customer customer
    ) {
        return customerService.findById(id)
                .map(existingCustomer -> {
                    existingCustomer.setName(customer.getName());
                    existingCustomer.setCompanyName(customer.getCompanyName());
                    existingCustomer.setContactPosition(
                            customer.getContactPosition()
                    );
                    existingCustomer.setWebsite(customer.getWebsite());
                    existingCustomer.setEmail(customer.getEmail());
                    existingCustomer.setPhone(customer.getPhone());
                    existingCustomer.setAddress(customer.getAddress());
                    existingCustomer.setCity(customer.getCity());
                    existingCustomer.setPostalCode(customer.getPostalCode());
                    existingCustomer.setActive(customer.isActive());

                    return ResponseEntity.ok(
                            customerService.save(existingCustomer)
                    );
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return customerService.findById(id)
                .map(customer -> {
                    customerService.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}