package com.srm.creditengine.auth.infrastructure.adapter.out.persistence;

import com.srm.creditengine.auth.domain.Usuario;
import com.srm.creditengine.auth.domain.UsuarioRepository;
import com.srm.creditengine.auth.domain.Role;
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

    private Usuario toDomain(UsuarioJpaEntity entity) {
        return new Usuario(entity.getId(), entity.getUsername(), entity.getPasswordHash(),
            Role.valueOf(entity.getRole()), entity.isDeveTrocarSenha());
    }
}
