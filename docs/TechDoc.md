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

- `BcbPtaxClient` (`@FeignClient`, url de `app.cambio.provider.base-url`) chama a função OData `CotacaoMoedaPeriodo` do BCB com assinatura real `(moeda, dataInicial, dataFinalCotacao)` (validada no `$metadata`).
- `BcbPtaxTaxaCambioProvider` traduz o par em consulta, usa a cotação de **Fechamento mais recente** (`cotacaoVenda`) e aplica a orientação do par:
  - `ParMoedas(USD, BRL)` → taxa = cotação de venda do USD em BRL;
  - `ParMoedas(BRL, USD)` → taxa = `1 / cotação`;
  - Par sem a moeda cotada configurada (`quote-currency`, default `USD`) → `Optional.empty()`.
- `dataHoraCotacao` (com microssegundos, ex.: `2026-08-13 10:08:11.052389`) é normalizado para ISO e convertido para `Instant` no fuso `America/Sao_Paulo`.
- Query params OData iniciados com `@` são enviados via `@RequestParam Map<String, String>` (o `SpringMvcContract` do Feign não expande nomes com `@` como template).
- Timeout configurado no Feign; falha/indisponibilidade do provedor (`FeignException`) → `ExchangeRateProviderUnavailableException` → degradação graciosa com `503`.

### 1.6 Persistência

- Sem migração nova: tabelas `moeda` e `taxa_cambio` existem desde `V1__init_schema.sql`; seed BRL/USD em `V2__seed_reference_data.sql`.
- Consulta de taxa vigente: `findFirstByCodigoBaseAndCodigoCotacaoAndVigenciaLessThanEqualOrderByVigenciaDesc`, suportada pelo índice `idx_taxa_cambio_par_vigencia`.
- `MoedaRepositoryAdapter` valida a existência da moeda (`moeda` PK por `codigo`).

### 1.7 Configuração (`application.yml`)

```yaml
app:
  cambio:
    provider:
      base-url: ${BCB_PTAX_BASE_URL:https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata}
      timeout-ms: ${BCB_PTAX_TIMEOUT_MS:5000}
      quote-currency: ${BCB_PTAX_QUOTE_CURRENCY:USD}
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: ${app.cambio.provider.timeout-ms}
            read-timeout: ${app.cambio.provider.timeout-ms}
```

### 1.8 Testes

- Unitários: `ParMoedasTest`, `TaxaCambioTest`, `TaxaCambioUpdaterTest`, `TaxaVigenteReaderTest`, `DinheiroConverterTest`, `TaxaCambioOrchestratorTest`, `BcbPtaxTaxaCambioProviderTest` (Mockito).
- Contrato HTTP (WireMock): `BcbPtaxClientContractTest`.
- Controller (MockMvc): `TaxaCambioControllerTest`.
- Persistência (Testcontainers PostgreSQL): `TaxaCambioRepositoryAdapterTest`.
- Handler global: `GlobalExceptionHandlerTest` (409/404/503/400).

---

## 2. Stack e dependências

- Java 21, Spring Boot 3.5.16, Spring Cloud **2025.0.3** (Northfields, Boot 3.5.x) via BOM `spring-cloud-dependencies`.
- `spring-cloud-starter-openfeign` para integrações HTTP externas; `wiremock-standalone` (3.13.2) como dependência de teste.
- JaCoCo ≥ 90% de cobertura de linha (atual: ~99%).

## 3. Operação

- Banco: PostgreSQL; migrações via Flyway (`spring.flyway.locations=classpath:db/migration`); `ddl-auto: validate`.
- Porta configurável (`PORT`, default 8080); CORS por `CORS_ALLOWED_ORIGINS`.
- Secrets e credenciais apenas por variáveis de ambiente (`DB_*`, `BCB_PTAX_*`).