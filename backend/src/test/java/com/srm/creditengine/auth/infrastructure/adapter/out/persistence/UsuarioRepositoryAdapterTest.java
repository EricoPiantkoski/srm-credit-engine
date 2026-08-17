package com.srm.creditengine.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.auth.domain.Role;
import com.srm.creditengine.auth.domain.Usuario;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UsuarioRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UsuarioRepositoryAdapter adapter;

    @Test
    void findsSeededAdminByUsername() {
        Optional<Usuario> usuario = adapter.findByUsername("admin");

        assertThat(usuario).isPresent();
        assertThat(usuario.get().role()).isEqualTo(Role.ADMIN);
        assertThat(usuario.get().passwordHash()).isNotBlank();
        assertThat(usuario.get().deveTrocarSenha()).isTrue();
    }

    @Test
    void findsSeededAdminById() {
        Usuario admin = adapter.findByUsername("admin").orElseThrow();
        Optional<Usuario> found = adapter.findById(admin.id());

        assertThat(found).isPresent();
        assertThat(found.get().username()).isEqualTo("admin");
    }

    @Test
    void findByUsernameReturnsEmptyForUnknown() {
        assertThat(adapter.findByUsername("nobody")).isEmpty();
    }
}