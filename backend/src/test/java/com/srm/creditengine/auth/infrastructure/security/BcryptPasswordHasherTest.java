package com.srm.creditengine.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher();

    @Test
    void encodeAndMatchesRoundTrip() {
        String encoded = hasher.encode("admin123");
        assertThat(encoded).startsWith("$2");
        assertThat(hasher.matches("admin123", encoded)).isTrue();
    }

    @Test
    void matchesRejectsWrongPassword() {
        String encoded = hasher.encode("admin123");
        assertThat(hasher.matches("wrong", encoded)).isFalse();
    }
}