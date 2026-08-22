package com.srm.creditengine.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void novoCreatesUsuarioWithoutId() {
        Usuario usuario = Usuario.novo("admin", "hash", Role.ADMIN, true);
        assertThat(usuario.id()).isNull();
        assertThat(usuario.username()).isEqualTo("admin");
        assertThat(usuario.role()).isEqualTo(Role.ADMIN);
        assertThat(usuario.deveTrocarSenha()).isTrue();
        assertThat(usuario.failedLoginAttempts()).isEqualTo(0);
        assertThat(usuario.lockedUntil()).isNull();
    }

    @Test
    void withIdAssignsId() {
        Usuario usuario = Usuario.novo("admin", "hash", Role.ADMIN, false).withId(7L);
        assertThat(usuario.id()).isEqualTo(7L);
    }

    @Test
    void comSenhaTrocadaClearsFlag() {
        Usuario usuario = Usuario.novo("admin", "old-hash", Role.ADMIN, true).comSenhaTrocada("new-hash");
        assertThat(usuario.passwordHash()).isEqualTo("new-hash");
        assertThat(usuario.deveTrocarSenha()).isFalse();
    }

    @Test
    void rejectsBlankUsername() {
        assertThatThrownBy(() -> new Usuario(1L, " ", "hash", Role.ADMIN, false, 0, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankPasswordHash() {
        assertThatThrownBy(() -> new Usuario(1L, "admin", " ", Role.ADMIN, false, 0, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullRole() {
        assertThatThrownBy(() -> new Usuario(1L, "admin", "hash", null, false, 0, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void isLockedReturnsFalseWhenNotLocked() {
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false, 0, null);
        assertThat(usuario.isLocked(java.time.Instant.now())).isFalse();
    }

    @Test
    void isLockedReturnsTrueWhenLocked() {
        Instant now = java.time.Instant.now();
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false, 5, now.plusSeconds(900));
        assertThat(usuario.isLocked(now)).isTrue();
    }

    @Test
    void isLockedReturnsFalseWhenLockExpired() {
        Instant now = java.time.Instant.now();
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false, 5, now.minusSeconds(1));
        assertThat(usuario.isLocked(now)).isFalse();
    }

    @Test
    void incrementFailedAttemptsIncrementsCounter() {
        Instant now = java.time.Instant.now();
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false, 0, null);
        Usuario updated = usuario.incrementFailedAttempts(now);
        assertThat(updated.failedLoginAttempts()).isEqualTo(1);
        assertThat(updated.lockedUntil()).isNull();
    }

    @Test
    void incrementFailedAttemptsLocksAfterFive() {
        Instant now = java.time.Instant.now();
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false, 4, null);
        Usuario updated = usuario.incrementFailedAttempts(now);
        assertThat(updated.failedLoginAttempts()).isEqualTo(5);
        assertThat(updated.lockedUntil()).isNotNull();
        assertThat(updated.lockedUntil()).isAfter(now);
    }

    @Test
    void resetFailedAttemptsClearsCounter() {
        Instant now = java.time.Instant.now();
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false, 3, now.plusSeconds(900));
        Usuario updated = usuario.resetFailedAttempts();
        assertThat(updated.failedLoginAttempts()).isEqualTo(0);
        assertThat(updated.lockedUntil()).isNull();
    }
}