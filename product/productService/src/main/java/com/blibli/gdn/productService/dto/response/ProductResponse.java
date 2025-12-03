package com.blibli.gdn.productService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private String id;
    private String productId;
    private String name;
    private String description;
    private String category;
    private String brand;
    private List<String> tags;
    private List<VariantResponse> variants;
    private Instant createdAt;
    private Instant updatedAt;
}
