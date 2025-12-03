package com.blibli.gdn.productService.controller;

import com.blibli.gdn.productService.dto.GdnResponseData;
import com.blibli.gdn.productService.dto.response.ProductResponse;
import com.blibli.gdn.productService.mapper.ProductMapper;
import com.blibli.gdn.productService.model.Product;
import com.blibli.gdn.productService.service.VariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/products")
@RequiredArgsConstructor
@Slf4j
public class InternalProductController {

    private final VariantService variantService;
    private final ProductMapper productMapper;

    @GetMapping("/sku/{sku}")
    public ResponseEntity<GdnResponseData<ProductResponse>> getProductBySku(@PathVariable String sku) {
        log.info("Internal API: Looking up Product by SKU: {}", sku);
        Product product = variantService.findProductBySku(sku);
        ProductResponse productResponse = productMapper.toProductResponse(product);

        GdnResponseData<ProductResponse> response = GdnResponseData.<ProductResponse>builder()
                .data(productResponse)
                .message("Product found")
                .status(200)
                .success(true)
                .build();

        return ResponseEntity.ok(response);
    }
}
