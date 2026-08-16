# Plano de Implementação — Fase 1 (Fundação): Pontos 1, 2 e 3 do Domain

**Contexto**: Encerrado o contexto de banco de dados (schema via Flyway na inicialização da aplicação, migrações V1/V2 em português aplicadas). Nova solicitação: aplicar os **pontos 1, 2 e 3** do `.specs/domain.md` — fundação de domínio (`Dinheiro`), descoberta de bounded contexts e esqueleto hexagonal.

**Status**: AGUARDANDO APROVAÇÃO. Nenhum código será gerado até a aprovação deste plano.

**Classificação**: Mudança **complexa** (envolve múltiplos módulos/contextos e restruturação de pacotes) — por isso este plano é apresentado antes da implementação.

**Convenção de nomenclatura**: nomes de domínio em **português** (entidades, VOs, portas, casos de uso, endpoints, tabelas/colunas de negócio); campos técnicos (`id`, `version`, `created_at`, `BigDecimal`, `Instant`), **exceções e classes de infraestrutura** (handlers, DTOs, config) em inglês.

**Idioma de código**: identificadores, mensagens de erro, mensagens de validação e logs **sempre em inglês** (exceto palavras reservadas de domínio em português). Prosa de documentação e comunicação com o usuário permanecem em PT-BR.

---

## 1. Escopo de contexto de arquitetura (confirmado)

Os pontos 1–3 correspondem à **Fase 1 — Fundação** do roadmap (§8 do domain). A ordem é lógica:

1. **Ponto 1 — `Dinheiro`**: precisão numérica como fundamento de um sistema financeiro (`BigDecimal` + VO imutável + escala + `HALF_EVEN`, ADR-002);
2. **Ponto 2 — Bounded contexts**: decompor o problema em domínios independentes (`cambio`, `precificacao`, `liquidacao`, `extrato`) antes de qualquer código de negócio;
3. **Ponto 3 — Hexagonal**: esqueleto de camadas por módulo (`domain/application/infrastructure`) que embrulha o `Dinheiro` e prepara as fronteiras para a evolução a microserviços (ADR-001).

**Tratamento de exceções (Ponto 10, parte da Fase 1):** o domínio **lança** exceções de domínio; a tradução para HTTP é responsabilidade única do handler global `@ControllerAdvice` no adapter web. Nenhuma exceção é tratada, ignorada ou mapeada localmente no domínio.

Decisão de estrutura: **monolito modular por bounded context dentro de um único deployable**, com **kernel compartilhado** (`shared/`) para o `Dinheiro`/`CodigoMoeda` usado por todos os domínios. As portas já isolam as fronteiras (desacoplamento), mas a implementação das entidades/casos de uso de cada domínio fica para as Fases 2+ (Câmbio, Precificação, etc.), conforme o roadmap.

---

## 2. Ponto 1 — `Dinheiro` Value Object (kernel compartilhado)

### 2.1 Arquivos a criar

| Arquivo | Conteúdo |
| --- | --- |
| `backend/src/main/java/com/srm/creditengine/shared/domain/model/CodigoMoeda.java` | VO imutável de código de moeda ISO 4217 (3 letras maiúsculas) |
| `backend/src/main/java/com/srm/creditengine/shared/domain/model/Dinheiro.java` | VO monetário imutável: `valor` + `moeda` + `escala` |
| `backend/src/main/java/com/srm/creditengine/shared/domain/exception/DomainException.java` | Base das exceções de domínio (esperadas) |
| `backend/src/main/java/com/srm/creditengine/shared/domain/exception/IncompatibleCurrenciesException.java` | Exceção de domínio: soma/operação entre moedas distintas |
| `backend/src/main/java/com/srm/creditengine/infrastructure/adapter/in/web/GlobalExceptionHandler.java` | `@RestControllerAdvice` — traduz exceções em respostas HTTP (Ponto 10) |
| `backend/src/test/java/com/srm/creditengine/shared/domain/model/CodigoMoedaTest.java` | Testes unitários (JUnit) |
| `backend/src/test/java/com/srm/creditengine/shared/domain/model/DinheiroTest.java` | Testes unitários (JUnit) |
| `backend/src/test/java/com/srm/creditengine/infrastructure/adapter/in/web/GlobalExceptionHandlerTest.java` | Teste unitário do handler global |

### 2.2 `CodigoMoeda`

O construtor **delega a validação a um método validador local** (`validar`), que garante o invariante estrutural do tipo em qualquer caminho de construção (fronteira HTTP, testes, leituras de banco, eventos) — independente de framework. **Bean Validation (`@NotNull`/`@Pattern`) não é usado no domínio**: ele só é executado por um engine (via `@Valid` na fronteira HTTP) e não protege `new CodigoMoeda(...)` fora dela; além disso, a diretriz exige domínio independente de framework. Bean Validation pertence à fronteira HTTP (ver §2.5).

