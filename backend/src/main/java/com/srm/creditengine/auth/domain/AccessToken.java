package com.srm.creditengine.auth.domain;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {

    public AccessToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }
    }
}
