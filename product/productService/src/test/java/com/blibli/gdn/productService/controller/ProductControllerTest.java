package com.blibli.gdn.productService.controller;

import com.blibli.gdn.productService.dto.request.ProductRequest;
import com.blibli.gdn.productService.dto.request.VariantRequest;
import com.blibli.gdn.productService.dto.response.ProductResponse;
import com.blibli.gdn.productService.dto.response.VariantResponse;
import com.blibli.gdn.productService.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductResponse productResponse;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        productResponse = ProductResponse.builder()
                .id("1")
                .productId("P001")
                .name("Test Product")
                .category("Electronics")
                .variants(java.util.Collections.singletonList(
                        VariantResponse.builder()
                                .sku("P001-BLACK-001")
                                .price(100.0)
                                .stock(10)
                                .color("Black")
                                .size("M")
                                .build()
                ))
                .build();
                
        productRequest = ProductRequest.builder()
                .productId("P001")
                .name("Test Product")
                .category("Electronics")
                .variants(java.util.Collections.singletonList(
                        VariantRequest.builder()
                                .sku("P001-BLACK-001")
                                .price(100.0)
                                .stock(10)
                                .color("Black")
                                .size("M")
                                .build()
                ))
                .build();
    }

    @Test
    void createProduct_Admin() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(productResponse);

        mockMvc.perform(post("/api/v1/products")
                .header("X-User-Role", "ROLE_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Test Product"))
                .andExpect(jsonPath("$.message").value("Product created successfully"));
    }

    @Test
    void createProduct_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .header("X-User-Role", "ROLE_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProduct() throws Exception {
        when(productService.getProduct("1")).thenReturn(productResponse);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.message").value("Product retrieved successfully"));
    }

    @Test
    void searchProducts() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(Collections.singletonList(productResponse));
        when(productService.searchProducts(anyString(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/products")
                .param("name", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Test Product"))
                .andExpect(jsonPath("$.message").value("Products retrieved successfully"));
    }
}
