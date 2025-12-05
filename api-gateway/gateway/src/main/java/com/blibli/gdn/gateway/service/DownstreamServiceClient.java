package com.blibli.gdn.gateway.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownstreamServiceClient {

    @Value("${services.member.url}")
    private String memberServiceUrl;

    @Value("${services.product.url}")
    private String productServiceUrl;

    @Value("${services.cart.url}")
    private String cartServiceUrl;

    private final RestClient restClient = RestClient.create();

    @CircuitBreaker(name = "memberService", fallbackMethod = "memberServiceFallback")
    @TimeLimiter(name = "memberService")
    public ResponseEntity<Map<String, Object>> checkMemberService() {
        log.debug("Calling Member Service health check");

        try {
            ResponseEntity<Map> response = restClient.get()
                    .uri(memberServiceUrl + "/actuator/health")
                    .retrieve()
                    .toEntity(Map.class);

            log.debug("Member Service responded: {}", response.getStatusCode());
            return ResponseEntity.ok((Map<String, Object>) response.getBody());

        } catch (RestClientException e) {
            log.error("Member Service call failed: {}", e.getMessage());
            throw e;
        }
    }


    @CircuitBreaker(name = "productService", fallbackMethod = "productServiceFallback")
    @TimeLimiter(name = "productService")
    public ResponseEntity<Map<String, Object>> checkProductService() {
        log.debug("Calling Product Service health check");

        try {
            ResponseEntity<Map> response = restClient.get()
                    .uri(productServiceUrl + "/actuator/health")
                    .retrieve()
                    .toEntity(Map.class);

            log.debug("Product Service responded: {}", response.getStatusCode());
            return ResponseEntity.ok((Map<String, Object>) response.getBody());

        } catch (RestClientException e) {
            log.error("Product Service call failed: {}", e.getMessage());
            throw e;
        }
    }


    @CircuitBreaker(name = "cartService", fallbackMethod = "cartServiceFallback")
    @TimeLimiter(name = "cartService")
    public ResponseEntity<Map<String, Object>> checkCartService() {
        log.debug("Calling Cart Service health check");

        try {
            ResponseEntity<Map> response = restClient.get()
                    .uri(cartServiceUrl + "/actuator/health")
                    .retrieve()
                    .toEntity(Map.class);

            log.debug("Cart Service responded: {}", response.getStatusCode());
            return ResponseEntity.ok((Map<String, Object>) response.getBody());

        } catch (RestClientException e) {
            log.error("Cart Service call failed: {}", e.getMessage());
            throw e;
        }
    }


    private ResponseEntity<Map<String, Object>> memberServiceFallback(Exception e) {
        log.warn("Member Service circuit breaker activated: {}", e.getMessage());

        Map<String, Object> fallbackResponse = Map.of(
                "status", "CIRCUIT_OPEN",
                "service", "member-service",
                "message", "Service temporarily unavailable. Circuit breaker is OPEN.",
                "fallback", true
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }


    private ResponseEntity<Map<String, Object>> productServiceFallback(Exception e) {
        log.warn("Product Service circuit breaker activated: {}", e.getMessage());

        Map<String, Object> fallbackResponse = Map.of(
                "status", "CIRCUIT_OPEN",
                "service", "product-service",
                "message", "Service temporarily unavailable. Circuit breaker is OPEN.",
                "fallback", true
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }


    private ResponseEntity<Map<String, Object>> cartServiceFallback(Exception e) {
        log.warn("Cart Service circuit breaker activated: {}", e.getMessage());

        Map<String, Object> fallbackResponse = Map.of(
                "status", "CIRCUIT_OPEN",
                "service", "cart-service",
                "message", "Service temporarily unavailable. Circuit breaker is OPEN.",
                "fallback", true
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }
}

