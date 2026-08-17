package com.srm.creditengine.audit.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "acao", nullable = false, length = 32)
    private String acao;

    @Column(name = "recurso", nullable = false, length = 255)
    private String recurso;

    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;

    @Column(name = "chave_idempotencia", length = 36)
    private String chaveIdempotencia;

    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLogJpaEntity() {}

    public AuditLogJpaEntity(String username, String acao, String recurso, String resultado,
                             String chaveIdempotencia, String requestId, Instant createdAt) {
        this.username = username;
        this.acao = acao;
        this.recurso = recurso;
        this.resultado = resultado;
        this.chaveIdempotencia = chaveIdempotencia;
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAcao() {
        return acao;
    }

    public String getRecurso() {
        return recurso;
    }

    public String getResultado() {
        return resultado;
    }

    public String getChaveIdempotencia() {
        return chaveIdempotencia;
    }

    public String getRequestId() {
        return requestId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}