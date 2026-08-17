# TechDoc — SRM Credit Engine

Documentação técnica das decisões de arquitetura, módulos, contratos de API, persistência, configuração e operação do backend.

---

## 1. Módulo Câmbio (`com.srm.creditengine.cambio`)

### 1.1 Responsabilidade

Armazenar e prover taxas de câmbio por par de moedas, com histórico por vigência, atualização manual ou integração externa (BCB PTAX) e conversão de valores com arredondamento controlado.

### 1.2 Estrutura (arquitetura hexagonal)

```
cambio/
├── domain/
│   ├── ParMoedas                     VO: base + cotacao (devem diferir)
│   ├── TaxaCambio                    entidade: par, taxa (escala 8), vigencia (Instant)
│   ├── MoedaRepository               porta de saída
│   ├── TaxaCambioProvider            porta de saída (adapter externo)
│   ├── TaxaCambioRepository          porta de saída
│   └── exception/                    UnknownCurrency, ExchangeRateConflict,
│                                     ExchangeRateNotFound, ExchangeRateProviderUnavailable
├── application/
│   ├── TaxaCambioUpdater             atualização manual (valida moedas, conflito de vigência → 409)
│   ├── TaxaVigenteReader             leitura da taxa vigente (data máxima ≤ referência)
│   ├── DinheiroConverter             conversão base * taxa e inversa 1 / taxa
│   └── TaxaCambioOrchestrator        provedor → repositório (idempotente por vigência)
└── infrastructure/
    ├── adapter/in/web/TaxaCambioController
    ├── adapter/out/persistence/      MoedaJpaEntity, TaxaCambioJpaEntity, JpaRepository, adapters
    ├── adapter/out/external/         BcbPtaxClient (Feign), BcbPtaxTaxaCambioProvider
    └── config/TaxaCambioProviderConfig
```

### 1.3 Regras de negócio

- Moeda deve existir antes de aceitar taxa (validação via `MoedaRepository`).
- Taxa deve ser positiva; persistida com escala 8 (`NUMERIC(19,8)`), arredondamento `HALF_EVEN`.
- Um par pode ter várias taxas em vigências distintas (histórico); `UNIQUE (codigo_base, codigo_cotacao, vigencia)`.
- Conversão: `base * taxa` para a moeda de cotação; operação inversa usa `1 / taxa` com arredondamento controlado.
- A vigência ocupada rejeita a atualização manual com `409`.

### 1.4 Contratos de API

Base: `/api/taxas-cambio`. Erros em formato padronizado `{ "message": "..." }` via `GlobalExceptionHandler`.

| Operação | Método e endpoint | Sucesso | Erros |
| --- | --- | --- | --- |
| Atualizar taxa manualmente | `PUT /api/taxas-cambio` | `200` | `400`, `409`, `422` |
| Obter taxa vigente | `GET /api/taxas-cambio/vigente?codigoBase=USD&codigoCotacao=BRL` | `200` | `400`, `404`, `503` |
| Integrar taxa do provedor (BCB PTAX) | `POST /api/taxas-cambio/integracao?codigoBase=USD&codigoCotacao=BRL` | `200` | `400`, `422`, `503` |
| Converter valor entre moedas | `POST /api/taxas-cambio/convert` | `200` | `400`, `404`, `422`, `503` |

**Fallback para o provedor (decisão aprovada):** quando não há taxa vigente armazenada para o par, a leitura busca automaticamente no BCB PTAX:
- `GET /vigente`: busca no provedor **sem persistir** (GET permanece *safe*/sem efeito colateral); se o provedor não tiver dados → `404`.
- `POST /convert`: busca no provedor **e persiste de forma idempotente** (via `TaxaCambioOrchestrator` — só grava se a vigência ainda não existe); POST permite efeito colateral.
- Falha da integração (BCB indisponível/erro) → `503` orientando a inserção manual via `PUT`.

**Mapeamento exceção → HTTP** (todos no `GlobalExceptionHandler`):

| Exceção | HTTP |
| --- | --- |
| `UnknownCurrencyException` (DomainException genérico) | `422 Unprocessable Entity` |
| `ExchangeRateConflictException` | `409 Conflict` |
| `ExchangeRateNotFoundException` | `404 Not Found` |
| `ExchangeRateProviderUnavailableException` | `503 Service Unavailable` |
| `IncompatibleCurrenciesException` | `422 Unprocessable Entity` |
| `ConstraintViolationException` / `MethodArgumentNotValidException` | `400 Bad Request` |

