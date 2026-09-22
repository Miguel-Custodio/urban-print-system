package com.urbanprint.backend.controller;

import com.urbanprint.backend.model.Product;
import com.urbanprint.backend.model.ProductPriceScale;
import com.urbanprint.backend.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> findAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Product create(@RequestBody Product product) {
        return productService.save(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable Long id,
            @RequestBody Product product
    ) {
        return productService.findById(id)
                .map(existingProduct -> {
                    existingProduct.setProductName(product.getProductName());
                    existingProduct.setItemCode(product.getItemCode());
                    existingProduct.setCategory(product.getCategory());
                    existingProduct.setDescription(product.getDescription());
                    existingProduct.setPricingMethod(product.getPricingMethod());
                    existingProduct.setUnit(product.getUnit());
                    existingProduct.setBaseCost(product.getBaseCost());
                    existingProduct.setBasePrice(product.getBasePrice());
                    existingProduct.setMinimumOrderFee(product.getMinimumOrderFee());
                    existingProduct.setDefaultWidth(product.getDefaultWidth());
                    existingProduct.setDefaultHeight(product.getDefaultHeight());
                    existingProduct.setDefaultStock(product.getDefaultStock());
                    existingProduct.setSidesInks(product.getSidesInks());
                    existingProduct.setProductionDepartment(product.getProductionDepartment());
                    existingProduct.setTurnaroundDays(product.getTurnaroundDays());
                    existingProduct.setActive(product.isActive());

                    existingProduct.getPriceScales().clear();
                    if (product.getPriceScales() != null) {
                        for (ProductPriceScale scale : product.getPriceScales()) {
                            existingProduct.addPriceScale(scale);
                        }
                    }

                    return ResponseEntity.ok(productService.save(existingProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return productService.findById(id)
                .map(product -> {
                    productService.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}