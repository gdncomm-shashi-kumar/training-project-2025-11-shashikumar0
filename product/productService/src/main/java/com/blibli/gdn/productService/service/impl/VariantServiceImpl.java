package com.blibli.gdn.productService.service.impl;

import com.blibli.gdn.productService.exception.ProductNotFoundException;
import com.blibli.gdn.productService.model.Product;
import com.blibli.gdn.productService.model.Variant;
import com.blibli.gdn.productService.repository.ProductRepository;
import com.blibli.gdn.productService.service.VariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VariantServiceImpl implements VariantService {

    private final ProductRepository productRepository;

    @Override
    @Cacheable(value = "variants", key = "#sku")
    public Variant findBySku(String sku) {
        log.info("Looking up variant with SKU: {}", sku);
        Optional<Product> productOpt = productRepository.findByVariantsSku(sku);

        if (productOpt.isPresent()) {
            log.info("Product found for SKU: {}. Product ID: {}", sku, productOpt.get().getProductId());
            return productOpt.get().getVariants().stream()
                    .filter(variant -> sku.equals(variant.getSku()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("Variant not found in product variants list for SKU: {}", sku);
                        return new ProductNotFoundException("SKU not found in product variants: " + sku);
                    });
        } else {
            log.error("No product found containing variant with SKU: {}", sku);
            throw new ProductNotFoundException("Product not found for SKU: " + sku);
        }
    }

    @Override
    @Cacheable(value = "productsBySku", key = "#sku")
    public Product findProductBySku(String sku) {
        log.info("Looking up product with variant SKU: {}", sku);
        return productRepository.findByVariantsSku(sku)
                .orElseThrow(() -> new ProductNotFoundException("Product not found for SKU: " + sku));
    }
}