### 1.5 Adapter externo — BCB PTAX (Feign Client)

- `BcbPtaxClient` (`@FeignClient`, url de `app.cambio.bcb-ptax.base-url`) chama a função OData `CotacaoMoedaPeriodo` do BCB com assinatura real `(moeda, dataInicial, dataFinalCotacao)` (validada no `$metadata`).
- `BcbPtaxTaxaCambioProvider` traduz o par em consulta, usa a cotação de **Fechamento mais recente** (`cotacaoVenda`) e aplica a orientação do par:
  - `ParMoedas(USD, BRL)` → taxa = cotação de venda do USD em BRL;
  - `ParMoedas(BRL, USD)` → taxa = `1 / cotação`;
  - Par sem a moeda cotada configurada (`quote-currency`, default `USD`) → `Optional.empty()`.
- `dataHoraCotacao` (com microssegundos, ex.: `2026-08-13 10:08:11.052389`) é normalizado para ISO e convertido para `Instant` no fuso `America/Sao_Paulo`.
- Query params OData iniciados com `@` são enviados via `@RequestParam Map<String, String>` (o `SpringMvcContract` do Feign não expande nomes com `@` como template).
- Timeout configurado no Feign; falha/indisponibilidade do provedor (`FeignException`) → `ExchangeRateProviderUnavailableException` → degradação graciosa com `503`.

**Resiliência (retry + circuit breaker):** as duas chamadas externas (BCB PTAX e AwesomeAPI) são protegidas por:
- **Retry com backoff no Feign** via `spring.cloud.openfeign.client.config.<name>.retryer` → `BackoffRetryer` (`feign.Retryer.Default` com período inicial 100ms, máximo 1s, 3 tentativas — backoff exponencial).
- **ErrorDecoder customizado** (`RetryableServerErrorDecoder`): converte qualquer resposta 5xx em `RetryableException` para que o retry acima efetivamente atue (o `ErrorDecoder.Default` só retenta quando há header `Retry-After`).
- **Circuit breaker** (resilience4j, starter `spring-cloud-starter-circuitbreaker-resilience4j`): `@CircuitBreaker(name = "bcbPtax" | "awesomeApiBrc", fallbackMethod = "obtainFallback")` nos providers; o fallback relança `ExchangeRateProviderUnavailableException` (→ `503` com `resolution` de inserção manual). Config em `resilience4j.circuitbreaker` (janela deslizante 10, mínimo 5 chamadas, limiar 50%, half-open 3).

### 1.6 Persistência

- Sem migração nova: tabelas `moeda` e `taxa_cambio` existem desde `V1__init_schema.sql`; seed BRL/USD em `V2__seed_reference_data.sql`.
- Consulta de taxa vigente: `findFirstByCodigoBaseAndCodigoCotacaoAndVigenciaLessThanEqualOrderByVigenciaDesc`, suportada pelo índice `idx_taxa_cambio_par_vigencia`.
- `MoedaRepositoryAdapter` valida a existência da moeda (`moeda` PK por `codigo`).

### 1.7 Configuração (`application.yml`)

```yaml
app:
  cambio:
    bcb-ptax:
      base-url: ${BCB_PTAX_BASE_URL:https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata}
      timeout-ms: ${BCB_PTAX_TIMEOUT_MS:5000}
      quote-currency: ${BCB_PTAX_QUOTE_CURRENCY:USD}
    awesome-api:
      base-url: ${AWESOME_API_BASE_URL:https://economia.awesomeapi.com.br}
      timeout-ms: ${AWESOME_API_TIMEOUT_MS:5000}
spring:
  cloud:
    openfeign:
      client:
        config:
          bcbPtax:
            connect-timeout: ${app.cambio.bcb-ptax.timeout-ms}
            read-timeout: ${app.cambio.bcb-ptax.timeout-ms}
          awesomeApiBrc:
            connect-timeout: ${app.cambio.awesome-api.timeout-ms}
            read-timeout: ${app.cambio.awesome-api.timeout-ms}
```

### 1.8 Segunda fonte de câmbio — AwesomeAPI (`(BRL, USD)`)

