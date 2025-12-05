package com.blibli.gdn.gateway.util;

import com.blibli.gdn.gateway.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JwtUtil
 */
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-validation-must-be-256-bits-long";
    private static final String TEST_MEMBER_ID = "member-123";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_ROLE = "USER";

    @BeforeEach
    void setUp() {
        lenient().when(jwtConfig.getSecret()).thenReturn(TEST_SECRET);
        lenient().when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L); // 15 minutes
    }

    @Test
    void testParseValidAccessToken() {
        // Given
        String token = createTestToken("access", 900000L);

        // When
        Claims claims = jwtUtil.parseToken(token);

        // Then
        assertNotNull(claims);
        assertEquals(TEST_MEMBER_ID, claims.getSubject());
        assertEquals(TEST_EMAIL, claims.get("email", String.class));
        assertEquals(TEST_ROLE, claims.get("role", String.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void testParseExpiredToken() {
        // Given - token expired 1 hour ago
        String token = createTestToken("access", -3600000L);

        // When & Then
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.parseToken(token));
    }

    @Test
    void testValidateAccessToken_Valid() {
        // Given
        String token = createTestToken("access", 900000L);

        // When
        boolean isValid = jwtUtil.validateAccessToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateAccessToken_Expired() {
        // Given
        String token = createTestToken("access", -3600000L);

        // When
        boolean isValid = jwtUtil.validateAccessToken(token);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateAccessToken_WrongType() {
        // Given
        String token = createTestToken("refresh", 900000L);

        // When
        boolean isValid = jwtUtil.validateAccessToken(token);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testGetMemberId() {
        // Given
        String token = createTestToken("access", 900000L);
        Claims claims = jwtUtil.parseToken(token);

        // When
        String memberId = jwtUtil.getMemberId(claims);

        // Then
        assertEquals(TEST_MEMBER_ID, memberId);
    }

    @Test
    void testGetEmail() {
        // Given
        String token = createTestToken("access", 900000L);
        Claims claims = jwtUtil.parseToken(token);

        // When
        String email = jwtUtil.getEmail(claims);

        // Then
        assertEquals(TEST_EMAIL, email);
    }

    @Test
    void testGetRole() {
        // Given
        String token = createTestToken("access", 900000L);
        Claims claims = jwtUtil.parseToken(token);

        // When
        String role = jwtUtil.getRole(claims);

        // Then
        assertEquals(TEST_ROLE, role);
    }

    @Test
    void testIsAccessToken() {
        // Given
        String token = createTestToken("access", 900000L);
        Claims claims = jwtUtil.parseToken(token);

        // When
        boolean isAccessToken = jwtUtil.isAccessToken(claims);

        // Then
        assertTrue(isAccessToken);
    }

    @Test
    void testIsRefreshToken() {
        // Given
        String token = createTestToken("refresh", 900000L);
        Claims claims = jwtUtil.parseToken(token);

        // When
        boolean isRefreshToken = jwtUtil.isRefreshToken(claims);

        // Then
        assertTrue(isRefreshToken);
    }

    /**
     * Helper method to create test JWT tokens
     */
    private String createTestToken(String type, long expirationOffset) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationOffset);

        return Jwts.builder()
                .subject(TEST_MEMBER_ID)
                .claim("email", TEST_EMAIL)
                .claim("role", TEST_ROLE)
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }
}
