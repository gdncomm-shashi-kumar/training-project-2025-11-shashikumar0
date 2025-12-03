package com.blibli.gdn.productService.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private java.time.Instant timestamp;
    private int status;
    private String error;
    private String message;
    private Object details;
    private String path;
    private String traceId;
}
