package com.srm.creditengine.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.auth.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
class RefreshTokenRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RefreshTokenRepositoryAdapter adapter;

    @Autowired
    private RefreshTokenJpaRepository jpaRepository;

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    @BeforeEach
    void clean() {
        jpaRepository.deleteAll();
    }

    @Test
    void savesAndFindsByTokenHash() {
        Long usuarioId = usuarioJpaRepository.findAll().get(0).getId();

        RefreshToken saved = adapter.save(new RefreshToken(
            null, "hashed-token", usuarioId, Instant.parse("2026-08-23T12:00:00Z"), false));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.tokenHash()).isEqualTo("hashed-token");
        assertThat(saved.revoked()).isFalse();

        Optional<RefreshToken> found = adapter.findByTokenHash("hashed-token");
        assertThat(found).isPresent();
        assertThat(found.get().usuarioId()).isEqualTo(usuarioId);
    }

    @Test
    void updatingExistingTokenRevokesIt() {
        Long usuarioId = usuarioJpaRepository.findAll().get(0).getId();
        RefreshToken saved = adapter.save(new RefreshToken(
            null, "hashed-token", usuarioId, Instant.parse("2026-08-23T12:00:00Z"), false));

        RefreshToken revoked = adapter.save(saved.revogar());

        assertThat(revoked.revoked()).isTrue();
        Optional<RefreshToken> found = adapter.findByTokenHash("hashed-token");
        assertThat(found).isPresent();
        assertThat(found.get().revoked()).isTrue();
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    void findByTokenHashReturnsEmptyForUnknown() {
        assertThat(adapter.findByTokenHash("unknown")).isEmpty();
    }
}