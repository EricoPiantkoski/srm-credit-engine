package com.srm.creditengine.auth.domain;

import java.time.Instant;
import java.util.Objects;

public record RefreshToken(Long id, String tokenHash, Long usuarioId, Instant expiresAt, boolean revoked) {

    public RefreshToken {
        Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        Objects.requireNonNull(usuarioId, "usuarioId must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (tokenHash.isBlank()) {
            throw new IllegalArgumentException("tokenHash must not be blank");
        }
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public RefreshToken revogar() {
        return new RefreshToken(id, tokenHash, usuarioId, expiresAt, true);
    }
}
