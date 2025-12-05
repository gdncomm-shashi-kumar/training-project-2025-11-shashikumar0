package com.blibli.gdn.gateway.filter;

import com.blibli.gdn.gateway.model.GatewayErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${rate-limit.default-limit:300}")
    private int defaultLimit;

    @Value("${rate-limit.per-user:true}")
    private boolean perUser;

    @Value("${rate-limit.per-ip:false}")
    private boolean perIp;

    private static final long WINDOW_SIZE_SECONDS = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String traceId = (String) request.getAttribute("traceId");

        String rateLimitKey = getRateLimitKey(request);

        try {
            long currentCount = incrementAndGetCount(rateLimitKey);
            long remaining = Math.max(0, defaultLimit - currentCount);

            response.setHeader("X-RateLimit-Limit", String.valueOf(defaultLimit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + WINDOW_SIZE_SECONDS));

            if (currentCount > defaultLimit) {
                log.warn("Rate limit exceeded for key: {} (count: {})", rateLimitKey, currentCount);
                sendRateLimitExceededResponse(response, path, traceId);
                return;
            }

            log.debug("Rate limit check passed for key: {} (count: {}/{})", rateLimitKey, currentCount, defaultLimit);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Rate limit check failed: {}", e.getMessage(), e);
            filterChain.doFilter(request, response);
        }
    }


    private String getRateLimitKey(HttpServletRequest request) {
        StringBuilder key = new StringBuilder("rate_limit:");

        if (perUser) {
            String userId = (String) request.getAttribute("X-User-Id");
            if (userId != null) {
                key.append("user:").append(userId);
            } else {
                key.append("ip:").append(getClientIp(request));
            }
        } else if (perIp) {
            key.append("ip:").append(getClientIp(request));
        } else {
            key.append("global");
        }

        long currentWindow = System.currentTimeMillis() / 1000 / WINDOW_SIZE_SECONDS;
        key.append(":").append(currentWindow);

        return key.toString();
    }


    private long incrementAndGetCount(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            count = 0L;
        }

        if (count == 1) {
            redisTemplate.expire(key, WINDOW_SIZE_SECONDS, TimeUnit.SECONDS);
        }

        return count;
    }


    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }


    private void sendRateLimitExceededResponse(HttpServletResponse response, String path, String traceId)
            throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> details = new HashMap<>();
        details.put("limit", defaultLimit + "/min");

        GatewayErrorResponse errorResponse = GatewayErrorResponse.of(
                429,
                "Too Many Requests",
                "Rate limit exceeded",
                path,
                traceId,
                details
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
