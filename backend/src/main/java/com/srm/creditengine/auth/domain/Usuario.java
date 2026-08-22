package com.srm.creditengine.auth.domain;

import java.time.Instant;
import java.util.Objects;

public record Usuario(Long id, String username, String passwordHash, Role role, boolean deveTrocarSenha,
                      int failedLoginAttempts, Instant lockedUntil) {

    public Usuario {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(role, "role must not be null");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
    }

    public static Usuario novo(String username, String passwordHash, Role role, boolean deveTrocarSenha) {
        return new Usuario(null, username, passwordHash, role, deveTrocarSenha, 0, null);
    }

    public Usuario withId(Long id) {
        return new Usuario(id, username, passwordHash, role, deveTrocarSenha, failedLoginAttempts, lockedUntil);
    }

    public Usuario comSenhaTrocada(String novoHash) {
        return new Usuario(id, username, novoHash, role, false, failedLoginAttempts, lockedUntil);
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public Usuario incrementFailedAttempts(Instant now) {
        int newAttempts = failedLoginAttempts + 1;
        Instant newLockedUntil = newAttempts >= 5 ? now.plusSeconds(900) : null;
        return new Usuario(id, username, passwordHash, role, deveTrocarSenha, newAttempts, newLockedUntil);
    }

    public Usuario resetFailedAttempts() {
        return new Usuario(id, username, passwordHash, role, deveTrocarSenha, 0, null);
    }
}
