package com.blibli.gdn.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenDenylistService
 */
@ExtendWith(MockitoExtension.class)
class TokenDenylistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenDenylistService tokenDenylistService;

    private static final String TEST_TOKEN = "test.jwt.token";
    private static final String DENYLIST_KEY = "token:denied:" + TEST_TOKEN;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testDenyToken() {
        // Given
        long expirationSeconds = 900L;

        // When
        tokenDenylistService.denyToken(TEST_TOKEN, expirationSeconds);

        // Then
        verify(valueOperations).set(DENYLIST_KEY, "denied", expirationSeconds, TimeUnit.SECONDS);
    }

    @Test
    void testIsTokenDenied_True() {
        // Given
        when(redisTemplate.hasKey(DENYLIST_KEY)).thenReturn(true);

        // When
        boolean isDenied = tokenDenylistService.isTokenDenied(TEST_TOKEN);

        // Then
        assertTrue(isDenied);
        verify(redisTemplate).hasKey(DENYLIST_KEY);
    }

    @Test
    void testIsTokenDenied_False() {
        // Given
        when(redisTemplate.hasKey(DENYLIST_KEY)).thenReturn(false);

        // When
        boolean isDenied = tokenDenylistService.isTokenDenied(TEST_TOKEN);

        // Then
        assertFalse(isDenied);
        verify(redisTemplate).hasKey(DENYLIST_KEY);
    }

    @Test
    void testRemoveFromDenylist() {
        // When
        tokenDenylistService.removeFromDenylist(TEST_TOKEN);

        // Then
        verify(redisTemplate).delete(DENYLIST_KEY);
    }
}
