package com.lvatong.lft.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "lvatong-test-secret-key-must-be-at-least-32-characters";
    private static final long ACCESS_EXPIRY  = 86_400_000L;
    private static final long REFRESH_EXPIRY = 604_800_000L;

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
    }

    @Test
    void generateAccessToken_shouldBeValidAndContainCorrectClaims() {
        String token = provider.generateAccessToken(42L, "testUser", "USER");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("testUser");
        assertThat(provider.getRoleFromToken(token)).isEqualTo("USER");
    }

    @Test
    void generateRefreshToken_shouldBeMarkedAsRefreshType() {
        String token = provider.generateRefreshToken(42L);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.isRefreshToken(token)).isTrue();
        assertThat(provider.getUserIdFromToken(token)).isEqualTo(42L);
    }

    @Test
    void accessToken_shouldNotBeRefreshType() {
        String token = provider.generateAccessToken(1L, "user", "USER");
        assertThat(provider.isRefreshToken(token)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_forGarbageInput() {
        assertThat(provider.validateToken("not.a.jwt.token")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
        assertThat(provider.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_forTamperedToken() {
        String token = provider.generateAccessToken(1L, "user", "USER");
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "tampered_signature";
        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_forExpiredToken() {
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 1L, 1L);
        String token = shortLived.generateAccessToken(1L, "user", "USER");

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(shortLived.validateToken(token)).isFalse();
    }
}
