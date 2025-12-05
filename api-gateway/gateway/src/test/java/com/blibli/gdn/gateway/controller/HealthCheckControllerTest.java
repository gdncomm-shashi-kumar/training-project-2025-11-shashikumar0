package com.blibli.gdn.gateway.controller;

import com.blibli.gdn.gateway.service.DownstreamServiceClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HealthCheckControllerTest {

    @Mock
    private DownstreamServiceClient downstreamServiceClient;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    private HealthCheckController controller;

    @BeforeEach
    void setUp() {
        controller = new HealthCheckController(downstreamServiceClient, circuitBreakerRegistry);
        
        // Mock circuit breaker registry to return empty optional by default
        when(circuitBreakerRegistry.find(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void testHealth_ShouldReturnUpStatus() {
        // When
        ResponseEntity<Map<String, Object>> response = controller.health();

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("API Gateway");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void testServicesHealth_AllServicesUp() {
        // Given
        Map<String, Object> memberResponse = Map.of("status", "UP");
        Map<String, Object> productResponse = Map.of("status", "UP");
        Map<String, Object> cartResponse = Map.of("status", "UP");
        
        when(downstreamServiceClient.checkMemberService())
                .thenReturn(ResponseEntity.ok(memberResponse));
        when(downstreamServiceClient.checkProductService())
                .thenReturn(ResponseEntity.ok(productResponse));
        when(downstreamServiceClient.checkCartService())
                .thenReturn(ResponseEntity.ok(cartResponse));

        // When
        ResponseEntity<Map<String, Object>> response = controller.servicesHealth();

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKeys("memberService", "productService", "cartService");
        
        verify(downstreamServiceClient).checkMemberService();
        verify(downstreamServiceClient).checkProductService();
        verify(downstreamServiceClient).checkCartService();
    }

    @Test
    void testServicesHealth_ServiceDown() {
        // Given - Member service returns fallback response and circuit breaker is OPEN
        Map<String, Object> fallbackResponse = Map.of(
                "status", "CIRCUIT_OPEN",
                "service", "member-service",
                "fallback", true
        );
        
        // Mock circuit breaker to return OPEN state
        CircuitBreaker.State openState = CircuitBreaker.State.OPEN;
        when(circuitBreaker.getState()).thenReturn(openState);
        when(circuitBreakerRegistry.find("memberService")).thenReturn(Optional.of(circuitBreaker));
        
        when(downstreamServiceClient.checkMemberService())
                .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse));
        when(downstreamServiceClient.checkProductService())
                .thenReturn(ResponseEntity.ok(Map.of("status", "UP")));
        when(downstreamServiceClient.checkCartService())
                .thenReturn(ResponseEntity.ok(Map.of("status", "UP")));

        // When
        ResponseEntity<Map<String, Object>> response = controller.servicesHealth();

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKeys("memberService", "productService", "cartService");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> memberStatus = (Map<String, Object>) response.getBody().get("memberService");
        assertThat(memberStatus.get("status")).isEqualTo("CIRCUIT_OPEN");
        assertThat(memberStatus.get("circuitBreaker")).isEqualTo("OPEN");
    }

    @Test
    void testCircuitBreakersStatus() {
        // When
        ResponseEntity<Map<String, Object>> response = controller.circuitBreakersStatus();

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        
        // Verify circuit breaker registry was queried
        verify(circuitBreakerRegistry, atLeastOnce()).find(anyString());
    }
}

