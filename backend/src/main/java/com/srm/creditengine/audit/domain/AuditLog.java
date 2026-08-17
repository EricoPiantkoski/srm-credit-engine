package com.srm.creditengine.audit.domain;

import java.time.Instant;
import java.util.Objects;

public record AuditLog(Long id, String username, String acao, String recurso,
                       ResultadoAuditoria resultado, String chaveIdempotencia,
                       String requestId, Instant createdAt) {

    public AuditLog {
        Objects.requireNonNull(acao, "acao must not be null");
        Objects.requireNonNull(recurso, "recurso must not be null");
        Objects.requireNonNull(resultado, "resultado must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (acao.isBlank()) {
            throw new IllegalArgumentException("acao must not be blank");
        }
        if (recurso.isBlank()) {
            throw new IllegalArgumentException("recurso must not be blank");
        }
    }

    public static AuditLog novo(String username, String acao, String recurso, ResultadoAuditoria resultado,
                                String chaveIdempotencia, String requestId) {
        return new AuditLog(null, username, acao, recurso, resultado, chaveIdempotencia, requestId, Instant.now());
    }
}