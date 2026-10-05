package com.urbanprint.backend.service;

import com.urbanprint.backend.model.Product;
import com.urbanprint.backend.model.ProductPriceScale;
import com.urbanprint.backend.model.ProductSize;
import com.urbanprint.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Product save(Product product) {
        if (product.getSizes() != null) {
            for (ProductSize size : product.getSizes()) {
                size.setProduct(product);
                if (size.getPriceScales() != null) {
                    for (ProductPriceScale scale : size.getPriceScales()) {
                        scale.setSize(size);
                        scale.calculateDerivedFields();
                    }
                }
            }
        }
        return productRepository.save(product);
    }

    @Transactional
    public void deleteById(Long id) {
        // Removes legacy price scale rows (old product_id column) that are
        // not managed by the entity cascade, then deletes the product.
        productRepository.deleteLegacyPriceScalesByProductId(id);
        productRepository.deleteById(id);
    }
}