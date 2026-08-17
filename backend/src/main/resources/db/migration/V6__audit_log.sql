CREATE TABLE audit_log (
    id                 BIGSERIAL    PRIMARY KEY,
    username           VARCHAR(100),
    acao               VARCHAR(32)  NOT NULL,
    recurso            VARCHAR(255) NOT NULL,
    resultado          VARCHAR(20)  NOT NULL,
    chave_idempotencia VARCHAR(36),
    request_id         VARCHAR(36),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX idx_audit_log_username ON audit_log (username);