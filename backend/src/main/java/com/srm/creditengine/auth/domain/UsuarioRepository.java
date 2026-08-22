package com.srm.creditengine.auth.domain;

import java.util.Optional;

public interface UsuarioRepository {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findById(Long id);

    Usuario save(Usuario usuario);
}
