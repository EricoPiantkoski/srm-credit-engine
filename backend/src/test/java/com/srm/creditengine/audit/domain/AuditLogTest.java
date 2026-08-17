package com.srm.creditengine.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditLogTest {

    @Test
    void novoCreatesLogWithoutIdAndWithTimestamp() {
        AuditLog log = AuditLog.novo(
            "admin", "LOGIN", "/api/auth/login", ResultadoAuditoria.SUCESSO, null, "req-1");

        assertThat(log.id()).isNull();
        assertThat(log.username()).isEqualTo("admin");
        assertThat(log.acao()).isEqualTo("LOGIN");
        assertThat(log.recurso()).isEqualTo("/api/auth/login");
        assertThat(log.resultado()).isEqualTo(ResultadoAuditoria.SUCESSO);
        assertThat(log.createdAt()).isNotNull();
    }

    @Test
    void rejectsBlankAcao() {
        assertThatThrownBy(() -> new AuditLog(
            null, "admin", " ", "/api", ResultadoAuditoria.SUCESSO, null, null, Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankRecurso() {
        assertThatThrownBy(() -> new AuditLog(
            null, "admin", "LOGIN", " ", ResultadoAuditoria.SUCESSO, null, null, Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullResultado() {
        assertThatThrownBy(() -> new AuditLog(
            null, "admin", "LOGIN", "/api", null, null, null, Instant.now()))
            .isInstanceOf(NullPointerException.class);
    }
}