package com.blibli.gdn.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitFilter filter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Set filter properties
        ReflectionTestUtils.setField(filter, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(filter, "defaultLimit", 300);
        ReflectionTestUtils.setField(filter, "perUser", true);
        ReflectionTestUtils.setField(filter, "perIp", false);
    }

    @Test
    void testRateLimitDisabled_ShouldPassThrough() throws Exception {
        // Given
        ReflectionTestUtils.setField(filter, "rateLimitEnabled", false);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void testWithinRateLimit_ShouldAllow() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        when(request.getAttribute("X-User-Id")).thenReturn("user123");
        when(request.getAttribute("traceId")).thenReturn("trace123");
        when(valueOperations.increment(anyString())).thenReturn(50L);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response).setHeader("X-RateLimit-Limit", "300");
        verify(response).setHeader("X-RateLimit-Remaining", "250");
        verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    void testExceedsRateLimit_ShouldBlock() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        when(request.getAttribute("X-User-Id")).thenReturn("user123");
        when(request.getAttribute("traceId")).thenReturn("trace123");
        when(valueOperations.increment(anyString())).thenReturn(301L);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(429);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testFirstRequest_ShouldSetExpiration() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        when(request.getAttribute("X-User-Id")).thenReturn("user123");
        when(request.getAttribute("traceId")).thenReturn("trace123");
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(redisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testUnauthenticatedUser_ShouldUseIpAddress() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        when(request.getAttribute("X-User-Id")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getAttribute("traceId")).thenReturn("trace123");
        when(valueOperations.increment(anyString())).thenReturn(10L);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(valueOperations).increment(contains("ip:192.168.1.1"));
    }
}

