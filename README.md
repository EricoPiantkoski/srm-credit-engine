 <p align="right">
  <img alt="Java" src="https://img.shields.io/badge/Java-21-E34F26?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=spring&logoColor=white">
  <img alt="React" src="https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black">
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-5.x-3178C6?logo=typescript&logoColor=white">
  <img alt="Vite" src="https://img.shields.io/badge/Vite-5.x-646CFF?logo=vite&logoColor=white">
  <img alt="Build" src="https://img.shields.io/github/actions/workflow/status/EricoPiantkoski/srm-credit-engine/ci.yml">
  <img alt="Release" src="https://img.shields.io/github/v/release/EricoPiantkoski/srm-credit-engine">
</p>

# SRM Credit Engine

## Sobre o projeto

O **SRM Credit Engine** é uma plataforma que visa automatizar e gerenciar o fluxo de recebíveis da **SRM Asset**, especializada em fundos de investimento em direitos creditórios. A parir da globalização do portfólio, a adoção de multimoedas (BRL e USD) tornou-se uma necessidade e é promovida por esse projeto por meio de um sistema robusto, seguro e auditável.

A fim de **precificar e liquidar** ativos com segurança e precisão decimal, a plataforma recebe um lote de recebíveis, calcula o **deságio** (desconto) com base no risco do ativo e na moeda de pagamento, e registra a transação de forma **auditável**, **atômica** e **idempotente**, de maneira que nenhuma liquidação fique "pela metade"

### Domínio SRM Credit Engine

- **Câmbio**: Responsável por armazenar e prover taxas de câmbio por par de moedas, permitindo atualização manual ou integração externa (com BCB PTAX e AwesomeAPI), conversão com arredondamento controlado e histórico por vigência da taxa.
- **Precificação**: cada tipo de recebível tem sua regra de risco (spread), aplicada via padrão `Strategy`; a fórmula `valor presente = valor face / (1 + taxa base + spread) ^ prazo` é calculada com precisão decimal.
- **Liquidação**: registra a antecipação de um lote em uma única transação `ACID`, com `optimistic locking` por recebível (via `version`) e estado `DISPONIVEL → LIQUIDADO`, protegendo contra dupla liquidação simultânea ou sequencial; idempotência por `chaveIdempotencia` (UUID).
- **Extrato**: consultas analíticas de alto volume por período, cedente e moeda, com `SQL nativo` otimizado e paginação por cursor.

### Da stack e arquitetura

- **Backend em monolito modular** com arquitetura hexagonal: domínio isolado por *bounded contexts* (câmbio, precificação, liquidação, extrato), casos de uso em camada de aplicação e adaptadores web/persistência — portas que preservam a evolução futura para microserviços sem reescrever o domínio.
- **Java 21 + Spring Boot 3** com tipagem forte, validação por Bean Validation e tratamento global de exceções resiliente.
- **PostgreSQL** com integridade `ACID`, `NUMERIC` para dinheiro (sem ponto flutuante), migrações versionadas via **Flyway** e índices para consultas de extrato.
- **Resiliência** nas chamadas externas: `retry` com `backoff` e `circuit breaker` (`resilience4j`) para BCB PTAX e AwesomeAPI, com degradação graciosa e orientação ao operador.
- **Qualidade**: testes unitários, de contrato (WireMock), de integração (Testcontainers) e de API; gate de cobertura **JaCoCo ≥ 90%**.
- **Frontend** (React 18 + TypeScript + Vite) organizado por features, com TanStack Query, React Hook Form + Zod, e testes Vitest + Playwright.

## Quick Start com Docker (Recomendado)

A forma mais rápida de rodar a stack completa (backend + frontend + PostgreSQL):

```bash
# Clonar e subir
git clone <repo-url>
cd srm_asset
docker compose up --build -d

# Verificar saúde
curl http://localhost:8080/api/health          # {"status":"UP"}
curl http://localhost:8080/api/health/readiness # {"status":"UP"}

# Acessar
# Frontend: http://localhost:5173
# Backend API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# Postgres: localhost:5656 (user: postgres, pass: postgres, db: srm_credit)
```

> **O arquivo `.env` já está configurado com valores de desenvolvimento** (JWT_SECRET, DB, CORS, etc.).  
> Para produção, gere seu próprio `JWT_SECRET`: `openssl rand -base64 32` e configure as variáveis no provedor de cloud.

### Autenticação
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Parar e limpar
```bash
docker compose down          # para containers
docker compose down -v       # para e remove volumes (reset DB)
```

---

### Documentação técnica

Para se aprofundar em cada um dos assuntos técnicos, ou visualizar contratos de API, decisões arquiteturais, dentre outros temas de interesse, planejei essa série de documentos a quem interessar, então sinta-se à vontade para explorar.

