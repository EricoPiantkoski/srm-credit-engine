CREATE TABLE moeda (
    codigo VARCHAR(3)  PRIMARY KEY,
    nome   VARCHAR(64) NOT NULL,
    escala INT         NOT NULL DEFAULT 2,
    CHECK (codigo = upper(codigo))
);

CREATE TABLE taxa_cambio (
    id             BIGSERIAL      PRIMARY KEY,
    codigo_base    VARCHAR(3)     NOT NULL REFERENCES moeda (codigo),
    codigo_cotacao VARCHAR(3)     NOT NULL REFERENCES moeda (codigo),
    taxa           NUMERIC(19, 8) NOT NULL CHECK (taxa > 0),
    vigencia       TIMESTAMPTZ    NOT NULL,
    CHECK (codigo_base <> codigo_cotacao),
    UNIQUE (codigo_base, codigo_cotacao, vigencia)
);

CREATE TABLE tipo_recebivel (
    codigo VARCHAR(32)   PRIMARY KEY,
    nome   VARCHAR(64)   NOT NULL,
    spread NUMERIC(9, 6) NOT NULL CHECK (spread >= 0)
);

CREATE TABLE recebivel (
    id               BIGSERIAL     PRIMARY KEY,
    referencia_externa VARCHAR(64) NOT NULL UNIQUE,
    codigo_tipo      VARCHAR(32)   NOT NULL REFERENCES tipo_recebivel (codigo),
    valor_face       NUMERIC(19,4) NOT NULL CHECK (valor_face > 0),
    codigo_moeda     VARCHAR(3)    NOT NULL REFERENCES moeda (codigo),
    data_vencimento  DATE          NOT NULL,
    cedente          VARCHAR(128)  NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE liquidacao (
    id               BIGSERIAL   PRIMARY KEY,
    chave_idempotencia VARCHAR(64) NOT NULL UNIQUE,
    status           VARCHAR(16) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE liquidacao_item (
    id                    BIGSERIAL     PRIMARY KEY,
    liquidacao_id         BIGINT        NOT NULL REFERENCES liquidacao (id),
    recebivel_id          BIGINT        NOT NULL REFERENCES recebivel (id),
    valor_presente        NUMERIC(19,4) NOT NULL CHECK (valor_presente >= 0),
    spread_aplicado       NUMERIC(9,6)  NOT NULL CHECK (spread_aplicado >= 0),
    prazo_meses           NUMERIC(9,6)  NOT NULL CHECK (prazo_meses >= 0),
    valor_pagamento       NUMERIC(19,4) NOT NULL CHECK (valor_pagamento >= 0),
    codigo_moeda_pagamento VARCHAR(3)   NOT NULL REFERENCES moeda (codigo),
    taxa_aplicada         NUMERIC(19,8) CHECK (taxa_aplicada IS NULL OR taxa_aplicada > 0),
    UNIQUE (liquidacao_id, recebivel_id)
);

CREATE INDEX idx_taxa_cambio_par_vigencia ON taxa_cambio (codigo_base, codigo_cotacao, vigencia);
CREATE INDEX idx_recebivel_cedente ON recebivel (cedente);
CREATE INDEX idx_liquidacao_item_liquidacao ON liquidacao_item (liquidacao_id);
CREATE INDEX idx_liquidacao_periodo ON liquidacao (created_at);