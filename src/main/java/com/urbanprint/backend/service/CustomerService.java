package com.urbanprint.backend.service;

import com.urbanprint.backend.model.Customer;
import com.urbanprint.backend.repository.CustomerRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final JdbcTemplate jdbcTemplate;

    public CustomerService(CustomerRepository customerRepository, JdbcTemplate jdbcTemplate) {
        this.customerRepository = customerRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    /**
     * Saves a customer and blocks duplicated e-mails.
     * Throws IllegalStateException with a readable message when the e-mail
     * already belongs to another customer (the controller converts it to 409).
     */
    public Customer save(Customer customer) {
        customer.setEmail(normalizeEmail(customer.getEmail()));

        String email = customer.getEmail();

        if (email != null && !email.isBlank()) {
            customerRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
                boolean sameCustomer = customer.getId() != null
                        && customer.getId().equals(existing.getId());

                if (!sameCustomer) {
                    throw new IllegalStateException(
                            "A customer with the email \"" + email
                            + "\" already exists (customer #" + existing.getId()
                            + "). Duplicated emails are not allowed.");
                }
            });
        }

        return customerRepository.save(customer);
    }

    /**
     * Deletes a customer only when there are no quotes or orders linked to it.
     */
    public void deleteById(Long id) {
        long quotes = linkedQuoteCount(id);
        long orders = linkedOrderCount(id);

        if (quotes > 0 || orders > 0) {
            throw new IllegalStateException(
                    "Cannot delete customer #" + id + ": there are " + quotes
                    + " quote(s) and " + orders
                    + " order(s) linked to it. Move or delete them first.");
        }

        customerRepository.deleteById(id);
    }

    /**
     * Preview of duplicated contacts (same normalized e-mail).
     * Each group lists every customer with its quote/order counts.
     */
    public List<Map<String, Object>> findDuplicateGroups() {
        List<Map<String, Object>> groups = new ArrayList<>();

        duplicateGroupsByEmail().forEach((email, group) -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("email", email);

            List<Map<String, Object>> customers = new ArrayList<>();
            for (Customer customer : group) {
                customers.add(toCustomerMap(customer));
            }

            result.put("customers", customers);
            groups.add(result);
        });

        return groups;
    }

    /**
     * Merges each duplicated group: keeps the customer with the most linked
     * history (ties keep the lowest id), moves its quotes/orders to it and
     * deletes the other duplicated contacts.
     */
    @Transactional
    public List<Map<String, Object>> mergeDuplicates() {
        List<Map<String, Object>> summary = new ArrayList<>();

        for (Map.Entry<String, List<Customer>> entry : duplicateGroupsByEmail().entrySet()) {
            List<Customer> group = entry.getValue();

            Customer keeper = group.get(0);
            long keeperLinks = linkedCount(keeper.getId());

            for (Customer candidate : group) {
                long links = linkedCount(candidate.getId());

                if (links > keeperLinks
                        || (links == keeperLinks && candidate.getId() < keeper.getId())) {
                    keeper = candidate;
                    keeperLinks = links;
                }
            }

            List<Long> deletedIds = new ArrayList<>();
            long movedQuotes = 0;
            long movedOrders = 0;

            for (Customer other : group) {
                if (other.getId().equals(keeper.getId())) {
                    continue;
                }

                movedQuotes += jdbcTemplate.update(
                        "UPDATE quotes SET customer_id = ? WHERE customer_id = ?",
                        keeper.getId(), other.getId());

                movedOrders += jdbcTemplate.update(
                        "UPDATE orders SET customer_id = ? WHERE customer_id = ?",
                        keeper.getId(), other.getId());

                jdbcTemplate.update("DELETE FROM customers WHERE id = ?", other.getId());
                deletedIds.add(other.getId());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("email", entry.getKey());
            result.put("keptCustomerId", keeper.getId());
            result.put("deletedCustomerIds", deletedIds);
            result.put("movedQuotes", movedQuotes);
            result.put("movedOrders", movedOrders);
            summary.add(result);
        }

        return summary;
    }

    // ==== helpers ====

    private Map<String, List<Customer>> duplicateGroupsByEmail() {
        Map<String, List<Customer>> allByEmail = new LinkedHashMap<>();

        for (Customer customer : customerRepository.findAll()) {
            String email = normalizeEmail(customer.getEmail());

            if (email == null) {
                continue;
            }

            allByEmail.computeIfAbsent(email, key -> new ArrayList<>()).add(customer);
        }

        Map<String, List<Customer>> duplicates = new LinkedHashMap<>();
        allByEmail.forEach((email, group) -> {
            if (group.size() > 1) {
                duplicates.put(email, group);
            }
        });

        return duplicates;
    }

    private Map<String, Object> toCustomerMap(Customer customer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", customer.getId());
        map.put("name", customer.getName());
        map.put("companyName", customer.getCompanyName());
        map.put("email", customer.getEmail());
        map.put("active", customer.isActive());
        map.put("quoteCount", linkedQuoteCount(customer.getId()));
        map.put("orderCount", linkedOrderCount(customer.getId()));
        map.put("linkedTotal", linkedCount(customer.getId()));
        return map;
    }

    private long linkedQuoteCount(Long customerId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quotes WHERE customer_id = ?",
                Integer.class, customerId);
        return count == null ? 0 : count;
    }

    private long linkedOrderCount(Long customerId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE customer_id = ?",
                Integer.class, customerId);
        return count == null ? 0 : count;
    }

    private long linkedCount(Long customerId) {
        return linkedQuoteCount(customerId) + linkedOrderCount(customerId);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        String normalized = email.trim().toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }
}