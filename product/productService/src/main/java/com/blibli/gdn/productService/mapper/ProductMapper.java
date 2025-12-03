package com.blibli.gdn.productService.mapper;

import com.blibli.gdn.productService.dto.request.ProductRequest;
import com.blibli.gdn.productService.dto.request.VariantRequest;
import com.blibli.gdn.productService.dto.response.ProductResponse;
import com.blibli.gdn.productService.dto.response.VariantResponse;
import com.blibli.gdn.productService.model.Product;
import com.blibli.gdn.productService.model.Variant;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    public Product toProduct(ProductRequest request) {
        if (request == null) {
            return null;
        }
        return Product.builder()
                .productId(request.getProductId())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .brand(request.getBrand())
                .tags(request.getTags())
                .variants(toVariants(request.getVariants()))
                .build();
    }

    public ProductResponse toProductResponse(Product product) {
        if (product == null) {
            return null;
        }
        return ProductResponse.builder()
                .id(product.getId())
                .productId(product.getProductId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .tags(product.getTags())
                .variants(toVariantResponses(product.getVariants()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private List<Variant> toVariants(List<VariantRequest> variantRequests) {
        if (variantRequests == null) {
            return Collections.emptyList();
        }
        return variantRequests.stream()
                .map(this::toVariant)
                .collect(Collectors.toList());
    }

    private Variant toVariant(VariantRequest request) {
        if (request == null) {
            return null;
        }
        return Variant.builder()
                .sku(request.getSku())
                .size(request.getSize())
                .color(request.getColor())
                .price(request.getPrice())
                .stock(request.getStock())
                .build();
    }

    private List<VariantResponse> toVariantResponses(List<Variant> variants) {
        if (variants == null) {
            return Collections.emptyList();
        }
        return variants.stream()
                .map(this::toVariantResponse)
                .collect(Collectors.toList());
    }

    private VariantResponse toVariantResponse(Variant variant) {
        if (variant == null) {
            return null;
        }
        return VariantResponse.builder()
                .sku(variant.getSku())
                .size(variant.getSize())
                .color(variant.getColor())
                .price(variant.getPrice())
                .stock(variant.getStock())
                .build();
    }
}
