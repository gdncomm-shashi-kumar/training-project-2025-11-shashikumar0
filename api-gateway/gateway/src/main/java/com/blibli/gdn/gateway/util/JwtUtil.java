package com.blibli.gdn.gateway.util;

import com.blibli.gdn.gateway.config.JwtConfig;
import com.blibli.gdn.gateway.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final JwtConfig jwtConfig;


    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw e;
        }
    }


    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }


    public String getMemberId(Claims claims) {
        return claims.getSubject();
    }


    public String getEmail(Claims claims) {
        return claims.get("email", String.class);
    }


    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }


    public String getTokenType(Claims claims) {
        return claims.get("type", String.class);
    }


    public boolean isAccessToken(Claims claims) {
        String type = getTokenType(claims);
        return "access".equals(type);
    }


    public boolean isRefreshToken(Claims claims) {
        String type = getTokenType(claims);
        return "refresh".equals(type);
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = parseToken(token);
            return isAccessToken(claims) && !isTokenExpired(claims);
        } catch (Exception e) {
            log.error("Access token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String generateAccessToken(UUID memberId, String email, Role role) {
        log.debug("Generating access token for member: {}", memberId);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", memberId.toString());
        claims.put("email", email);
        claims.put("role", role.name());
        claims.put("type", "access");

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }


    public String generateRefreshToken(UUID memberId) {
        log.debug("Generating refresh token for member: {}", memberId);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", memberId.toString());
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }


    public Long getAccessTokenExpirationInSeconds() {
        return jwtConfig.getAccessTokenExpiration() / 1000;
    }


    public UUID extractMemberId(String token) {
        Claims claims = parseToken(token);
        String subject = claims.getSubject();
        return UUID.fromString(subject);
    }
}
