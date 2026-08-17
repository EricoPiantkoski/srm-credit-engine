package com.srm.creditengine.audit.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.audit.domain.AuditLog;
import com.srm.creditengine.audit.domain.ResultadoAuditoria;
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
class AuditLogRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuditLogRepositoryAdapter adapter;

    @Autowired
    private AuditLogJpaRepository jpaRepository;

    @BeforeEach
    void clean() {
        jpaRepository.deleteAll();
    }

    @Test
    void persistsAuditLogRow() {
        adapter.registrar(AuditLog.novo(
            "admin", "LOGIN", "/api/auth/login", ResultadoAuditoria.SUCESSO, null, "req-1"));

        assertThat(jpaRepository.count()).isEqualTo(1);
        AuditLogJpaEntity saved = jpaRepository.findAll().get(0);
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getAcao()).isEqualTo("LOGIN");
        assertThat(saved.getRecurso()).isEqualTo("/api/auth/login");
        assertThat(saved.getResultado()).isEqualTo("SUCESSO");
        assertThat(saved.getRequestId()).isEqualTo("req-1");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void persistsIdempotencyKey() {
        adapter.registrar(AuditLog.novo(
            "admin", "LIQUIDAR_LOTE", "/api/liquidacoes", ResultadoAuditoria.SUCESSO,
            "11111111-1111-1111-1111-111111111111", "req-2"));

        AuditLogJpaEntity saved = jpaRepository.findAll().get(0);
        assertThat(saved.getChaveIdempotencia()).isEqualTo("11111111-1111-1111-1111-111111111111");
    }
}