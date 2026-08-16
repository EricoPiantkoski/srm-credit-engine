# Modelagem de Dados — SRM Credit Engine

Este documento descreve o modelo de dados do SRM Credit Engine, apresentando o Diagrama ER (Entidade-Relacionamento) e os scripts DDL necessários para a criação da estrutura do banco. O schema é versionado pelo Flyway, cujas migrações residem em `backend/src/main/resources/db/migration`.

## 1. Convenção de Nomenclatura

Os nomes de domínio — tabelas, entidades e colunas de negócio — permanecem em **português**, mesmo com o código em inglês. Campos técnicos (`id`, `version`, `created_at`) permanecem em inglês. A adoção dessa convenção garante que a linguagem ubíqua do negócio (moeda, recebível, liquidação, tipo de recebível) seja reconhecível no modelo persistido e no código de domínio, formando uma mescla deliberada português-inglês.

| Conceito de domínio | Tabela | Colunas de negócio |
| --- | --- | --- |
| Moeda | `moeda` | `codigo`, `nome`, `escala` |
| Taxa de câmbio | `taxa_cambio` | `codigo_base`, `codigo_cotacao`, `taxa`, `vigencia` |
| Tipo de recebível | `tipo_recebivel` | `codigo`, `nome`, `spread` |
| Recebível | `recebivel` | `referencia_externa`, `codigo_tipo`, `valor_face`, `codigo_moeda`, `data_vencimento`, `cedente` |
| Liquidação | `liquidacao` | `chave_idempotencia`, `status` |
| Item de liquidação | `liquidacao_item` | `valor_presente`, `spread_aplicado`, `prazo_meses`, `valor_pagamento`, `codigo_moeda_pagamento`, `taxa_aplicada` |

## 2. Visão Geral do Modelo

O modelo atende aos quatro domínios de negócio do sistema, preservando suas fronteiras:

- **Moedas** (`moeda`) e **Taxas de Câmbio** (`taxa_cambio`) — domínio de Câmbio;
- **Tipos de Recebíveis** (`tipo_recebivel`) — catálogo de produtos com spread por tipo;
- **Recebíveis** (`recebivel`) — ativos a serem precificados e liquidados;
- **Liquidações** (`liquidacao` e `liquidacao_item`) — registro transacional auditável das operações.

A precisão numérica segue a decisão documentada em `docs/architecture_decision_records-db_definition.md`: valores monetários em `NUMERIC` com escala definida, sem ponto flutuante.

## 3. Diagrama ER

```
┌──────────────────┐          ┌──────────────────────────┐
│      moeda       │          │       taxa_cambio        │
│──────────────────│          │──────────────────────────│
│ codigo PK        │◄─────────│ codigo_base  FK          │
│ nome             │          │ codigo_cotacao FK        │
│ escala           │          │ taxa                     │
└──────────────────┘          │ vigencia                 │
                              └──────────────────────────┘

┌──────────────────────┐          ┌─────────────────────────────┐
│   tipo_recebivel     │          │         recebivel           │
│──────────────────────│          │─────────────────────────────│
│ codigo PK            │◄─────────│ id PK                       │
│ nome                 │          │ referencia_externa UNIQUE   │
│ spread               │          │ codigo_tipo FK              │
└──────────────────────┘          │ valor_face                  │
                                  │ codigo_moeda FK ───────────► moeda
                                  │ data_vencimento             │
                                  │ cedente                     │
                                  │ version (locking)           │
                                  └─────────────────────────────┘

┌──────────────────────┐          ┌───────────────────────────────────┐
│     liquidacao       │          │       liquidacao_item            │
│──────────────────────│          │───────────────────────────────────│
│ id PK                │◄─────────│ id PK                            │
│ chave_idempotencia   │          │ liquidacao_id FK                 │
│   UNIQUE             │          │ recebivel_id FK ────────────────► recebivel
│ status               │          │ valor_presente                   │
│ created_at           │          │ spread_aplicado                  │
└──────────────────────┘          │ prazo_meses                      │
                                  │ valor_pagamento                  │
                                  │ codigo_moeda_pagamento FK ──────► moeda
                                  │ taxa_aplicada                    │
                                  └───────────────────────────────────┘
```

### Relacionamentos

| Entidade A | Entidade B | Cardinalidade | Semântica |
| --- | --- | --- | --- |
| `moeda` | `taxa_cambio` | 1 — N | Uma moeda é base ou cotada em várias taxas |
| `tipo_recebivel` | `recebivel` | 1 — N | Um tipo define vários recebíveis (spread herdado) |
| `moeda` | `recebivel` | 1 — N | Um recebível tem uma moeda de emissão |
| `liquidacao` | `liquidacao_item` | 1 — N | Uma liquidação agrega vários itens (lote) |
| `recebivel` | `liquidacao_item` | 1 — N | Um recebível pode ser liquidado em operações distintas (um por lote, protegido por `UNIQUE`) |
| `moeda` | `liquidacao_item` | 1 — N | O item liquida numa moeda de pagamento |

## 4. Scripts DDL

Os scripts abaixo são a fonte da migração `V1__init_schema.sql` (estrutura) e `V2__seed_reference_data.sql` (dados de referência).

### 4.1 Estrutura (`V1__init_schema.sql`)

```sql
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
```

### 4.2 Dados de Referência (`V2__seed_reference_data.sql`)

```sql
INSERT INTO moeda (codigo, nome, escala) VALUES
    ('BRL', 'Real Brasileiro', 2),
    ('USD', 'Dólar Americano', 2);

INSERT INTO tipo_recebivel (codigo, nome, spread) VALUES
    ('DUPLICATA_MERCANTIL', 'Duplicata Mercantil', 0.015),
    ('CHEQUE_PRE_DATADO', 'Cheque Pré-datado', 0.025);
```

## 5. Justificativa das Decisões de Modelagem

- **`NUMERIC` para todos os valores monetários**: precisão decimal exata exigida pelo domínio financeiro.
- **`taxa_cambio` com `UNIQUE (codigo_base, codigo_cotacao, vigencia)`**: garante uma única taxa vigente por par em um instante e preserva o histórico por vigência.
- **`CHECK (codigo_base <> codigo_cotacao)`**: impede taxa de uma moeda contra si mesma.
- **`referencia_externa UNIQUE`**: identifica o recebível pela referência externa do cedente, evitando duplicidade.
- **`chave_idempotencia UNIQUE`**: implementa idempotência da liquidação (retries não duplicam operações).
- **Coluna `version` em `recebivel`**: suporta Optimistic Locking para detecção de conflito de escrita concorrente.
- **`liquidacao_item` com `UNIQUE (liquidacao_id, recebivel_id)`**: impede o mesmo recebível de aparecer duas vezes no mesmo lote.
- **`taxa_aplicada` anulável**: registra a taxa de câmbio apenas quando a operação for cross-currency, preservando a auditabilidade sem acoplar a regra de conversão.
- **Índices**: `idx_taxa_cambio_par_vigencia` acelera a busca da taxa vigente; `idx_recebivel_cedente` e `idx_liquidacao_periodo` servem ao extrato de liquidação filtrado por cedente e período.

## 6. Convenções de Migração

- Versões seguem o padrão `V<numero>__<descricao>.sql` do Flyway;
- Migrações são **imutáveis** após aplicação — correções posteriores exigem nova migração `V<n>`;
- `ddl-auto: validate` (JPA) valida que as entidades mapeadas estão coerentes com o schema versionado;
- Estrutura e dados de referência são separados em migrações distintas para rastreabilidade.
