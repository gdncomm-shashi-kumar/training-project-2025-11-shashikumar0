package com.blibli.gdn.productService.controller;

import com.blibli.gdn.productService.dto.GdnResponseData;
import com.blibli.gdn.productService.dto.request.ProductRequest;
import com.blibli.gdn.productService.dto.response.ProductResponse;
import com.blibli.gdn.productService.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<GdnResponseData<ProductResponse>> createProduct(
            @jakarta.validation.Valid @RequestBody ProductRequest productRequest,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.info("Create product request received for: {}", productRequest.getName());

        if (role == null || !role.contains("ROLE_ADMIN")) {
            log.warn("Forbidden: User does not have ROLE_ADMIN");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ProductResponse created = productService.createProduct(productRequest);
        GdnResponseData<ProductResponse> response = GdnResponseData.<ProductResponse>builder()
                .data(created)
                .message("Product created successfully")
                .status(HttpStatus.CREATED.value())
                .success(true)
                .build();

        log.info("Product created successfully with ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GdnResponseData<ProductResponse>> getProduct(@PathVariable String id) {
        log.info("Get product request for ID: {}", id);
        ProductResponse product = productService.getProduct(id);

        GdnResponseData<ProductResponse> response = GdnResponseData.<ProductResponse>builder()
                .data(product)
                .message("Product retrieved successfully")
                .status(HttpStatus.OK.value())
                .success(true)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<GdnResponseData<Page<ProductResponse>>> searchProducts(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {

        log.info("Search products request - name: {}, category: {}, page: {}, size: {}", name, category, page, size);

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        Page<ProductResponse> products = productService.searchProducts(name, category, pageable);

        GdnResponseData<Page<ProductResponse>> response = GdnResponseData.<Page<ProductResponse>>builder()
                .data(products)
                .message("Products retrieved successfully")
                .status(HttpStatus.OK.value())
                .success(true)
                .build();

        return ResponseEntity.ok(response);
    }
}
