package com.blibli.gdn.productService.service.impl;

import com.blibli.gdn.productService.dto.request.ProductRequest;
import com.blibli.gdn.productService.dto.response.ProductResponse;
import com.blibli.gdn.productService.exception.ProductNotFoundException;
import com.blibli.gdn.productService.mapper.ProductMapper;
import com.blibli.gdn.productService.model.Product;
import com.blibli.gdn.productService.repository.ProductRepository;
import com.blibli.gdn.productService.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        log.info("Creating product: {}", productRequest.getName());
        Product product = productMapper.toProduct(productRequest);
        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(String id) {
        log.info("Fetching product with productId: {}", id);
        Product product = productRepository.findByProductId(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with productId: " + id));
        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String name, String category, Pageable pageable) {
        log.info("Searching products with name: {}, category: {}", name, category);
        Page<Product> products;
        if (category != null && !category.isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCaseAndCategory(name, category, pageable);
        } else {
            products = productRepository.findByNameContainingIgnoreCase(name, pageable);
        }
        return products.map(productMapper::toProductResponse);
    }
}