```java
package com.srm.creditengine.shared.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record CodigoMoeda(String codigo) {

    private static final Pattern ISO_ALPHA3 = Pattern.compile("[A-Z]{3}");

    public CodigoMoeda {
        validar(codigo);
    }

    private static void validar(String codigo) {
        Objects.requireNonNull(codigo, "codigo must not be null");
        if (!ISO_ALPHA3.matcher(codigo).matches()) {
            throw new IllegalArgumentException(
                "currency code must be 3 uppercase letters, but was: " + codigo);
        }
    }
}
```

### 2.3 `Dinheiro`

Cada operação **delega a validação a um método dedicado** e, depois, executa somente o que propõe (SRP). A validação de moedas distintas lança `IncompatibleCurrenciesException` (exceção de domínio **esperada**, traduzida pelo handler global — não um erro local).

```java
package com.srm.creditengine.shared.domain.model;

import com.srm.creditengine.shared.domain.exception.IncompatibleCurrenciesException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Dinheiro(BigDecimal valor, CodigoMoeda moeda, int escala) {

    public Dinheiro {
        Objects.requireNonNull(valor, "valor must not be null");
        Objects.requireNonNull(moeda, "moeda must not be null");
        if (escala < 0) {
            throw new IllegalArgumentException(
                "escala must be non-negative, but was: " + escala);
        }
        valor = valor.setScale(escala, RoundingMode.HALF_EVEN);
    }

    public Dinheiro somar(Dinheiro outro) {
        validarMesmaMoeda(outro);
        return new Dinheiro(valor.add(outro.valor), moeda, escala);
    }

    public Dinheiro multiplicar(BigDecimal fator) {
        validarFator(fator);
        return new Dinheiro(valor.multiply(fator), moeda, escala);
    }

    private void validarMesmaMoeda(Dinheiro outro) {
        if (!moeda.equals(outro.moeda)) {
            throw new IncompatibleCurrenciesException(moeda, outro.moeda);
        }
    }

    private void validarFator(BigDecimal fator) {
        Objects.requireNonNull(fator, "fator must not be null");
    }
}
```

### 2.4 Exceções de domínio

Exceções são artefatos técnicos (não objetos de domínio), portanto em **inglês** — em linha com `domain.md` §2.10 (`DomainException`, `ValidationException`, `OptimisticLockException`).

```java
package com.srm.creditengine.shared.domain.exception;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
```

```java
package com.srm.creditengine.shared.domain.exception;

import com.srm.creditengine.shared.domain.model.CodigoMoeda;

public class IncompatibleCurrenciesException extends DomainException {

    public IncompatibleCurrenciesException(CodigoMoeda first, CodigoMoeda second) {
        super("Cannot operate on amounts of different currencies: "
            + first + " and " + second);
    }
}
```

### 2.5 Handler global (`GlobalExceptionHandler`) — Ponto 10

Único ponto de tradução exceção → HTTP. Exceções **esperadas** ganham status semântico e mensagem clara; **inesperadas** viram `500` genérico com `requestId` e log estruturado, sem expor detalhes internos. A validação de **entrada na fronteira HTTP** usa Bean Validation (`@NotNull`, `@Pattern`) nos DTOs de request → `MethodArgumentNotValidException` → `400` com o campo e a mensagem, traduzida aqui. O domínio permanece framework-free (§2.2).

```java
package com.srm.creditengine.infrastructure.adapter.in.web;

import com.srm.creditengine.shared.domain.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorBody> handleDomainException(DomainException ex) {
        return ResponseEntity.unprocessableEntity()
            .body(new ErrorBody(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorBody(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleUnexpected(Exception ex, HttpServletRequest req) {
        String requestId = UUID.randomUUID().toString();
        log.error("requestId={} unhandled error on {}", requestId, req.getRequestURI(), ex);
        return ResponseEntity.internalServerError()
            .body(new ErrorBody("Unexpected internal error."));
    }

    public record ErrorBody(String message) {
    }
}
```

### 2.6 Regras de negócio cobertas por teste

- `CodigoMoeda`: aceita `BRL`/`USD`; rejeita `null`, minúsculas, tamanho ≠ 3, dígitos (validação delegada a `validar`);
- `Dinheiro`: rejeita `valor` nulo, `moeda` nula, `escala` negativa; aplica escala com `HALF_EVEN`; `somar` **delega** a validação (`validarMesmaMoeda`): mesma moeda soma, moedas distintas → `IncompatibleCurrenciesException`; `multiplicar` **delega** a validação (`validarFator`): fator nulo → `NullPointerException`, fator válido multiplica preservando moeda/escala;
- `GlobalExceptionHandler`: `DomainException` → `422` com mensagem; `MethodArgumentNotValidException` → `400` com campo e mensagem; inesperada → `500` com corpo genérico (sem detalhes internos).

