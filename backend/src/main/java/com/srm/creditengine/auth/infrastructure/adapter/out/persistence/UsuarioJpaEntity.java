package com.srm.creditengine.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "usuario")
public class UsuarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Column(name = "deve_trocar_senha", nullable = false)
    private boolean deveTrocarSenha;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UsuarioJpaEntity() {}

    public UsuarioJpaEntity(String username, String passwordHash, String role, boolean deveTrocarSenha,
                            Instant createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.deveTrocarSenha = deveTrocarSenha;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public boolean isDeveTrocarSenha() {
        return deveTrocarSenha;
    }
}
