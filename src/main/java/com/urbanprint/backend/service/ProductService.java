package com.urbanprint.backend.service;

import com.urbanprint.backend.model.Product;
import com.urbanprint.backend.model.ProductPriceScale;
import com.urbanprint.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Product save(Product product) {
        if (product.getPriceScales() != null) {
            for (ProductPriceScale scale : product.getPriceScales()) {
                scale.setProduct(product);
                scale.calculateDerivedFields();
            }
        }
        return productRepository.save(product);
    }

    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }
}