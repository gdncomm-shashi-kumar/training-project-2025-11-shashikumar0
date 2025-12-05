package com.blibli.gdn.gateway.service;

import com.blibli.gdn.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenDenylistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String DENYLIST_PREFIX = "token:denied:";

    public void denyToken(String token, long expirationSeconds) {
        String key = DENYLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "denied", expirationSeconds, TimeUnit.SECONDS);
        log.info("Token added to denylist with TTL: {} seconds", expirationSeconds);
    }


    public boolean isTokenDenied(String token) {
        String key = DENYLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }


    public void removeFromDenylist(String token) {
        String key = DENYLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Token removed from denylist");
    }


    public void denyBothTokens(String accessToken, String refreshToken) {
        try {
            long accessTTL = getRemainingTTL(accessToken);
            long refreshTTL = getRemainingTTL(refreshToken);

            denyToken(accessToken, accessTTL);
            denyToken(refreshToken, refreshTTL);

            log.info("Both tokens added to denylist - access TTL: {}s, refresh TTL: {}s",
                    accessTTL, refreshTTL);
        } catch (Exception e) {
            log.error("Error adding tokens to denylist: {}", e.getMessage(), e);
            throw e;
        }
    }


    private long getRemainingTTL(String token) {
        try {
            Claims claims = jwtUtil.parseToken(token);
            long expirationTime = claims.getExpiration().getTime();
            long currentTime = System.currentTimeMillis();
            long remainingMs = expirationTime - currentTime;

            return Math.max(1, remainingMs / 1000);
        } catch (Exception e) {
            log.warn("Error calculating TTL for token: {}", e.getMessage());
            return 3600;
        }
    }
}
