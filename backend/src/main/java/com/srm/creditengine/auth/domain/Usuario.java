package com.srm.creditengine.auth.domain;

import java.util.Objects;

public record Usuario(Long id, String username, String passwordHash, Role role, boolean deveTrocarSenha) {

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
        return new Usuario(null, username, passwordHash, role, deveTrocarSenha);
    }

    public Usuario withId(Long id) {
        return new Usuario(id, username, passwordHash, role, deveTrocarSenha);
    }

    public Usuario comSenhaTrocada(String novoHash) {
        return new Usuario(id, username, novoHash, role, false);
    }
}
