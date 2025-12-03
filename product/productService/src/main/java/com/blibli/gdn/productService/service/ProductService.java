package com.blibli.gdn.productService.service;

import com.blibli.gdn.productService.dto.request.ProductRequest;
import com.blibli.gdn.productService.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse createProduct(ProductRequest productRequest);

    ProductResponse getProduct(String id);

    Page<ProductResponse> searchProducts(String name, String category, Pageable pageable);
}
