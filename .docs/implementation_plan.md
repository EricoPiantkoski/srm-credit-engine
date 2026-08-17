# Plano de Implementação — Pontos 7 a 10

Base: `.specs/domain.md` (§2.7–2.10 e §3.3–3.4). Estado atual: Pontos 4–6 entregues (Câmbio, Precificação, Simulação), 152 testes, BUILD SUCCESS, JaCoCo ≥ 90%. Módulos `liquidacao` e `extrato` existem como esqueleto vazio (`.gitkeep`). Tabelas `liquidacao`/`liquidacao_item` **já existem** no `V1__init_schema.sql` — não recriar.

---

## Ponto 7 — Liquidação com integridade ACID e proteção contra concorrência

> `domain.md` §2.7 e §3.3.

### 7.1 Modelo de domínio (`liquidacao/domain`)
- `Liquidacao` — agregado raiz (lote, `chaveIdempotencia`, `StatusLiquidacao`, itens).
- `ItemLiquidacao` — valor face, spread aplicado, prazo meses, valor presente, moeda de pagamento, taxa aplicada, valor líquido.
- `StatusLiquidacao` — enum (`PROCESSANDO`, `LIQUIDADA`, `FALHOU`).
- Value objects reutilizados de `shared`/`precificacao` (`Dinheiro`, `CodigoMoeda`).

### 7.2 Casos de uso (`liquidacao/application`)
- `LiquidarLote` — orquestra por **portas** (Precificação + Câmbio), nunca por implementações:
  - valida `chaveIdempotencia` única (duplicada → `409`);
  - precifica cada recebível (spread por tipo, conversão cambial no final);
  - persiste cabeçalho + itens numa **única transação** (`@Transactional`).
- `ConsultarLiquidacao` — por id, por status, por período (para o Ponto 9/Extrato).
- `RepositorioLiquidacao` (porta de saída) + `RepositorioRecebivel` (com lock otimista).

### 7.3 Infraestrutura (`liquidacao/infrastructure`)
- `LiquidacaoJpaEntity` + `LiquidacaoItemJpaEntity` mapeando as tabelas já existentes (`V1`).
- `LiquidacaoJpaRepository` (Spring Data JPA) implementando a porta.
- `@Version` no `RecebivelJpaEntity` (otimistic locking) — propagar do domínio à persistência.
- Resposta de conflito: `OptimisticLockException` → `409` com orientação de reprocessamento (handler no Ponto 10).

### 7.4 API HTTP
- `POST /api/liquidacoes` — `201 Created`; erros `400` (validação), `409` (idempotência/versão), `422` (regra de negócio).
- `GET /api/liquidacoes/{id}` — `200` com itens auditáveis; `404` quando não existir.

### 7.5 Critérios de aceite
- Lote atômico: se um item falhar, nada persiste;
- Conflito de versão → `409` e a aplicação reage de forma controlada;
- Mesma `chaveIdempotencia` não duplica liquidação;
- Cada item registra valor face, spread, prazo, valor presente, moeda, taxa e valor líquido (auditoria).

---

## Ponto 8 — API First: contratos antes da implementação

> `domain.md` §2.8.

### 8.1 Contratos definidos (OpenAPI/Springdoc como contrato vivo)
| Operação | Método | Sucesso | Erro |
| --- | --- | --- | --- |
| Criar recebível | `POST /api/recebiveis` | 201 | 400, 409 |
| Listar recebíveis | `GET /api/recebiveis` | 200 | — |
| Simular precificação | `POST /api/simulacoes/precificacao` | 200 | 400, 422 |
| Liquidação em lote | `POST /api/liquidacoes` | 201 | 400, 409, 422 |
| Atualizar taxa de câmbio | `PUT /api/taxas-cambio` | 200 | 400, 409 |
| Extrato de liquidação | `GET /api/liquidacoes/extrato` | 200 | 400 |

### 8.2 Anotação dos endpoints (padrão já usado em Câmbio/Precificação/Simulação)
- `@Tag`, `@Operation`, `@ApiResponse` em `LiquidacaoController` e `ExtratoController`.
- Exemplos de requisição/resposta nos contratos novos.
- `422` para validação semântica de negócio (separar de `400` malformado).

### 8.3 Verificação
- `OpenApiContractTest` estendido: validar que `/v3/api-docs` contém os novos paths/summaries.