- `AwesomeApiBrcClient` (`@FeignClient`, url de `app.cambio.awesome-api.base-url`) chama `GET /json/last/BRL-USD`.
- `AwesomeApiBrcProvider` usa o campo `ask` **diretamente** (sem `1 / taxa`), escala 8, para `ParMoedas(BRL, USD)`; par inverso `(USD, BRL)` não é suportado por esta fonte (`Optional.empty()`).
- Vigência derivada de `create_date` (`yyyy-MM-dd HH:mm:ss`) interpretada no fuso `America/Sao_Paulo`; conversão de datas/horários em `America/Sao_Paulo`.
- `TaxaCambioProviderRouter` (`@Primary`) roteia: `(BRL, USD)` → AwesomeAPI; demais pares → BCB PTAX.
- Falha/indisponibilidade da AwesomeAPI (`FeignException`) → `ExchangeRateProviderUnavailableException` → `503` (retry com backoff + circuit breaker, ver seção 1.5).

### 1.9 Testes

- Unitários: `ParMoedasTest`, `TaxaCambioTest`, `TaxaCambioUpdaterTest`, `TaxaVigenteReaderTest`, `DinheiroConverterTest`, `TaxaCambioOrchestratorTest`, `BcbPtaxTaxaCambioProviderTest`, `AwesomeApiBrcProviderTest`, `TaxaCambioProviderRouterTest` (Mockito).
- Contrato HTTP (WireMock): `BcbPtaxClientContractTest`, `AwesomeApiBrcClientContractTest`.
- Resiliência (integração, WireMock + Testcontainers): `FeignRetryIntegrationTest` (503 → retry com backoff, 3 tentativas), `CircuitBreakerIntegrationTest` (abre após 2 falhas e fallback responde sem chamar o provedor).
- OpenAPI: `OpenApiContractTest` (`/v3/api-docs` expõe `@Operation`/`@ApiResponse`/`@Tag`).
- Controller (MockMvc): `TaxaCambioControllerTest`.
- Persistência (Testcontainers PostgreSQL): `TaxaCambioRepositoryAdapterTest`.
- Handler global: `GlobalExceptionHandlerTest` (409/404/503/400).

---

## 2. Módulo Precificação (`com.srm.creditengine.precificacao`)

### 2.1 Responsabilidade

Calcular o valor presente/antecipado de recebíveis (deságio por spread de tipo + taxa base), registrar e listar recebíveis, e simular a precificação sem persistir. Quando a moeda de pagamento difere da moeda do recebível, aplica a taxa de câmbio vigente via `CambioGateway`.

### 2.2 Estrutura (arquitetura hexagonal)

```
precificacao/
├── domain/
│   ├── Recebivel                    entidade: referenciaExterna, codigoTipo, valorFace,
│   │                                 codigoMoeda, dataVencimento, cedente, version
│   ├── TipoRecebivel                enum/entidade de referência (DUPLICATA_MERCANTIL, CHEQUE_PRE_DATADO)
│   ├── Spread                       VO: valor percentual positivo
│   ├── TaxaCambioAplicada           VO: moeda pagamento, taxa, escala
│   ├── ResultadoPrecificacao        VO: valorPresente, desagio, taxaCambioAplicada (opcional)
│   ├── RecebivelQueryCriteria       VO: filtros (cedente, codigoMoeda, codigoTipo, page, size)
│   ├── PrecificacaoStrategy         porta de domínio: spreadFor(Recebivel)
│   ├── DuplicataMercantilStrategy   spread 1,5%
│   ├── ChequePreDatadoStrategy      spread 2,5%
│   ├── PrecificacaoStrategyResolver  resolve por codigoTipo
│   ├── RecebivelRepository          porta de saída
│   ├── TipoRecebivelRepository      porta de saída
│   ├── MoedaCatalog                 porta de saída (escala da moeda)
│   ├── CambioGateway                porta de saída (conversão via taxas vigentes)
│   └── exception/                   UnknownReceivableType, UnknownCurrency,
│                                     InvalidPricing, ExchangeRateUnavailable, ReceivableConflict
├── application/
│   ├── PrecificacaoEngine           motor: valorPresente = valorFace / (1 + taxaBase + spread)^prazoMeses
│   ├── RecebivelCreator             cria recebível (valida vencimento futuro, unicidade de referenciaExterna)
│   ├── RecebivelQuery               lista recebíveis com filtros
│   └── PrecificacaoSimulator        simula sem persistir (referenciaExterna/cedente = "simulacao")
└── infrastructure/
    ├── adapter/in/web/              RecebivelController, SimulacaoController
    ├── adapter/out/persistence/     RecebivelJpaEntity, TipoRecebivelJpaEntity,
    │                                 JpaRepository, adapters, MoedaCatalogAdapter
    ├── adapter/out/cambio/          CambioGatewayAdapter (usa TaxaCambioUpdater/TaxaVigenteReader)
    └── config/PrecificacaoConfig
```

