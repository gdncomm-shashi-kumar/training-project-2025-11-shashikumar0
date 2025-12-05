package com.blibli.gdn.gateway.controller;

import com.blibli.gdn.gateway.service.DownstreamServiceClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthCheckController {

    private final DownstreamServiceClient downstreamServiceClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;


    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "API Gateway");
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }


    @GetMapping("/health/services")
    public ResponseEntity<Map<String, Object>> servicesHealth() {
        Map<String, Object> servicesHealth = new HashMap<>();

        try {
            ResponseEntity<Map<String, Object>> memberHealth = downstreamServiceClient.checkMemberService();
            servicesHealth.put("memberService", createHealthStatus(memberHealth, "memberService"));
        } catch (Exception e) {
            servicesHealth.put("memberService", createErrorStatus("memberService", e));
        }

        try {
            ResponseEntity<Map<String, Object>> productHealth = downstreamServiceClient.checkProductService();
            servicesHealth.put("productService", createHealthStatus(productHealth, "productService"));
        } catch (Exception e) {
            servicesHealth.put("productService", createErrorStatus("productService", e));
        }

        try {
            ResponseEntity<Map<String, Object>> cartHealth = downstreamServiceClient.checkCartService();
            servicesHealth.put("cartService", createHealthStatus(cartHealth, "cartService"));
        } catch (Exception e) {
            servicesHealth.put("cartService", createErrorStatus("cartService", e));
        }

        return ResponseEntity.ok(servicesHealth);
    }


    @GetMapping("/health/circuit-breakers")
    public ResponseEntity<Map<String, Object>> circuitBreakersStatus() {
        Map<String, Object> circuitBreakers = new HashMap<>();

        circuitBreakerRegistry.find("memberService").ifPresent(cb -> {
            Map<String, Object> cbStatus = new HashMap<>();
            cbStatus.put("state", cb.getState().toString());
            cbStatus.put("failureRate", cb.getMetrics().getFailureRate());
            cbStatus.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            cbStatus.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            circuitBreakers.put("memberService", cbStatus);
        });

        circuitBreakerRegistry.find("productService").ifPresent(cb -> {
            Map<String, Object> cbStatus = new HashMap<>();
            cbStatus.put("state", cb.getState().toString());
            cbStatus.put("failureRate", cb.getMetrics().getFailureRate());
            cbStatus.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            cbStatus.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            circuitBreakers.put("productService", cbStatus);
        });

        circuitBreakerRegistry.find("cartService").ifPresent(cb -> {
            Map<String, Object> cbStatus = new HashMap<>();
            cbStatus.put("state", cb.getState().toString());
            cbStatus.put("failureRate", cb.getMetrics().getFailureRate());
            cbStatus.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            cbStatus.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            circuitBreakers.put("cartService", cbStatus);
        });

        return ResponseEntity.ok(circuitBreakers);
    }


    private Map<String, Object> createHealthStatus(ResponseEntity<Map<String, Object>> response, String serviceName) {
        Map<String, Object> status = new HashMap<>();

        boolean isFallback = response.getBody() != null &&
                Boolean.TRUE.equals(response.getBody().get("fallback"));

        if (isFallback) {
            status.put("status", "CIRCUIT_OPEN");
            status.put("circuitBreaker", "OPEN");
            status.put("message", response.getBody().get("message"));
        } else {
            status.put("status", response.getStatusCode().is2xxSuccessful() ? "UP" : "DOWN");
            status.put("circuitBreaker", getCircuitBreakerState(serviceName));
            status.put("details", response.getBody());
        }

        return status;
    }


    private Map<String, Object> createErrorStatus(String serviceName, Exception e) {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "DOWN");
        status.put("circuitBreaker", getCircuitBreakerState(serviceName));
        status.put("error", e.getMessage());
        return status;
    }


    private String getCircuitBreakerState(String circuitBreakerName) {
        return circuitBreakerRegistry.find(circuitBreakerName)
                .map(cb -> cb.getState().toString())
                .orElse("UNKNOWN");
    }
}
