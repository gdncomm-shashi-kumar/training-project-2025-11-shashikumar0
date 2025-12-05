package com.blibli.gdn.gateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayErrorResponse {
    

    private String timestamp;
    

    private Integer status;
    

    private String error;
    

    private String message;
    

    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    private String path;

    private String traceId;

    public static GatewayErrorResponse of(Integer status, String error, String message, String path, String traceId) {
        return GatewayErrorResponse.builder()
                .timestamp(ZonedDateTime.now().toString())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .traceId(traceId)
                .build();
    }

    public static GatewayErrorResponse of(Integer status, String error, String message, String path, String traceId, Map<String, Object> details) {
        return GatewayErrorResponse.builder()
                .timestamp(ZonedDateTime.now().toString())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .traceId(traceId)
                .details(details)
                .build();
    }
}
