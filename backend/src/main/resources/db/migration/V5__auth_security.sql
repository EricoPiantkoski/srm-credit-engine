CREATE TABLE usuario (
    id               BIGSERIAL    PRIMARY KEY,
    username         VARCHAR(64)  NOT NULL UNIQUE,
    password_hash    VARCHAR(128) NOT NULL,
    role             VARCHAR(32)  NOT NULL DEFAULT 'ADMIN',
    deve_trocar_senha BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE refresh_token (
    id          BIGSERIAL    PRIMARY KEY,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    usuario_id  BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_token_usuario ON refresh_token (usuario_id);
CREATE INDEX idx_refresh_token_expires ON refresh_token (expires_at);

INSERT INTO usuario (username, password_hash, role, deve_trocar_senha)
VALUES ('admin',
        '$2b$10$7JpHZIfcM82aicMGdwkTneC0QujiOR6agCpaShM9zxkuTs1NvdItS',
        'ADMIN',
        TRUE);