### 2.3 Regras de negócio

- Vencimento deve ser no futuro (validado por `validateDataVencimentoInFuture`).
- `referenciaExterna` é único por cedente; duplicidade → `ReceivableConflictException` → `409`.
- Fórmula: `valorPresente = valorFace / (1 + taxaBase + spread) ^ prazoMeses`; `prazoMeses = dias / 30` (escala 6, HALF_EVEN); expoente inteiro → `BigDecimal.pow`, fracionário → `Math.pow` (ver ADR-003).
- `spread` por tipo: duplicata mercantil 1,5%, cheque pré-datado 2,5% (ver ADR-004).
- Conversão cambial aplicada apenas no final: `valorPresente * taxa` quando `codigoMoeda` ≠ `moedaPagamento`; ausência de taxa → `ExchangeRateUnavailableException`.
- O simulador nunca persiste; usa a mesma fórmula do motor.

### 2.4 Contratos de API

Base: `/api`. Erros em formato padronizado `{ "message": "..." }` via `GlobalExceptionHandler`.

| Operação | Método e endpoint | Sucesso | Erros |
| --- | --- | --- | --- |
| Criar recebível | `POST /api/recebiveis` | `201` | `400`, `409`, `422` |
| Listar recebíveis | `GET /api/recebiveis?cedente=&codigoMoeda=&codigoTipo=&page=&size=` | `200` | `400` |
| Simular precificação | `POST /api/simulacoes/precificacao` | `200` | `400`, `422`, `503` |

**Mapeamento exceção → HTTP** (todos no `GlobalExceptionHandler`):

| Exceção | HTTP |
| --- | --- |
| `UnknownReceivableTypeException`, `UnknownCurrencyException`, `InvalidPricingException` | `422 Unprocessable Entity` |
| `ReceivableConflictException` | `409 Conflict` |
| `ExchangeRateUnavailableException` | `503 Service Unavailable` |
| `MethodArgumentNotValidException` / `ConstraintViolationException` | `400 Bad Request` |

### 2.5 Persistência

- Sem migração nova: tabelas `tipo_recebivel` e `recebivel` existem desde `V1__init_schema.sql`; seeds de tipos em `V2__seed_reference_data.sql`.
- `RecebivelJpaEntity` mapeia `valor_face NUMERIC(19,4)`, `data_vencimento DATE`, `version` (locking otimista).
- Escala real da moeda obtida via `MoedaCatalog` (`moeda.codigo` → `escala`), não hardcoded.

### 2.6 Configuração (`application.yml`)

```yaml
app:
  precificacao:
    taxa-base: ${PRECIFICACAO_TAXA_BASE:0.0}
```

### 2.7 Testes

- Unitários: `PrecificacaoEngineTest` (fórmula, validações), `DuplicataMercantilStrategyTest`, `ChequePreDatadoStrategyTest`, `PrecificacaoStrategyResolverTest`, `RecebivelTest`, `SpreadTest`, `RecebivelQueryCriteriaTest`, `RecebivelCreatorTest`, `RecebivelQueryTest`, `PrecificacaoSimulatorTest`.
- Controller (MockMvc): `RecebivelControllerTest`, `SimulacaoControllerTest`.
- Persistência (Testcontainers PostgreSQL): `RecebivelRepositoryAdapterTest`, `MoedaCatalogAdapterTest`.
- Gateway: `CambioGatewayAdapterTest`.

---

## 2A. Módulo Liquidação (`com.srm.creditengine.liquidacao`)

### 2A.1 Responsabilidade

Liquidar um lote de recebíveis de forma atômica e idempotente: precifica cada recebível (reutilizando `PrecificacaoEngine` + `PrecificacaoStrategyResolver`), aplica câmbio quando a moeda de pagamento difere da do ativo, persiste o cabeçalho + itens em uma única transação e oferece consulta por id e extrato por período.

### 2A.2 Estrutura (arquitetura hexagonal)

- **Domínio**: `Liquidacao`, `ItemLiquidacao`, `StatusLiquidacao`, porta `RepositorioLiquidacao`; exceções `LiquidacaoConflictException` (idempotência), `LiquidacaoVersionConflictException` (concorrência), `LiquidacaoNotFoundException`, `RecebivelNotFoundException`.
- **Aplicação**: `LiquidarLote` (`@Transactional`, idempotência por `chaveIdempotencia`, precificação via estratégia, bump de versão por recebível), `ConsultarLiquidacao`.
- **Infraestrutura**: `LiquidacaoJpaEntity`/`LiquidacaoItemJpaEntity`, `LiquidacaoJpaRepository`, `LiquidacaoRepositoryAdapter` (também `tryBumpVersion` na porta `RecebivelRepository`), `LiquidacaoConfig`, `ExtratoController`/`LiquidacaoController`.

