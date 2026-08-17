package com.srm.creditengine.auth.domain;

import java.time.Instant;

public record TokenPair(AccessToken accessToken, String refreshToken, Instant refreshTokenExpiresAt) {

    public TokenPair {
        if (accessToken == null) {
            throw new IllegalArgumentException("accessToken must not be null");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken must not be blank");
        }
        if (refreshTokenExpiresAt == null) {
            throw new IllegalArgumentException("refreshTokenExpiresAt must not be null");
        }
    }
}