### 2.7 Justificativa (ADR-002) e decisão de precisão

Sem `double`/`float` para dinheiro; `BigDecimal` com escala e arredondamento explícitos; `HALF_EVEN` (banker's rounding) evita viés sistemático em muitas operações.

---

## 3. Ponto 2 — Bounded contexts (restruturação de pacotes)

### 3.1 Estrutura-alvo de `com.srm.creditengine`

```
backend/src/main/java/com/srm/creditengine/
├── CreditEngineApplication.java          (mantém)
├── shared/                               (kernel compartilhado)
│   ├── domain/model/
│   │   ├── CodigoMoeda.java
│   │   └── Dinheiro.java
│   └── domain/exception/
│       ├── DomainException.java
│       └── IncompatibleCurrenciesException.java
├── cambio/                               (Câmbio)
│   ├── domain/                           (modelo + portas — Fase 2)
│   ├── application/                      (casos de uso — Fase 2)
│   └── infrastructure/                   (adapters — Fase 2)
├── precificacao/                         (Precificação)
│   ├── domain/                           (Fase 3)
│   ├── application/                      (Fase 3)
│   └── infrastructure/                   (Fase 3)
├── liquidacao/                           (Liquidação)
│   ├── domain/                           (Fase 4)
│   ├── application/                      (Fase 4)
│   └── infrastructure/                   (Fase 4)
├── extrato/                              (Extrato — Fase 5)
│   ├── application/
│   └── infrastructure/
└── infrastructure/                       (adapters globais atuais)
    ├── adapter/in/web/
    │   ├── HealthController.java         (mantém)
    │   └── GlobalExceptionHandler.java
    ├── config/CorsConfig.java            (mantém)
    └── config/OpenApiConfig.java         (mantém)
```

### 3.2 Alterações na estrutura existente

| Ação | Caminho |
| --- | --- |
| Remover | `application/port/in/.gitkeep` e `application/port/out/.gitkeep` (estrutura plana antiga) |
| Remover | `domain/model/.gitkeep` (estrutura plana antiga) |
| Criar | Diretórios por contexto (`cambio/`, `precificacao/`, `liquidacao/`, `extrato/`) com `.gitkeep` em `domain/`, `application/`, `infrastructure/` |
| Manter | `infrastructure/` raiz (HealthController, config) e `CreditEngineApplication.java` |

### 3.3 Mapeamento domínio ↔ módulo (do §2.2/§3 do domain)

| Módulo | Responsabilidade | Fase de implementação |
| --- | --- | --- |
| `cambio` | Câmbio: taxas, conversão, histórico | Fase 2 |
| `precificacao` | Precificação: deságio, spreads, strategy | Fase 3 |
| `liquidacao` | Liquidação: ACID, optimistic locking, idempotência | Fase 4 |
| `extrato` | Extrato: SQL nativo, cursor, filtros | Fase 5 |

---

## 4. Ponto 3 — Esqueleto hexagonal por módulo

Cada módulo de negócio adota o layout hexagonal do §2.3 do domain:

```
domain/        -> entidades, value objects, regras, portas (interfaces)
application/   -> casos de uso, orquestração, serviços de aplicação
infrastructure -> adapters (web, persistência), configuração
```

Nesta fase cria-se apenas o **esqueleto** (estrutura de diretórios com `.gitkeep`). As portas de saída (`ProvedorTaxaCambio`, `RepositorioTaxaCambio`, `EstrategiaPrecificacao`, etc.) e os casos de uso serão adicionados nas respectivas fases, evitando abstrações sem necessidade concreta (conforme `.agents/backend.md`).

Regras de desacoplamento já documentadas e a preservar (§2.3/§3 do domain):
- Domínio não conhece JPA, Spring ou HTTP;
- Conversão cambial nunca dentro de `precificacao` — a Precificação consulta o Câmbio por porta;
- a Liquidação orquestra Precificação e Câmbio por portas;
- o Extrato não passa pela camada de negócio, mas passa pela de aplicação;
- exceções de domínio são lançadas no domínio e traduzidas apenas pelo handler global no adapter web.

---

## 5. Verificação

- `cd backend && ./mvnw verify` — build, testes unitários (CodigoMoeda/Dinheiro/GlobalExceptionHandler) e gate JaCoCo ≥ 90%;
- `git status` limpo dos arquivos órfãos (estrutura plana removida);
- Aplicação continua subindo com schema via Flyway (não afetado);
- `/api/health` continua `200 {"status":"UP"}`.