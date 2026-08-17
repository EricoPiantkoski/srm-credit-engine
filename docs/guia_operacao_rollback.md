# Guia de Operação e Rollback — SRM Credit Engine

Guia operacional para subir, operar, monitorar e reverter o SRM Credit Engine em ambiente de execução. Complementa o README (execução local) e o TechDoc (configuração), focando na operação e nas estratégias de reversão.

---

## 1. Visão geral do ambiente

| Componente | Onde roda | Observações |
| --- | --- | --- |
| Backend | Spring Boot (Java 21), `PORT` default 8080 | Monolito modular; profile `local` apenas para desenvolvimento |
| Frontend | React SPA (Vite), dev server | Consome a API via REST; CORS liberado por env |
| PostgreSQL 16 | `docker compose` (local) ou serviço gerenciado | Fonte transacional única; schema versionado por Flyway |

Variáveis de ambiente do backend:

| Variável | Descrição | Obrigatória |
| --- | --- | --- |
| `DB_URL` | JDBC URL do PostgreSQL | Sim |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | Sim |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas (ex.: `https://app.exemplo.com`) | Sim |
| `PORT` | Porta HTTP (default `8080`) | Não |
| `BCB_PTAX_BASE_URL` | Endpoint do BCB PTAX (default `https://olinda.bcb.gov.br/.../odata`) | Não |
| `AWESOME_API_BASE_URL` | Endpoint da AwesomeAPI (default `https://economia.awesomeapi.com.br`) | Não |
| `PRECIFICACAO_TAXA_BASE` | Taxa base da precificação (default `0.0`) | Não |

---

## 2. Subida e checagem de saúde

### 2.1 Subir a infraestrutura (PostgreSQL local)

```bash
docker compose up -d
```

### 2.2 Subir o backend

Desenvolvimento:

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Produção (perfil padrão, sem defaults de banco/CORS — tudo via env):

```bash
DB_URL=jdbc:postgresql://<host>:5432/srm_credit \
DB_USERNAME=... DB_PASSWORD=... \
CORS_ALLOWED_ORIGINS=https://app.exemplo.com \
./mvnw spring-boot:run
```

### 2.3 Checagem de saúde

```bash
curl -s http://localhost:8080/api/health
# {"status":"UP"}
```

Endpoints de observabilidade expostos (`management.endpoints.web.exposure.include=health,info,metrics,prometheus`):

- `GET /api/health` — health check da aplicação.
- `GET /actuator/health` — health completo do Actuator (inclui datasource).
- `GET /actuator/metrics` — métricas (JVM, HTTP, etc.).
- `GET /actuator/prometheus` — métricas no formato Prometheus (scrape).

### 2.4 Migrações (Flyway)

- Migrações executadas automaticamente na subida (`spring.flyway.enabled=true`, `locations=classpath:db/migration`, `ddl-auto: validate`).
- Arquivos: `V1__init_schema.sql`, `V2__seed_reference_data.sql`, `V3__extrato_indexes.sql`, `V4__recebivel_status.sql`.
- **Não edite** uma migração já aplicada: crie `V5__...` em diante. O Flyway valida o checksum das migrações aplicadas e falha a subida se detectar divergência.

---

## 3. Operação rotineira

### 3.1 Câmbio

- Atualização manual: `PUT /api/taxas-cambio` (vigência ocupada → `409`; use nova vigência).
- Integração automática: `POST /api/taxas-cambio/integracao?codigoBase=USD&codigoCotacao=BRL` busca a taxa no BCB PTAX e persiste de forma idempotente.
- Leitura com fallback: `GET /api/taxas-cambio/vigente` busca no banco e, na ausência, no provedor (sem persistir).
- **Indisponibilidade dos provedores** (BCB/AwesomeAPI): o retry com backoff (3 tentativas) + circuit breaker degradam de forma controlada; a API responde `503` orientando a inserção manual via `PUT`. Operar por inserção manual enquanto o provedor estiver fora.

### 3.2 Recebíveis

- Cadastro: `POST /api/recebiveis` (vencimento futuro; `referenciaExterna` única por cedente → `409` em duplicidade).
- Listagem: `GET /api/recebiveis?cedente=&codigoMoeda=&codigoTipo=&page=&size=`.

### 3.3 Liquidação

- `POST /api/liquidacoes` com `chaveIdempotencia` (UUID), `codigoMoedaPagamento` e `recebiveisIds`.
- **Idempotência**: reenviar a mesma chave → `409` sem duplicar.
- **Concorrência**: dois lotes disputando o mesmo recebível → um vence, o outro recebe `409` (optimistic locking por `version` + status `DISPONIVEL`/`LIQUIDADO`). Reprocessar com dados atuais (a precificação é recalculada na transação).
- **Auditoria**: cada item registra valor face, spread, prazo, valor presente, moeda, taxa e valor líquido; nada é sobrescrito.

### 3.4 Extrato

