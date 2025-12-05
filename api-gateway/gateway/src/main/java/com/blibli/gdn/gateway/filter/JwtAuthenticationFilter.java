package com.blibli.gdn.gateway.filter;

import com.blibli.gdn.gateway.config.PublicEndpointsConfig;
import com.blibli.gdn.gateway.model.*;
import com.blibli.gdn.gateway.service.TokenDenylistService;
import com.blibli.gdn.gateway.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final PublicEndpointsConfig publicEndpointsConfig;
    private final TokenDenylistService tokenDenylistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String traceId = UUID.randomUUID().toString();

        request.setAttribute("traceId", traceId);

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path) && !isOptionalAuthEndpoint(path)) {
            log.debug("Public endpoint accessed: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // Handle optional auth endpoints (e.g., cart APIs)
        if (isOptionalAuthEndpoint(path)) {
            handleOptionalAuth(request, response, filterChain, path, traceId);
            return;
        }

        // Extract token from Authorization header for protected endpoints
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            sendErrorResponse(response, 401, "Unauthorized", "Missing or invalid token", path, traceId);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // Parse and validate token
            Claims claims = jwtUtil.parseToken(token);

            // Check if token is in denylist (user logged out)
            if (tokenDenylistService.isTokenDenied(token)) {
                log.warn("Token is in denylist (user logged out): {}", path);
                sendErrorResponse(response, 401, "Unauthorized", "Token has been revoked", path, traceId);
                return;
            }

            // Validate token type (must be access token)
            if (!jwtUtil.isAccessToken(claims)) {
                log.warn("Invalid token type for path: {}", path);
                sendErrorResponse(response, 401, "Unauthorized", "Invalid token type", path, traceId);
                return;
            }

            // Check if token is expired
            if (jwtUtil.isTokenExpired(claims)) {
                log.warn("Expired token for path: {}", path);
                sendErrorResponse(response, 401, "Unauthorized", "Token has expired", path, traceId);
                return;
            }

            // Extract user information and add to request headers
            String memberId = jwtUtil.getMemberId(claims);
            String email = jwtUtil.getEmail(claims);
            String role = jwtUtil.getRole(claims);

            // Add user context headers for downstream services
            request.setAttribute("X-User-Id", memberId);
            request.setAttribute("X-User-Email", email);
            request.setAttribute("X-User-Role", role);

            log.debug("Authenticated user: {} ({})", email, memberId);

            // Continue filter chain
            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            sendErrorResponse(response, 401, "Unauthorized", "Token has expired", path, traceId);
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            sendErrorResponse(response, 401, "Unauthorized", "Malformed token", path, traceId);
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            sendErrorResponse(response, 401, "Unauthorized", "Invalid token signature", path, traceId);
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage(), e);
            sendErrorResponse(response, 401, "Unauthorized", "Token validation failed", path, traceId);
        }
    }


    private void handleOptionalAuth(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain, String path, String traceId) 
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        // If token is provided, try to validate it
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                Claims claims = jwtUtil.parseToken(token);
                
                // Check if token is in denylist (user logged out)
                if (tokenDenylistService.isTokenDenied(token)) {
                    log.debug("Token is in denylist for optional auth endpoint, treating as guest: {}", path);
                    setGuestContext(request);
                } 
                // Validate token type and expiration
                else if (jwtUtil.isAccessToken(claims) && !jwtUtil.isTokenExpired(claims)) {
                    // Extract user information
                    String memberId = jwtUtil.getMemberId(claims);
                    String email = jwtUtil.getEmail(claims);
                    String role = jwtUtil.getRole(claims);
                    
                    // Add user context headers
                    request.setAttribute("X-User-Id", memberId);
                    request.setAttribute("X-User-Email", email);
                    request.setAttribute("X-User-Role", role);
                    request.setAttribute("X-User-Type", "authenticated");
                    request.setAttribute("X-Has-Valid-Token", "true");
                    
                    log.debug("Authenticated user for optional auth endpoint: {} ({})", email, memberId);
                } else {
                    // Invalid or expired token, treat as guest
                    setGuestContext(request);
                    log.debug("Invalid/expired token for optional auth endpoint, treating as guest: {}", path);
                }
            } catch (Exception e) {
                // Token validation failed, treat as guest
                setGuestContext(request);
                log.debug("Token validation failed for optional auth endpoint, treating as guest: {}", path);
            }
        } else {
            // No token provided, treat as guest
            setGuestContext(request);
            log.debug("No token provided for optional auth endpoint, treating as guest: {}", path);
        }
        
        // Continue with the request (either authenticated or guest)
        filterChain.doFilter(request, response);
    }


    private void setGuestContext(HttpServletRequest request) {
        String guestId = "guest-" + UUID.randomUUID().toString();
        request.setAttribute("X-User-Id", guestId);
        request.setAttribute("X-User-Email", "");
        request.setAttribute("X-User-Role", "GUEST");
        request.setAttribute("X-User-Type", "guest");
        request.setAttribute("X-Has-Valid-Token", "false");
    }


    private boolean isPublicEndpoint(String path) {
        return publicEndpointsConfig.getPublicEndpoints().stream()
                .anyMatch(endpoint -> {
                    if (endpoint.endsWith("/**")) {
                        String prefix = endpoint.substring(0, endpoint.length() - 3);
                        return path.startsWith(prefix);
                    }
                    return path.equals(endpoint);
                });
    }


    private boolean isOptionalAuthEndpoint(String path) {
        return publicEndpointsConfig.getOptionalAuthEndpoints().stream()
                .anyMatch(endpoint -> {
                    if (endpoint.endsWith("/**")) {
                        String prefix = endpoint.substring(0, endpoint.length() - 3);
                        return path.startsWith(prefix);
                    }
                    return path.equals(endpoint);
                });
    }


    private void sendErrorResponse(HttpServletResponse response, int status, String error, String message,
                                    String path, String traceId) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        GatewayErrorResponse errorResponse = GatewayErrorResponse.of(status, error, message, path, traceId);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
