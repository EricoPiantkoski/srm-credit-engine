package com.srm.creditengine.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AccessTokenTest {

    @Test
    void rejectsBlankValue() {
        assertThatThrownBy(() -> new AccessToken(" ", Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullExpiry() {
        assertThatThrownBy(() -> new AccessToken("token", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void holdsValueAndExpiry() {
        Instant expiry = Instant.parse("2026-08-16T12:15:00Z");
        AccessToken token = new AccessToken("token", expiry);
        assertThat(token.value()).isEqualTo("token");
        assertThat(token.expiresAt()).isEqualTo(expiry);
    }
}

class TokenPairTest {

    @Test
    void rejectsBlankRefreshToken() {
        AccessToken access = new AccessToken("token", Instant.now());
        assertThatThrownBy(() -> new TokenPair(access, " ", Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAccessToken() {
        assertThatThrownBy(() -> new TokenPair(null, "refresh", Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void holdsFields() {
        Instant now = Instant.now();
        AccessToken access = new AccessToken("token", now.plusSeconds(900));
        TokenPair pair = new TokenPair(access, "refresh", now.plusSeconds(3600));
        assertThat(pair.accessToken()).isEqualTo(access);
        assertThat(pair.refreshToken()).isEqualTo("refresh");
        assertThat(pair.refreshTokenExpiresAt()).isEqualTo(now.plusSeconds(3600));
    }
}