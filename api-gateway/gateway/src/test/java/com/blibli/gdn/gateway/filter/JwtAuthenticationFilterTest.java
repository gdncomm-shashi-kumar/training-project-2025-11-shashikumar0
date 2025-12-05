package com.blibli.gdn.gateway.filter;

import com.blibli.gdn.gateway.config.PublicEndpointsConfig;
import com.blibli.gdn.gateway.service.TokenDenylistService;
import com.blibli.gdn.gateway.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PublicEndpointsConfig publicEndpointsConfig;

    @Mock
    private TokenDenylistService tokenDenylistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        lenient().when(publicEndpointsConfig.getPublicEndpoints()).thenReturn(
            Arrays.asList("/api/v1/auth/login", "/api/v1/auth/register", "/health", "/actuator/**")
        );
        lenient().when(publicEndpointsConfig.getOptionalAuthEndpoints()).thenReturn(
            Arrays.asList("/api/v1/cart/**")
        );
        lenient().when(tokenDenylistService.isTokenDenied(anyString())).thenReturn(false);
    }

    @Test
    void testPublicEndpoint_ShouldSkipAuthentication() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).parseToken(anyString());
    }

    @Test
    void testProtectedEndpoint_WithValidToken_ShouldAuthenticate() throws Exception {
        // Given
        String token = "valid.jwt.token";
        when(request.getRequestURI()).thenReturn("/api/v1/members/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        Claims claims = Jwts.claims()
                .subject("member123")
                .add("email", "user@example.com")
                .add("role", "USER")
                .add("type", "access")
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .build();

        when(jwtUtil.parseToken(token)).thenReturn(claims);
        when(jwtUtil.isAccessToken(claims)).thenReturn(true);
        when(jwtUtil.isTokenExpired(claims)).thenReturn(false);
        when(jwtUtil.getMemberId(claims)).thenReturn("member123");
        when(jwtUtil.getEmail(claims)).thenReturn("user@example.com");
        when(jwtUtil.getRole(claims)).thenReturn("USER");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(request).setAttribute("X-User-Id", "member123");
        verify(request).setAttribute("X-User-Email", "user@example.com");
        verify(request).setAttribute("X-User-Role", "USER");
    }

    @Test
    void testProtectedEndpoint_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/members/profile");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testProtectedEndpoint_WithExpiredToken_ShouldReturnUnauthorized() throws Exception {
        // Given
        String token = "expired.jwt.token";
        when(request.getRequestURI()).thenReturn("/api/v1/members/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.parseToken(token)).thenThrow(new ExpiredJwtException(null, null, "Token expired"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testProtectedEndpoint_WithMalformedToken_ShouldReturnUnauthorized() throws Exception {
        // Given
        String token = "malformed.token";
        when(request.getRequestURI()).thenReturn("/api/v1/members/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.parseToken(token)).thenThrow(new MalformedJwtException("Malformed"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testProtectedEndpoint_WithInvalidSignature_ShouldReturnUnauthorized() throws Exception {
        // Given
        String token = "invalid.signature.token";
        when(request.getRequestURI()).thenReturn("/api/v1/members/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.parseToken(token)).thenThrow(new SignatureException("Invalid signature"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testProtectedEndpoint_WithDeniedToken_ShouldReturnUnauthorized() throws Exception {
        // Given
        String token = "valid.but.denied.token";
        when(request.getRequestURI()).thenReturn("/api/v1/members/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        
        Claims claims = Jwts.claims()
                .subject("member123")
                .add("email", "user@example.com")
                .add("role", "USER")
                .add("type", "access")
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .build();

        when(jwtUtil.parseToken(token)).thenReturn(claims);
        when(tokenDenylistService.isTokenDenied(token)).thenReturn(true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(401);
        verify(tokenDenylistService).isTokenDenied(token);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testOptionalAuthEndpoint_WithValidToken_ShouldAuthenticateUser() throws Exception {
        // Given
        String token = "valid.jwt.token";
        when(request.getRequestURI()).thenReturn("/api/v1/cart");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        Claims claims = Jwts.claims()
                .subject("member123")
                .add("email", "user@example.com")
                .add("role", "USER")
                .add("type", "access")
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .build();

        when(jwtUtil.parseToken(token)).thenReturn(claims);
        when(jwtUtil.isAccessToken(claims)).thenReturn(true);
        when(jwtUtil.isTokenExpired(claims)).thenReturn(false);
        when(jwtUtil.getMemberId(claims)).thenReturn("member123");
        when(jwtUtil.getEmail(claims)).thenReturn("user@example.com");
        when(jwtUtil.getRole(claims)).thenReturn("USER");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(request).setAttribute("X-User-Id", "member123");
        verify(request).setAttribute("X-User-Type", "authenticated");
        verify(request).setAttribute("X-Has-Valid-Token", "true");
    }

    @Test
    void testOptionalAuthEndpoint_WithoutToken_ShouldTreatAsGuest() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/cart");
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(request).setAttribute(eq("X-User-Id"), startsWith("guest-"));
        verify(request).setAttribute("X-User-Role", "GUEST");
        verify(request).setAttribute("X-User-Type", "guest");
        verify(request).setAttribute("X-Has-Valid-Token", "false");
    }

    @Test
    void testOptionalAuthEndpoint_WithDeniedToken_ShouldTreatAsGuest() throws Exception {
        // Given
        String token = "valid.but.denied.token";
        when(request.getRequestURI()).thenReturn("/api/v1/cart");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        Claims claims = Jwts.claims()
                .subject("member123")
                .add("email", "user@example.com")
                .add("role", "USER")
                .add("type", "access")
                .expiration(new Date(System.currentTimeMillis() + 900000))
                .build();

        when(jwtUtil.parseToken(token)).thenReturn(claims);
        when(tokenDenylistService.isTokenDenied(token)).thenReturn(true);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(tokenDenylistService).isTokenDenied(token);
        verify(request).setAttribute(eq("X-User-Id"), startsWith("guest-"));
        verify(request).setAttribute("X-User-Role", "GUEST");
        verify(request).setAttribute("X-User-Type", "guest");
    }
}