- `GET /api/liquidacoes/extrato?dataInicial=&dataFinal=&status=&cedente=&lastId=&limit=` — paginação por cursor (`li.id > lastId`); a última página devolve o `lastId` para a próxima requisição.

### 3.5 API viva (OpenAPI)

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Spec JSON: `http://localhost:8080/v3/api-docs`

---

## 4. Monitoramento e alertas

- **Health**: monitorar `/api/health` (ou `/actuator/health`) com checagem de intervalo; `DOWN` no datasource indica problema de banco.
- **Circuit breakers** (resilience4j): estado `OPEN` significa que o provedor externo está falhando — alertar para os provedores de câmbio.
- **Métricas** (`/actuator/metrics`, `/actuator/prometheus`): latência HTTP, taxa de erro, uso de heap e conexões JDBC.
- **Logs**: padrão JSON/estruturado recomendado em produção; correlacionar por `traceId`.

Alertas sugeridos:

| Condição | Severidade | Ação |
| --- | --- | --- |
| Health `DOWN` por mais de 2 ciclos | Crítica | Verificar banco e rede |
| Circuit breaker `OPEN` | Alta | Verificar BCB/AwesomeAPI; operar com taxa manual |
| Erros `5xx` acima de limiar | Alta | Revisar logs e dependências externas |
| `409` em massa em liquidações | Média | Indica reprocessamento incorreto de lotes; revisar operação |

---

## 5. Rollback — estratégias e procedimentos

### 5.1 Princípios

- **Preferência por `git revert`** (não `git reset --hard`): reverte criando um commit novo, preservando o histórico, as tags e as releases já publicadas.
- Rollback de **código** e rollback de **banco** são decisões independentes — um pode ser necessário sem o outro.
- O sistema financeiro não tolera inconsistência: nenhum procedimento de rollback pode deixar dados parciais; a liquidação é atômica (tudo ou nada).

### 5.2 Rollback de código (git revert)

1. Identifique o merge problemático no histórico:

   ```bash
   git log --oneline -10
   ```

2. Reverta o merge criando um novo commit (não reescreva o histórico):

   ```bash
   git revert -m 1 <sha-do-merge>
   ```

3. Envie e abra PR para `main` como qualquer outra mudança:

   ```bash
   git push
   ```

4. Sincronize `develop` com a correção (merge-back).

**Observações:**

- Reverta o **merge** com `-m 1` para preservar a primeira parentagem.
- Após o revert, o bug continua existindo no código — trate-o em `hotfix/*` ou `feature/*` na sequência.
- Evite reverter um commit já revertido sem validação (os commits podem se anular silenciosamente).

### 5.3 Rollback de banco (Flyway)

O Flyway não desfaz migrações automaticamente (`undo` não habilitado por padrão). Estratégias:

- **Migração corretiva (recomendada):** se a mudança de schema causou problema, crie uma **nova** migração `V5__...` que reverta o efeito (ex.: recria coluna/tabela) em vez de apagar `V4`. Nunca altere uma migração já aplicada — o Flyway valida checksums e falha a subida.
- **Banco local/desenvolvimento:** pode-se resetar o schema com `flyway clean` (via `./mvnw flyway:clean` ou drop do volume) e reaplicar tudo do zero. **Nunca use `clean` em produção** — destrói todos os dados.
- **Produção:** restaurar de backup é a única reversão segura de dados. Certifique-se de ter backup validado antes de aplicar qualquer migração crítica.

### 5.4 Compensação de dados (negócio)

Se uma liquidação indevida foi registrada:

- A liquidação é **append-only** (auditável): não há endpoint de exclusão/estorno automático no escopo atual.
- O registro permanece no extrato como evidência; a compensação deve ser um **novo** registro (ex.: liquidação de valor negativo ou ajuste manual via processo operacional definido pela mesa).
- Nunca edite/apague linhas diretamente no banco fora de um procedimento formal de compensação, para preservar a trilha de auditoria.

### 5.5 Playbook rápido

| Sintoma | Ação imediata |
| --- | --- |
| Bug crítico em produção | `hotfix/vX.Y.Z` a partir de `main`; ou `git revert -m 1` do merge problemático |
| Migração quebrou a subida | Não editar a migração aplicada; criar `V5__...` corretiva |
| Provedor de câmbio fora | Aguardar circuit breaker; operar com `PUT` manual |
| Liquidação indevida | Não apagar; registrar compensação formal (nova operação) |
| Banco inconsistente | Restaurar backup validado; pausar escrita até recuperação |

---

## 6. Referências

- [README](../README.md) — estrutura do projeto, Git Flow e execução local.
- [TechDoc](TechDoc.md) — contratos de API, persistência, configuração e operação.
- [Diagrama C4](c4_architecture.md) — arquitetura em contexto/containers/componentes.
- [Decisões de Precificação e Liquidação](architecture_decision_records-precificacao_liquidacao.md) — idempotência, concorrência e auditoria.