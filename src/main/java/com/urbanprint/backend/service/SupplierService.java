package com.urbanprint.backend.service;

import com.urbanprint.backend.model.Supplier;
import com.urbanprint.backend.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        return supplierRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Supplier> findActive() {
        return supplierRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Supplier> findById(Long id) {
        return supplierRepository.findById(id);
    }

    @Transactional
    public Supplier create(Supplier supplier) {
        supplier.setId(null);

        if (supplier.getName() == null || supplier.getName().isBlank()) {
            throw new IllegalArgumentException("Supplier name is required.");
        }

        String name = supplier.getName().trim();
        supplier.setName(name);

        if (supplierRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "A supplier with the name \"" + name + "\" already exists."
            );
        }

        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier update(Long id, Supplier data) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));

        if (data.getName() == null || data.getName().isBlank()) {
            throw new IllegalArgumentException("Supplier name is required.");
        }

        String name = data.getName().trim();

        supplierRepository.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException(
                            "A supplier with the name \"" + name + "\" already exists."
                    );
                });

        supplier.setName(name);
        supplier.setContactName(data.getContactName());
        supplier.setPhone(data.getPhone());
        supplier.setEmail(data.getEmail());
        supplier.setAddress(data.getAddress());
        supplier.setCity(data.getCity());
        supplier.setPaymentTerms(data.getPaymentTerms());
        supplier.setNotes(data.getNotes());
        supplier.setActive(data.isActive());

        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier setActive(Long id, boolean active) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));

        supplier.setActive(active);
        return supplierRepository.save(supplier);
    }
}