package com.srm.creditengine.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.auth.domain.AccessToken;
import com.srm.creditengine.auth.domain.Role;
import com.srm.creditengine.auth.domain.Usuario;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256-signing-1234";

    private final JwtTokenProvider provider =
        new JwtTokenProvider(SECRET, Duration.ofMinutes(15), Duration.ofDays(7), "SRM-CREDIT-ENGINE");

    @Test
    void issueAccessTokenProducesSignedJwtWithClaims() {
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false);
        AccessToken token = provider.issueAccessToken(usuario);

        assertThat(token.value()).isNotBlank();
        assertThat(token.value().split("\\.")).hasSize(3);
        assertThat(token.expiresAt()).isAfter(Instant.now());
        assertThat(ChronoUnit.SECONDS.between(Instant.now(), token.expiresAt())).isBetween(14 * 60L, 16 * 60L);
    }

    @Test
    void hashRefreshTokenIsStableAndUnique() {
        String h1 = provider.hashRefreshToken("raw-token");
        String h2 = provider.hashRefreshToken("raw-token");
        String h3 = provider.hashRefreshToken("other-token");

        assertThat(h1).isEqualTo(h2);
        assertThat(h1).hasSize(64);
        assertThat(h1).isNotEqualTo(h3);
    }

    @Test
    void generateRefreshTokenProducesDistinctValues() {
        assertThat(provider.generateRefreshToken()).isNotEqualTo(provider.generateRefreshToken());
    }

    @Test
    void refreshTokenExpiryIsInFuture() {
        assertThat(provider.refreshTokenExpiry()).isAfter(Instant.now());
    }
}