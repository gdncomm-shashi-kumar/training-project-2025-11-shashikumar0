package com.blibli.gdn.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(-1)
@Slf4j
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${security.headers.enabled:true}")
    private boolean securityHeadersEnabled;

    @Value("${security.headers.csp:default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'}")
    private String contentSecurityPolicy;

    @Value("${security.max-body-size:10485760}") // 10MB default
    private long maxBodySize;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!securityHeadersEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        long contentLength = request.getContentLengthLong();
        if (contentLength > maxBodySize) {
            log.warn("Request body size {} exceeds maximum allowed size {}", contentLength, maxBodySize);
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\":\"Payload Too Large\",\"message\":\"Request body size exceeds maximum allowed size of %d bytes\"}", 
                maxBodySize
            ));
            return;
        }


        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Prevent clickjacking attacks
        response.setHeader("X-Frame-Options", "DENY");
        
        // Enable XSS protection in browsers
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        if (!"http".equals(request.getScheme())) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }
        
        // Content Security Policy
        response.setHeader("Content-Security-Policy", contentSecurityPolicy);
        
        // Prevent browsers from sending the Referer header
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Control which features and APIs can be used
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        
        // Remove server information
        response.setHeader("X-Powered-By", "");
        response.setHeader("Server", "");
        
        // Cache control for sensitive data
        if (request.getRequestURI().contains("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        log.debug("Security headers added to response for: {}", request.getRequestURI());
        
        filterChain.doFilter(request, response);
    }
}