### 2A.3 Regras de negócio

- Cada item é precificado pelo `PrecificacaoEngine`; câmbio aplicado quando `moedaPagamento != moeda do ativo` (via `CambioGateway`).
- **Concorrência**: lock otimista por recebível — `tryBumpVersion(id, expectedVersion)` incrementa `version` apenas se ainda igual; 0 linhas → `LiquidacaoVersionConflictException` → 409. Sem retry automático: o cliente reprocessa com dados atuais (precificação é stateless). Detalhes no ADR `architecture_decision_records-optimistic_locking_liquidacao.md`.
- **Idempotência**: `chave_idempotencia` UNIQUE; chave duplicada → `LiquidacaoConflictException` → 409.
- `createdAt` controlado por aplicação (`Instant`).

### 2A.4 Contratos de API

- `POST /api/liquidacoes` — liquida um lote (201/400/409/422).
- `GET /api/liquidacoes/{id}` — consulta uma liquidação (200/404).
- `GET /api/liquidacoes/extrato?dataInicial=&dataFinal=&status=&cedente=&lastId=&limit=` — extrato por item com paginação por cursor (200/400).
- `GlobalExceptionHandler`: `LiquidacaoConflictException`/`LiquidacaoVersionConflictException` → 409, `LiquidacaoNotFoundException` → 404, `RecebivelNotFoundException` → 422.

### 2A.5 Persistência

- Tabelas `liquidacao`/`liquidacao_item` (V1) mapeadas em JPA com `@EntityGraph` para carregar itens; `version` no cabeçalho.
- Extrato via **SQL nativo** (`JdbcTemplate`) em `ConsultaLiquidacaoAdapter`: `li.id > :lastId ORDER BY li.id LIMIT :limit`, filtros por período/status/cedente, valores parametrizados.

### 2A.6 Testes

- Domínio/aplicação: `LiquidacaoTest`, `ItemLiquidacaoTest`, `LiquidarLoteTest`, `ConsultarLiquidacaoTest`.
- Web (MockMvc): `LiquidacaoControllerTest`, `ExtratoControllerTest`.
- Integração (Testcontainers): `LiquidacaoRepositoryAdapterTest` (inclui `tryBumpVersion`/conflito), `ConsultaLiquidacaoAdapterTest` (filtros + cursor).
- Contrato OpenAPI: `OpenApiContractTest`.

---

## 3. Stack e dependências

- Java 21, Spring Boot 3.5.16, Spring Cloud **2025.0.3** (Northfields, Boot 3.5.x) via BOM `spring-cloud-dependencies`.
- `spring-cloud-starter-openfeign` para integrações HTTP externas; `spring-cloud-starter-circuitbreaker-resilience4j` + `spring-retry` para resiliência; `wiremock-standalone` (3.13.2) como dependência de teste.
- `springdoc-openapi-starter-webmvc-ui` (2.8.17) com anotações `@Operation`/`@ApiResponse`/`@Tag` nos controllers (Swagger UI em `/swagger-ui.html`, spec em `/v3/api-docs`).
- JaCoCo ≥ 90% de cobertura de linha (atual: ~98%).
- Backend: 187 testes (unitários, web, integração, contrato), `./mvnw verify` BUILD SUCCESS.

## 4. Operação

- Banco: PostgreSQL; migrações via Flyway (`spring.flyway.locations=classpath:db/migration`); `ddl-auto: validate`.
- Porta configurável (`PORT`, default 8080); CORS por `CORS_ALLOWED_ORIGINS`.
- Perfis: o `application.yml` **não contém defaults de desenvolvimento** — banco (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`) e CORS (`CORS_ALLOWED_ORIGINS`) são obrigatórios via env; o perfil `local` (`application-local.yml`) fornece os valores de desenvolvimento (Postgres `localhost:5656`, CORS `http://localhost:5173`). Testes de contexto usam `@ActiveProfiles("test")` + `application-test.yml` para o CORS, com datasource via Testcontainers (`@DynamicPropertySource`).
- Secrets e credenciais apenas por variáveis de ambiente (`DB_*`, `BCB_PTAX_*`, `AWESOMEAPI_*`, `PRECIFICACAO_TAXA_BASE`).