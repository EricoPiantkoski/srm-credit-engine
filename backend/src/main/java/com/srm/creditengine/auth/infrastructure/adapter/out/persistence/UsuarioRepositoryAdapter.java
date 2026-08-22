package com.srm.creditengine.auth.infrastructure.adapter.out.persistence;

import com.srm.creditengine.auth.domain.Usuario;
import com.srm.creditengine.auth.domain.UsuarioRepository;
import com.srm.creditengine.auth.domain.Role;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository jpaRepository;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioJpaEntity entity;
        if (usuario.id() != null) {
            entity = jpaRepository.findById(usuario.id()).orElseThrow(() ->
                new IllegalStateException("usuario not found for id: " + usuario.id()));
            entity.setFailedLoginAttempts(usuario.failedLoginAttempts());
            entity.setLockedUntil(usuario.lockedUntil());
        } else {
            entity = new UsuarioJpaEntity(
                usuario.username(), usuario.passwordHash(), usuario.role().name(),
                usuario.deveTrocarSenha(), Instant.now());
            entity.setFailedLoginAttempts(usuario.failedLoginAttempts());
            entity.setLockedUntil(usuario.lockedUntil());
        }
        UsuarioJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private Usuario toDomain(UsuarioJpaEntity entity) {
        return new Usuario(entity.getId(), entity.getUsername(), entity.getPasswordHash(),
            Role.valueOf(entity.getRole()), entity.isDeveTrocarSenha(),
            entity.getFailedLoginAttempts(), entity.getLockedUntil());
    }
}
