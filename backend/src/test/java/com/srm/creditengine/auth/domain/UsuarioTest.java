package com.srm.creditengine.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void novoCreatesUsuarioWithoutId() {
        Usuario usuario = Usuario.novo("admin", "hash", Role.ADMIN, true);
        assertThat(usuario.id()).isNull();
        assertThat(usuario.username()).isEqualTo("admin");
        assertThat(usuario.role()).isEqualTo(Role.ADMIN);
        assertThat(usuario.deveTrocarSenha()).isTrue();
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
        assertThatThrownBy(() -> new Usuario(1L, " ", "hash", Role.ADMIN, false))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankPasswordHash() {
        assertThatThrownBy(() -> new Usuario(1L, "admin", " ", Role.ADMIN, false))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullRole() {
        assertThatThrownBy(() -> new Usuario(1L, "admin", "hash", null, false))
            .isInstanceOf(NullPointerException.class);
    }
}