- [**TechDoc**](docs/TechDoc.md) — documentação técnica completa: módulos (câmbio, precificação, liquidação), contratos de API, persistência, configuração e operação.
- [**Decisões de Arquitetura**](docs/architecture_decision_records-precificacao_liquidacao.md) — por que precificação usa Strategy, como a precisão decimal é garantida e como o optimistic locking protege a liquidação.
- [**Diagrama C4**](docs/c4_architecture.md) — arquitetura em camadas de contexto, containers e componentes (Mermaid).
- [**Guia de Operação e Rollback**](docs/guia_operacao_rollback.md) — subida, monitoramento, alertas e estratégias de reversão (código e banco).
- [**Topologia e Arquitetura**](docs/architecture_decision_records-architecture_definition.md) — a fundamentação por trás do monolito modular em repositório único.
- [**Banco de Dados**](docs/architecture_decision_records-db_definition.md) — racional de escolha do PostgreSQL (CAP/PACELC, ACID, escala).
- [**Modelo de Dados**](docs/database_model.md) — diagrama ER, DDL e convenções do schema.
- [**OpenAPI/Swagger**](docs/TechDoc.md) — o contrato vivo da API (ver abaixo).

### Consultando a API (OpenAPI/Swagger)

A API é documentada via **OpenAPI** e pode ser explorada de duas formas:

- **Swagger UI** (interativo): com o backend rodando, acesse [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). Nele você vê todos os endpoints, envia requisições reais e confere os contratos de request/response.
- **Especificação crua**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) retorna o JSON OpenAPI — útil para gerar clientes (ex.: `openapi-generator`) ou integrar ferramentas.

## Git Flow

O projeto adota o Git Flow. A decisão considera que o produto é um serviço empacotado e versionado, com versionamento semântico manual e releases controladas, com fluxo de Continuous Integration bloqueante definido. Continuous Deployment não é uma opção válida ao escopo do projeto.

Trunk Based não é uma opção válida para o projeto dado não possuir feature flags ou necessitar de controle de release. O caso se extende ao GitHub Flow (somente `main` + features), por não oferecer linha de integração estável nem branch de estabilização de release, expondo `main` a código instável em um domínio sensível.

![alt text](.contents/gitflow.png)

# ParaTodosVerem: A imagem demonstra o Git Flow, decrito em [Fluxo](#fluxo)

### Branches

| Branch | Origem | Merge | Função |
| --- | --- | --- | --- |
| `main` | — | recebe `release/*` e `hotfix/*` | Produção. Sempre estável e releasable |
| `release/vX.Y.Z` | `main` (automática) | `main` | Snapshot imutável de estabilização e bump de versão |
| `develop` | `main` | recebe `feature/*` e `hotfix/*` | Linha de integração das features |
| `feature/*` | `develop` | `develop` | Desenvolvimento de funcionalidades |
| `hotfix/vX.Y.Z` | `main` | `main` e merge-back em `develop` | Correção urgente em produção |

### Fluxo

1. **`feature/*`** é criada a partir de `develop`, desenvolvida e integrada via PR.
2. Todo merge em `develop` com conteúdo diferente de `main` e **sem PR aberto de `release/*` para `main`** dispara a criação automática de **`release/vX.Y.Z`** a partir de `main` (bump patch a partir da última tag).
3. Enquanto existir PR aberto de `release/*` para `main`, nenhuma nova release é criada — a release ativa guarda o estado e recebe o conteúdo de `develop` via PR.
4. **`release/vX.Y.Z`** é estabilizada e mergeada na `main` via PR.
5. O merge na `main` gera automaticamente a **tag `vX.Y.Z`** e a **GitHub Release**.
6. **`hotfix/vX.Y.Z`** nasce de `main`, corrige produção e retorna para `main` (tag) e `develop` (merge-back).

## Estrutura do Projeto

Repositório único com aplicativos independentes e infraestrutura local:

```
.
├── backend/              # Spring Boot 3 (Java 21, Maven, Maven Wrapper)
├── frontend/             # React 18 (Vite, TypeScript strict, pnpm)
├── .github/workflows/    # CI, release e criação automática de branches
├── docker-compose.yml    # PostgreSQL 16 local
└── .docs/                # Documentação técnica e planos
```

Cada aplicativo tem build, testes e ciclo de release próprios; o repositório concentra a fonte e a automação, sem impor release sincronizada entre eles.

### Backend — Arquitetura Hexagonal

```
backend/src/main/java/com/srm/creditengine/
├── application/          # casos de uso e portas (in/out)
├── domain/               # modelos e regras de negócio
└── infrastructure/       # adapters (web, persistência) e configuração
```

