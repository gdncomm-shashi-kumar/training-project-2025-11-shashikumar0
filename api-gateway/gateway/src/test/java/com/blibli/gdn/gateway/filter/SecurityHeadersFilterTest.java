package com.blibli.gdn.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityHeadersFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SecurityHeadersFilter filter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // Set default values
        ReflectionTestUtils.setField(filter, "securityHeadersEnabled", true);
        ReflectionTestUtils.setField(filter, "contentSecurityPolicy", "default-src 'self'");
        ReflectionTestUtils.setField(filter, "maxBodySize", 10485760L); // 10MB
    }

    @Test
    void testSecurityHeaders_ShouldBeAdded() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        when(request.getScheme()).thenReturn("https");
        when(request.getContentLengthLong()).thenReturn(1000L);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-Frame-Options", "DENY");
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response).setHeader("Content-Security-Policy", "default-src 'self'");
        verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        verify(response).setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testSecurityHeadersDisabled_ShouldSkip() throws Exception {
        // Given
        ReflectionTestUtils.setField(filter, "securityHeadersEnabled", false);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response, never()).setHeader(anyString(), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testRequestBodySizeExceeded_ShouldReject() throws Exception {
        // Given
        when(request.getContentLengthLong()).thenReturn(20000000L); // 20MB
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(413); // Request Entity Too Large
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testCacheHeaders_ShouldBeAddedForApiEndpoints() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        when(request.getScheme()).thenReturn("https");
        when(request.getContentLengthLong()).thenReturn(1000L);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
        verify(response).setHeader("Pragma", "no-cache");
        verify(response).setHeader("Expires", "0");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testHstsHeader_ShouldBeAddedForHttps() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        when(request.getScheme()).thenReturn("https");
        when(request.getContentLengthLong()).thenReturn(1000L);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testHstsHeader_ShouldNotBeAddedForHttp() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        when(request.getScheme()).thenReturn("http");
        when(request.getContentLengthLong()).thenReturn(1000L);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response, never()).setHeader(eq("Strict-Transport-Security"), anyString());
        verify(filterChain).doFilter(request, response);
    }
}