---

## Ponto 9 — Persistência: ORM para CRUD, SQL nativo para análise

> `domain.md` §2.9 e §3.4.

### 9.1 Escrita (JPA)
- Confirmar `ddl-auto: validate` + Flyway como única fonte do schema.
- `LiquidacaoJpaEntity`/`LiquidacaoItemJpaEntity` (do Ponto 7) sem fetch N+1 (`@EntityGraph`/joins explícitos).

### 9.2 Leitura analítica — Extrato (`extrato/`)
- Porta de saída `ConsultaLiquidacao` dedicada (segregada do repositório de escrita — CQRS leve).
- Adaptador com `JdbcTemplate` + **SQL nativo parametrizado** (sem injeção):
  - paginação por **cursor** (`WHERE id > :lastId ORDER BY id`) em vez de `OFFSET`;
  - filtros opcionais dinâmicos: período, status, cedente, moeda;
  - agregações (`GROUP BY` por período/moeda) em SQL, não em memória;
  - passa pela camada de aplicação (autorização e contrato), não pela de negócio.

### 9.3 Migração
- `V3__extrato_indexes.sql`: índices compostos conforme §5.3 do `domain.md` (período, cedente, moeda) — item pendente do checklist ("Índices adicionais para consultas de extrato/liquidação").

### 9.4 Critérios de aceite
- Extrato com filtros por período/status/cedente/moeda;
- Paginação estável por cursor;
- Agregações corretas e uso de índice.

---

## Ponto 10 — Tratamento de exceções global e resiliente

> `domain.md` §2.10. Base: `GlobalExceptionHandler` já existente (shared).

### 10.1 Mapeamentos a adicionar/confirmar
- `DomainException` → `4xx` semântico (já existe → `422` genérico);
- `MethodArgumentNotValidException` → `400` com campo + mensagem (já existe);
- **`OptimisticLockException` → `409`** com orientação de reprocessamento (novo — exige `@Version` no Ponto 7);
- **Conflito de idempotência → `409`** (novo);
- `ConstraintViolationException`/parâmetros malformados → `400` (já existe);
- Exceção inesperada → `500` genérico + `requestId` + log estruturado, sem detalhes internos (já existe);
- Nenhuma exceção ignorada; fluxo nunca segue após falha que comprometa consistência.

### 10.2 Regras permanentes
- Domínio lança, fronteira traduz (nada de HTTP no domínio);
- `ErrorBody(message, resolution)` mantido; `resolution` só nos casos 503/422 de câmbio;
- Segredos e dados sensíveis nunca em logs.

### 10.3 Verificação
- Testes MockMvc para os novos mapeamentos (`409` versão, `409` idempotência);
- Log estruturado de `500` com `requestId`.

---

## Estratégia de testes (transversal)
- **Unitários**: domínio `Liquidacao` (validações, estado), casos de uso com portas mockadas.
- **Integração (Testcontainers)**: transação atômica (rollback de item), optimistic locking (2 threads/2 chamadas), idempotência (mesma chave), extrato SQL nativo (filtros + cursor + agregação).
- **Web (MockMvc)**: `POST /api/liquidacoes`, `GET /api/liquidacoes/{id}`, `GET /api/liquidacoes/extrato`, novos handlers do Ponto 10.
- **Contrato**: `OpenApiContractTest` estendido.
- Gate: `./mvnw verify` BUILD SUCCESS + JaCoCo ≥ 90%.

## Impacto no checklist
- Liquidação (Ponto 7, peso 18): itens `[ ]` → `[x]`.
- Extrato (peso 8): itens `[ ]` → `[x]`.
- Qualidade e Testes (peso 5): teste manual curl dos novos endpoints.
- Dados e Infraestrutura (peso 2): índices do extrato.
- Documentação: atualizar `TechDoc.md`, ADRs (ADR-005 Optimistic Locking; ADR-007 JPA + SQL nativo), `database_model.md` se necessário.

## Ordem sugerida
1. Ponto 8 (contratos) como esqueleto dos endpoints → 2. Ponto 7 (domínio + persistência + transação) → 3. Ponto 10 (handlers 409) → 4. Ponto 9 (extrato SQL nativo + índices) → 5. Testes e fechamento do checklist.