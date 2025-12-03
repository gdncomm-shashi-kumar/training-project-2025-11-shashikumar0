package com.blibli.gdn.productService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantResponse {
    private String sku;
    private String size;
    private String color;
    private Double price;
    private Integer stock;
}
