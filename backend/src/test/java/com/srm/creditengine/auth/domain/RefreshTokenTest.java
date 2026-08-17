package com.srm.creditengine.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    @Test
    void isExpiredChecksExpiry() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        RefreshToken valid = new RefreshToken(1L, "hash", 1L, now.plusSeconds(60), false);
        RefreshToken expired = new RefreshToken(2L, "hash2", 1L, now.minusSeconds(1), false);

        assertThat(valid.isExpired(now)).isFalse();
        assertThat(expired.isExpired(now)).isTrue();
    }

    @Test
    void revogarMarksRevoked() {
        RefreshToken token = new RefreshToken(1L, "hash", 1L, Instant.parse("2026-08-23T12:00:00Z"), false);
        RefreshToken revoked = token.revogar();
        assertThat(revoked.revoked()).isTrue();
        assertThat(revoked.id()).isEqualTo(token.id());
        assertThat(revoked.tokenHash()).isEqualTo(token.tokenHash());
    }

    @Test
    void rejectsBlankTokenHash() {
        assertThatThrownBy(() -> new RefreshToken(1L, " ", 1L, Instant.parse("2026-08-23T12:00:00Z"), false))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullExpiry() {
        assertThatThrownBy(() -> new RefreshToken(1L, "hash", 1L, null, false))
            .isInstanceOf(NullPointerException.class);
    }
}