package com.blibli.gdn.productService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GdnResponseData<T> {
    private T data;
    private String message;
    private Integer status;
    private boolean success;
}
