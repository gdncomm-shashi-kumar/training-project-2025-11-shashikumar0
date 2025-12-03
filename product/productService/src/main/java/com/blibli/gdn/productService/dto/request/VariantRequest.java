package com.blibli.gdn.productService.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantRequest {
    @NotBlank(message = "SKU is required")
    private String sku;
    
    private String size;
    
    private String color;
    
    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    private Double price;
    
    @Min(value = 0, message = "Stock must be non-negative")
    private Integer stock;
}
