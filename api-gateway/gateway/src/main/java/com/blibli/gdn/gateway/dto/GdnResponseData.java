package com.blibli.gdn.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GdnResponseData<T> {
    private boolean success;
    private String message;
    private T data;
    private String traceId;
    private LocalDateTime timestamp;

    public static <T> GdnResponseData<T> success(T data, String message) {
        return GdnResponseData.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> GdnResponseData<T> error(String message) {
        return GdnResponseData.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