- `GET /api/health` → `{"status":"UP"}` (contrato inicial da API).
- Swagger/OpenAPI em `/swagger-ui.html` (springdoc).
- Testes de integração com Testcontainers (PostgreSQL isolado).
- Gate de cobertura JaCoCo ≥ 90% no `./mvnw verify`.
- Configuração por variáveis de ambiente (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`, `CORS_ALLOWED_ORIGINS`); perfil `local` traz os valores de desenvolvimento (banco/CORS) via `application-local.yml`.

### Frontend — Organização por Features

```
frontend/src/
├── app/            # bootstrap, providers
├── components/     # componentes visuais reutilizáveis
├── features/       # funcionalidades por domínio de interface
├── pages/          # composição de telas
├── lib/api/        # cliente HTTP tipado
├── state/          # estado global de cliente (Zustand, quando necessário)
├── styles/         # tokens e estilos globais
└── test/           # setup e servidor MSW
```

- TanStack Query para dados do servidor; formulários com React Hook Form + Zod.
- Testes com Vitest + React Testing Library + MSW; E2E com Playwright.
- Configuração pública via `VITE_*` (ver `frontend/.env.example`).

### Execução local

Infraestrutura (PostgreSQL 16):

```bash
docker compose up -d
```

Backend (porta 8080):

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Frontend (dev server):

```bash
cd frontend && pnpm install --frozen-lockfile && pnpm dev
```

O frontend requer Node.js `>=20`; a versão recomendada é Node 22, fixada em
`frontend/.nvmrc`. Os gates locais são:

```bash
cd frontend
pnpm typecheck && pnpm lint && pnpm test && pnpm build
pnpm test:coverage && pnpm test:e2e
```

O Sentry frontend permanece inativo sem `VITE_SENTRY_DSN`. Quando configurado,
use também `VITE_SENTRY_ENVIRONMENT`; o upload de sourcemaps exige
`SENTRY_AUTH_TOKEN`, `SENTRY_ORG` e `SENTRY_PROJECT`.

### Execução com Docker (produção/staging)

Imagens multi-stage: `backend/Dockerfile` (Maven → JRE 21, healthcheck em
`/api/health`) e `frontend/Dockerfile` (Node 22 + pnpm → nginx com proxy de
`/api` para o backend via `BACKEND_URL`).

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Crie um `.env` na raiz (já ignorado pelo Git) com as variáveis obrigatórias:

```dotenv
POSTGRES_DB=srm_asset
POSTGRES_USER=srm
POSTGRES_PASSWORD=segredo-do-banco
JWT_SECRET=segredo-base64-com-no-minimo-32-bytes
```

### Hooks de pre-commit

Com Node 22 ativo, `cd frontend && pnpm install` ativa o husky automaticamente
(script `prepare`). O hook `pre-commit` roda `lint-staged` (ESLint) sobre os
arquivos staged e bloqueia o commit em caso de erro.

### Deploy contínuo em staging

O workflow `.github/workflows/deploy-staging.yml` (push em `main` ou
`workflow_dispatch`) builda e publica as imagens backend/frontend no GHCR com
as tags `latest` e `sha-<commit>`. Com a variável de repositório
`DEPLOY_STAGING=true` e os secrets `STAGING_HOST`, `STAGING_SSH_USER` e
`STAGING_SSH_KEY` configurados, o job `deploy` executa no host
`docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d`
(repositório clonado em `/opt/srm_asset`, com o mesmo `.env` e
`BACKEND_IMAGE`/`FRONTEND_IMAGE` apontando para o GHCR).

## Gestão de Crise e Rollback

Um bug crítico em produção é tratado com hotfix, mas nem toda correção chega a tempo — às vezes a decisão é **reverter** a alteração. Reverter em `main` é a estratégia de rollback preferida quando a correção não pode ser desenvolvida e validada no tempo necessário.

A recomendação segura é reverter antes (git revert) para estabilizar o ambiente e, posteriormente, desenvolver um hotfix para corrigir o bug de maneira consistente.

### Reverter com segurança

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

### Por que `git revert` e não `git reset`

- **`git revert`** cria um commit novo desfazendo a alteração. O histórico permanece íntegro — essencial para rastreabilidade e para o fluxo de release, pois outros commits e branches já dependeram do código.
- **`git reset --hard`** apaga o histórico e gera conflitos irreversíveis na `main` compartilhada, quebrando a integridade das tags e releases já publicadas.

### Recomendações

- Reverta o **merge** com `-m 1` para preservar a primeira parentagem; depois sincronize `develop` com a correção.
- Após o revert, o bug continua existindo no código — trate-o em `hotfix/*` ou `feature/*` na sequência.
- Evite reverter um commit já revertido sem validação, pois os commits podem se anular silenciosamente.
