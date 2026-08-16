# Plano de Implementação — Ponto 4 (Motor de Câmbio) do Domain

**Contexto**: Encerrado e commitado o plano dos Pontos 1–3 (fundação: `Dinheiro`, bounded contexts, esqueleto hexagonal, handler global de exceções). Nova solicitação: aplicar o **Ponto 4 — Motor de Câmbio** do `.specs/domain.md` (§2.4 e §3.1), com provedor de taxas **real (BCB PTAX)** em vez do mock, conforme decisão explícita do usuário.

**Status**: APROVADO E IMPLEMENTADO (revisão v6). Build validado com `./mvnw verify` (69 testes, JaCoCo ~99%).

**Classificação**: Mudança **complexa** (novo módulo de negócio `cambio`, contratos de API, persistência JPA, integração HTTP externa) — por isso este plano é apresentado antes da implementação.

**Convenção de nomenclatura — palavra reservada de domínio em português, todo o resto em inglês**: nomes de domínio em **português** (`ParMoedas`, `TaxaCambio`); **infraestrutura, casos de uso, exceções, DTOs, classes técnicas e propriedades de configuração em inglês**, com a palavra de domínio em PT quando o artefato a referencia (`TaxaCambioUpdater`, `TaxaVigenteReader`, `DinheiroConverter`, `TaxaCambioOrchestrator`, `TaxaCambioProvider`, `TaxaCambioRepository`, `MoedaRepositoryAdapter`, `BcbPtaxTaxaCambioProvider`). O mesmo vale para qualquer artefato: `TipoMoeda` → `MoedaType`, `RepositorioMoeda` → `MoedaRepository`, `MoedaRepositorioAdapter` → `MoedaRepositoryAdapter`.

**Idioma de código**: identificadores, mensagens de erro, mensagens de validação e logs **sempre em inglês** (exceto palavras reservadas de domínio em português). Prosa de documentação e comunicação com o usuário permanecem em PT-BR.

---

## 1. Escopo (alinhado ao índice do domain)

O Ponto 4 (§2.4) exige que o domínio de Câmbio anteceda qualquer cross-currency:

- Armazenar taxas por par de moedas (histórico por vigência);
- Prover a **taxa vigente** (par + data máxima ≤ referência);
- Permitir **atualização manual** e **integração externa**;
- Converter valores entre moedas com arredondamento controlado.

O §3.1 define o domínio: modelo (`ParMoedas`, `TaxaCambio`), regras (moeda existente, taxa positiva, histórico, conversão `base * taxa` e inversa `1 / taxa`), portas de saída (`TaxaCambioProvider`, `TaxaCambioRepository`) e casos de uso (`TaxaCambioUpdater`, `TaxaVigenteReader`, `DinheiroConverter`).

**Decisão do provedor**: `TaxaCambioProvider` será implementado por um adapter **real BCB PTAX** (`BcbPtaxTaxaCambioProvider`) — cotação oficial diária do Banco Central (fonte autoritativa e auditável, sem chave de API). A chamada HTTP usa **Feign Client** (diretriz do `.agents/backend.md`: "Integrações com Feign Client"). O domínio permanece inalterado em relação à porta; apenas a implementação do adapter deixa de ser mock. Timeout configurado via Feign e degradação graciosa (503). Circuit breaker (Ponto 13) fica documentado como evolução, **sem** adicionar Resilience4j nesta etapa (KISS).

---

## 2. Estrutura-alvo no módulo `cambio`

```
cambio/
├── domain/
│   ├── ParMoedas.java                     (VO: base, cotacao)
│   ├── TaxaCambio.java                    (entidade: par, taxa, vigencia)
│   ├── MoedaRepository.java            (porta de saída)
│   ├── TaxaCambioProvider.java          (porta de saída)
│   ├── TaxaCambioRepository.java        (porta de saída)
│   └── exception/
│       ├── UnknownCurrencyException.java
│       ├── ExchangeRateConflictException.java
│       ├── ExchangeRateNotFoundException.java
│       └── ExchangeRateProviderUnavailableException.java
├── application/
│   ├── TaxaCambioUpdater.java          (caso de uso: atualiza/grava taxa manual)
│   ├── TaxaVigenteReader.java          (caso de uso: lê taxa vigente)
│   ├── DinheiroConverter.java          (caso de uso: converte dinheiro)
│   └── TaxaCambioOrchestrator.java     (caso de uso: orquestra provedor → repositório)
└── infrastructure/
    ├── adapter/in/web/
    │   └── TaxaCambioController.java
    ├── adapter/out/persistence/
    │   ├── MoedaJpaEntity.java
    │   ├── MoedaJpaRepository.java
    │   ├── MoedaRepositoryAdapter.java
    │   ├── TaxaCambioJpaEntity.java
    │   ├── TaxaCambioJpaRepository.java
    │   └── TaxaCambioRepositoryAdapter.java
    ├── adapter/out/external/
    │   ├── BcbPtaxClient.java
    │   └── BcbPtaxTaxaCambioProvider.java
    └── config/
        └── TaxaCambioProviderConfig.java
```

Remover: `cambio/domain/.gitkeep`, `cambio/application/.gitkeep`, `cambio/infrastructure/.gitkeep`.

### 2.1 Refatoração de nomenclatura no código existente (Fase 1)

Revisão do código atual (shared) para a mesma convenção (palavra reservada de domínio em PT, resto em inglês). Identificadores técnicos ainda em PT a renomear:

- `CodigoMoeda`: método `validar(String codigo)` → `validate(String codigo)` (palavra de domínio `codigo` permanece).
- `Dinheiro`: `somar(Dinheiro outro)` → `add(Dinheiro other)`; `multiplicar(BigDecimal fator)` → `multiply(BigDecimal factor)`; `validarMesmaMoeda` → `validateSameMoeda`; `validarFator` → `validateFactor`; mensagens `"fator must not be null"` → `"factor must not be null"`.
- Testes correspondentes: `DinheiroTest` atualizar chamadas `somar`/`multiplicar` e o caso `rejectsNullFator` → `rejectsNullFactor`.
- `GlobalExceptionHandler`: sem palavras PT; manter como está.

Nomes de domínio (`valor`, `moeda`, `escala`, `codigo`) permanecem em PT por serem vocabulário reservado do domínio.

---

## 3. Domínio (framework-free)

### 3.1 `ParMoedas` — VO

```java
package com.srm.creditengine.cambio.domain;

import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.util.Objects;

public record ParMoedas(CodigoMoeda base, CodigoMoeda cotacao) {

    public ParMoedas {
        Objects.requireNonNull(base, "base must not be null");
        Objects.requireNonNull(cotacao, "cotacao must not be null");
        if (base.equals(cotacao)) {
            throw new IllegalArgumentException("base and cotacao must differ, but both were: " + base);
        }
    }

    public boolean contem(CodigoMoeda moeda) {
        return base.equals(moeda) || cotacao.equals(moeda);
    }
}
```

### 3.2 `TaxaCambio` — entidade

O construtor delega a validação a `validate` (invariantes estruturais nascem válidas, conforme §0.2 do domain). Taxa persistida com escala 8 (`NUMERIC(19,8)` no banco).

```java
package com.srm.creditengine.cambio.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

public class TaxaCambio {

    private final ParMoedas par;
    private final BigDecimal taxa;
    private final Instant vigencia;

    public TaxaCambio(ParMoedas par, BigDecimal taxa, Instant vigencia) {
        validate(par, taxa, vigencia);
        this.par = par;
        this.taxa = taxa.setScale(8, RoundingMode.HALF_EVEN);
        this.vigencia = vigencia;
    }

    private void validate(ParMoedas par, BigDecimal taxa, Instant vigencia) {
        Objects.requireNonNull(par, "par must not be null");
        Objects.requireNonNull(taxa, "taxa must not be null");
        Objects.requireNonNull(vigencia, "vigencia must not be null");
        if (taxa.signum() <= 0) {
            throw new IllegalArgumentException("taxa must be positive, but was: " + taxa);
        }
    }

    public ParMoedas par() { return par; }
    public BigDecimal taxa() { return taxa; }
    public Instant vigencia() { return vigencia; }
}
```

### 3.3 Portas de saída

```java
package com.srm.creditengine.cambio.domain;

import com.srm.creditengine.shared.domain.model.CodigoMoeda;

public interface MoedaRepository {
    boolean exists(CodigoMoeda moeda);
}
```

```java
package com.srm.creditengine.cambio.domain;

import java.util.Optional;

public interface TaxaCambioProvider {
    Optional<TaxaCambio> obtain(ParMoedas par);
}
```

```java
package com.srm.creditengine.cambio.domain;

import java.time.Instant;
import java.util.Optional;

public interface TaxaCambioRepository {
    Optional<TaxaCambio> obtainVigente(ParMoedas par, Instant reference);
    boolean existsVigencia(ParMoedas par, Instant vigencia);
    void save(TaxaCambio taxa);
}
```

### 3.4 Exceções de domínio (inglês)

```java
package com.srm.creditengine.cambio.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class UnknownCurrencyException extends DomainException {
    public UnknownCurrencyException(String codigo) {
        super("currency code does not exist: " + codigo);
    }
}
```

```java
package com.srm.creditengine.cambio.domain.exception;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.shared.domain.exception.DomainException;
import java.time.Instant;

public class ExchangeRateConflictException extends DomainException {
    public ExchangeRateConflictException(ParMoedas par, Instant vigencia) {
        super("exchange rate already exists for pair " + par + " at " + vigencia);
    }
}
```

```java
package com.srm.creditengine.cambio.domain.exception;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.shared.domain.exception.DomainException;

public class ExchangeRateNotFoundException extends DomainException {
    public ExchangeRateNotFoundException(ParMoedas par) {
        super("no exchange rate found for pair " + par);
    }
}
```

```java
package com.srm.creditengine.cambio.domain.exception;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.shared.domain.exception.DomainException;

public class ExchangeRateProviderUnavailableException extends DomainException {
    public ExchangeRateProviderUnavailableException(ParMoedas par) {
        super("external exchange rate provider unavailable for pair " + par);
    }
}
```

---

## 4. Casos de uso (application, sem anotações Spring)

### 4.1 `TaxaCambioUpdater` (manual)

Valida moedas existentes (via `MoedaRepository`), delega os invariantes de `TaxaCambio` ao construtor e detecta conflito de vigência → `409` (`ExchangeRateConflictException`). Métodos de validação dedicados (SRP, §0.2).

```java
package com.srm.creditengine.cambio.application;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.MoedaRepository;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateConflictException;
import com.srm.creditengine.cambio.domain.exception.UnknownCurrencyException;
import java.math.BigDecimal;
import java.time.Instant;

public class TaxaCambioUpdater {

    private final TaxaCambioRepository repository;
    private final MoedaRepository moedaRepository;

    public TaxaCambioUpdater(TaxaCambioRepository repository, MoedaRepository moedaRepository) {
        this.repository = repository;
        this.moedaRepository = moedaRepository;
    }

    public TaxaCambio update(ParMoedas par, BigDecimal taxa, Instant vigencia) {
        validateMoedas(par);
        TaxaCambio taxaCambio = new TaxaCambio(par, taxa, vigencia);
        validateVigenciaAvailable(par, taxaCambio.vigencia());
        repository.save(taxaCambio);
        return taxaCambio;
    }

    private void validateMoedas(ParMoedas par) {
        validateMoeda(par.base());
        validateMoeda(par.cotacao());
    }

    private void validateMoeda(CodigoMoeda moeda) {
        if (!moedaRepository.exists(moeda)) {
            throw new UnknownCurrencyException(moeda.codigo());
        }
    }

    private void validateVigenciaAvailable(ParMoedas par, Instant vigencia) {
        if (repository.existsVigencia(par, vigencia)) {
            throw new ExchangeRateConflictException(par, vigencia);
        }
    }
}
```

### 4.2 `TaxaVigenteReader`

```java
package com.srm.creditengine.cambio.application;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import java.time.Instant;
import java.util.Optional;

public class TaxaVigenteReader {

    private final TaxaCambioRepository repository;

    public TaxaVigenteReader(TaxaCambioRepository repository) {
        this.repository = repository;
    }

    public Optional<TaxaCambio> read(ParMoedas par, Instant reference) {
        return repository.obtainVigente(par, reference);
    }
}
```

### 4.3 `DinheiroConverter`

Conversão de acordo com a orientação do par (regra do §3.1): `base * taxa` → cotação; inversa usa `1 / taxa`. O par deve conter a moeda do valor; caso contrário, `IncompatibleCurrenciesException`.

```java
package com.srm.creditengine.cambio.application;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.shared.domain.exception.IncompatibleCurrenciesException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class DinheiroConverter {

    public Dinheiro convert(Dinheiro valor, TaxaCambio taxaCambio) {
        ParMoedas par = taxaCambio.par();
        validateParContem(valor.moeda(), par);
        BigDecimal factor = factorFor(valor.moeda(), par, taxaCambio.taxa());
        CodigoMoeda target = otherMoeda(valor.moeda(), par);
        return new Dinheiro(valor.valor().multiply(factor), target, valor.escala());
    }

    private void validateParContem(CodigoMoeda moeda, ParMoedas par) {
        if (!par.contem(moeda)) {
            throw new IncompatibleCurrenciesException(moeda, par.cotacao());
        }
    }

    private BigDecimal factorFor(CodigoMoeda moeda, ParMoedas par, BigDecimal taxa) {
        if (moeda.equals(par.base())) {
            return taxa;
        }
        return BigDecimal.ONE.divide(taxa, 12, RoundingMode.HALF_EVEN);
    }

    private CodigoMoeda otherMoeda(CodigoMoeda moeda, ParMoedas par) {
        return moeda.equals(par.base()) ? par.cotacao() : par.base();
    }
}
```

### 4.4 `TaxaCambioOrchestrator` (busca real + persistência)

Orquestra `TaxaCambioProvider` (BCB PTAX) → `TaxaCambioRepository`. Sem conflito: se a vigência obtida ainda não existe, salva; idempotente por vigência (regra do §2.4).

```java
package com.srm.creditengine.cambio.application;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambioProvider;
import com.srm.creditengine.cambio.domain.MoedaRepository;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateProviderUnavailableException;
import com.srm.creditengine.cambio.domain.exception.UnknownCurrencyException;
import java.util.Optional;

public class TaxaCambioOrchestrator {

    private final TaxaCambioProvider provider;
    private final TaxaCambioRepository repository;
    private final MoedaRepository moedaRepository;

    public TaxaCambioOrchestrator(TaxaCambioProvider provider, TaxaCambioRepository repository, MoedaRepository moedaRepository) {
        this.provider = provider;
        this.repository = repository;
        this.moedaRepository = moedaRepository;
    }

    public TaxaCambio orchestrate(ParMoedas par) {
        validateMoedas(par);
        Optional<TaxaCambio> taxa = provider.obtain(par);
        if (taxa.isEmpty()) {
            throw new ExchangeRateProviderUnavailableException(par);
        }
        TaxaCambio obtained = taxa.get();
        if (!repository.existsVigencia(par, obtained.vigencia())) {
            repository.save(obtained);
        }
        return obtained;
    }

    private void validateMoedas(ParMoedas par) {
        if (!moedaRepository.exists(par.base())) {
            throw new UnknownCurrencyException(par.base().codigo());
        }
        if (!moedaRepository.exists(par.cotacao())) {
            throw new UnknownCurrencyException(par.cotacao().codigo());
        }
    }
}
```

---

## 5. Adapter externo — Feign Client + `BcbPtaxTaxaCambioProvider`

A integração com o serviço OData **PTAX** do Banco Central usa **Feign Client** (conforme `.agents/backend.md`). Duas peças em `adapter/out/external/`:

### 5.1 `BcbPtaxClient` — interface Feign (contrato HTTP)

```java
package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "bcbPtax", url = "${app.cambio.provider.base-url}")
public interface BcbPtaxClient {

    @GetMapping("/CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)")
    PtaxResponse queryCotacao(
        @RequestParam("@moeda") String moeda,
        @RequestParam("@dataInicial") String startDate,
        @RequestParam("@dataFinalCotacao") String endDate,
        @RequestParam("$format") String format,
        @RequestParam("$select") String select);
}
```

> Nota (validação contra o `$metadata` real do BCB): a assinatura da função `CotacaoMoedaPeriodo` é `(moeda, dataInicial, dataFinalCotacao)` — o segundo parâmetro é `dataInicial` (não `dataInicialCotacao`). No Feign, os `@RequestParam` com nome iniciado por `@` não são expandidos pelo `SpringMvcContract`; por isso a implementação final usa um único `@RequestParam Map<String, String>` (ver código real). O `dataHoraCotacao` do PTAX tem microssegundos (`yyyy-MM-dd HH:mm:ss.SSSSSS`) e é normalizado para ISO antes do parse.

### 5.2 `BcbPtaxTaxaCambioProvider` — traduz o par em consulta e monta o `TaxaCambio`

Consulta o PTAX nos últimos dias úteis, usa a cotação de **Fechamento** e aplica a orientação do par:

- `ParMoedas(USD, BRL)` → taxa = cotação de venda do USD em BRL;
- `ParMoedas(BRL, USD)` → taxa = `1 / cotação`;
- Par sem a moeda cotada configurada (`quote-currency`, default `USD`) → `Optional.empty()`.

Mensagens e formato: JSON `{"value":[{"cotacaoVenda":...,"dataHoraCotacao":"yyyy-MM-dd HH:mm:ss.SSSSSS","tipoBoletim":"Fechamento"}]}` (com microssegundos). O `dataHoraCotacao` é convertido para `Instant` no fuso `America/Sao_Paulo`; entre os `Fechamento` do período, é usado o **mais recente**.

```java
package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.TaxaCambioProvider;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class BcbPtaxTaxaCambioProvider implements TaxaCambioProvider {

    private static final String MOEDA_BRL = "BRL";
    private static final DateTimeFormatter PTAX_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final DateTimeFormatter PTAX_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final BcbPtaxClient client;
    private final String quoteCurrency;
    private final ZoneId timeZone;
    private final int retroactiveDays;

    public BcbPtaxTaxaCambioProvider(BcbPtaxClient client, String quoteCurrency, ZoneId timeZone, int retroactiveDays) {
        this.client = client;
        this.quoteCurrency = quoteCurrency;
        this.timeZone = timeZone;
        this.retroactiveDays = retroactiveDays;
    }

    @Override
    public Optional<TaxaCambio> obtain(ParMoedas par) {
        if (!supports(par)) {
            return Optional.empty();
        }
        LocalDate end = LocalDate.now(timeZone);
        LocalDate start = end.minusDays(retroactiveDays);
        PtaxResponse response = client.queryCotacao(
            "'" + quoteCurrency + "'",
            "'" + PTAX_DATE_FORMAT.format(start) + "'",
            "'" + PTAX_DATE_FORMAT.format(end) + "'",
            "json",
            "cotacaoVenda,dataHoraCotacao,tipoBoletim");
        if (response == null || response.value() == null || response.value().isEmpty()) {
            return Optional.empty();
        }
        PtaxQuote quote = response.value().stream()
            .filter(c -> "Fechamento".equals(c.tipoBoletim()))
            .findFirst()
            .orElse(response.value().get(0));
        return Optional.of(new TaxaCambio(par, rateFor(par, quote), parseTimestamp(quote.dataHoraCotacao())));
    }

    private boolean supports(ParMoedas par) {
        return par.contem(new CodigoMoeda(MOEDA_BRL))
            && (par.base().codigo().equals(quoteCurrency) || par.cotacao().codigo().equals(quoteCurrency));
    }

    private BigDecimal rateFor(ParMoedas par, PtaxQuote quote) {
        BigDecimal sellRate = new BigDecimal(quote.cotacaoVenda());
        if (par.base().codigo().equals(quoteCurrency)) {
            return sellRate;
        }
        return BigDecimal.ONE.divide(sellRate, 8, RoundingMode.HALF_EVEN);
    }

    private Instant parseTimestamp(String value) {
        return LocalDateTime.parse(value, PTAX_TIMESTAMP_FORMAT).atZone(timeZone).toInstant();
    }

    public record PtaxResponse(List<PtaxQuote> value) {}

    public record PtaxQuote(String cotacaoVenda, String dataHoraCotacao, String tipoBoletim) {}
}
```

---

## 6. Persistência JPA (adapta a tabela `taxa_cambio` e `moeda` existentes)

Nenhuma migração nova: as tabelas `moeda` e `taxa_cambio` já existem (V1) e o índice `idx_taxa_cambio_par_vigencia` já suporta a consulta de taxa vigente.

### 6.1 `TaxaCambioJpaEntity` + `TaxaCambioJpaRepository`

```java
package com.srm.creditengine.cambio.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "taxa_cambio")
public class TaxaCambioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_base", nullable = false, length = 3)
    private String codigoBase;

    @Column(name = "codigo_cotacao", nullable = false, length = 3)
    private String codigoCotacao;

    @Column(name = "taxa", nullable = false, precision = 19, scale = 8)
    private BigDecimal taxa;

    @Column(name = "vigencia", nullable = false)
    private Instant vigencia;

    protected TaxaCambioJpaEntity() {}

    public TaxaCambioJpaEntity(String codigoBase, String codigoCotacao, BigDecimal taxa, Instant vigencia) {
        this.codigoBase = codigoBase;
        this.codigoCotacao = codigoCotacao;
        this.taxa = taxa;
        this.vigencia = vigencia;
    }

    public Long getId() { return id; }
    public String getCodigoBase() { return codigoBase; }
    public String getCodigoCotacao() { return codigoCotacao; }
    public BigDecimal getTaxa() { return taxa; }
    public Instant getVigencia() { return vigencia; }
}
```

```java
package com.srm.creditengine.cambio.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxaCambioJpaRepository extends JpaRepository<TaxaCambioJpaEntity, Long> {

    boolean existsByCodigoBaseAndCodigoCotacaoAndVigencia(String codigoBase, String codigoCotacao, Instant vigencia);

    Optional<TaxaCambioJpaEntity> findFirstByCodigoBaseAndCodigoCotacaoAndVigenciaLessThanEqualOrderByVigenciaDesc(
        String codigoBase, String codigoCotacao, Instant reference);
}
```

### 6.2 `TaxaCambioRepositoryAdapter`

```java
package com.srm.creditengine.cambio.infrastructure.adapter.out.persistence;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TaxaCambioRepositoryAdapter implements TaxaCambioRepository {

    private final TaxaCambioJpaRepository jpaRepository;

    public TaxaCambioRepositoryAdapter(TaxaCambioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<TaxaCambio> obtainVigente(ParMoedas par, Instant reference) {
        return jpaRepository
            .findFirstByCodigoBaseAndCodigoCotacaoAndVigenciaLessThanEqualOrderByVigenciaDesc(
                par.base().codigo(), par.cotacao().codigo(), reference)
            .map(this::toDomain);
    }

    @Override
    public boolean existsVigencia(ParMoedas par, Instant vigencia) {
        return jpaRepository.existsByCodigoBaseAndCodigoCotacaoAndVigencia(
            par.base().codigo(), par.cotacao().codigo(), vigencia);
    }

    @Override
    public void save(TaxaCambio taxa) {
        jpaRepository.save(new TaxaCambioJpaEntity(
            taxa.par().base().codigo(), taxa.par().cotacao().codigo(), taxa.taxa(), taxa.vigencia()));
    }

    private TaxaCambio toDomain(TaxaCambioJpaEntity entity) {
        return new TaxaCambio(
            new ParMoedas(new CodigoMoeda(entity.getCodigoBase()), new CodigoMoeda(entity.getCodigoCotacao())),
            entity.getTaxa(), entity.getVigencia());
    }
}
```

### 6.3 `MoedaJpaEntity`, `MoedaJpaRepository`, `MoedaRepositoryAdapter`

Mesmo padrão, mapeando a tabela `moeda` (coluna `codigo`), para validar que a moeda existe antes de aceitar taxa.

---

## 7. Adapter web — `TaxaCambioController`

Contratos (API First, §2.8 do domain) — atualização manual e endpoints de consulta/leitura da taxa vigente, integração e conversão:

| Operação | Método | Sucesso | Erro |
| --- | --- | --- | --- |
| Atualizar taxa manualmente | `PUT /api/taxas-cambio` | `200` | `400`, `409`, `422` |
| Obter taxa vigente | `GET /api/taxas-cambio/vigente?codigoBase=USD&codigoCotacao=BRL` | `200` | `400`, `404` |
| Integrar taxa do provedor (BCB PTAX) | `POST /api/taxas-cambio/integracao?codigoBase=USD&codigoCotacao=BRL` | `200` | `400`, `422`, `503` |
| Converter valor entre moedas | `POST /api/taxas-cambio/convert` | `200` | `400`, `404`, `422` |

Bean Validation apenas nos DTOs de request (fronteira HTTP), mensagens em inglês. O controller delega toda regra aos casos de uso — não contém lógica de negócio.

```java
package com.srm.creditengine.cambio.infrastructure.adapter.in.web;

import com.srm.creditengine.cambio.application.DinheiroConverter;
import com.srm.creditengine.cambio.application.TaxaCambioOrchestrator;
import com.srm.creditengine.cambio.application.TaxaCambioUpdater;
import com.srm.creditengine.cambio.application.TaxaVigenteReader;
import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateNotFoundException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/taxas-cambio")
public class TaxaCambioController {

    private final TaxaCambioUpdater taxaCambioUpdater;
    private final TaxaVigenteReader taxaVigenteReader;
    private final DinheiroConverter dinheiroConverter;
    private final TaxaCambioOrchestrator taxaCambioOrchestrator;

    public TaxaCambioController(TaxaCambioUpdater taxaCambioUpdater, TaxaVigenteReader taxaVigenteReader,
                                DinheiroConverter dinheiroConverter, TaxaCambioOrchestrator taxaCambioOrchestrator) {
        this.taxaCambioUpdater = taxaCambioUpdater;
        this.taxaVigenteReader = taxaVigenteReader;
        this.dinheiroConverter = dinheiroConverter;
        this.taxaCambioOrchestrator = taxaCambioOrchestrator;
    }

    @PutMapping
    public ResponseEntity<TaxaCambioResponse> update(@Valid @RequestBody TaxaCambioUpdateRequest request) {
        ParMoedas par = new ParMoedas(new CodigoMoeda(request.codigoBase()), new CodigoMoeda(request.codigoCotacao()));
        TaxaCambio taxa = taxaCambioUpdater.update(par, request.taxa(), request.vigencia());
        return ResponseEntity.ok(toResponse(taxa));
    }

    @GetMapping("/vigente")
    public ResponseEntity<TaxaCambioResponse> current(
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String codigoBase,
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String codigoCotacao) {
        ParMoedas par = new ParMoedas(new CodigoMoeda(codigoBase), new CodigoMoeda(codigoCotacao));
        return taxaVigenteReader.read(par, Instant.now())
            .map(taxa -> ResponseEntity.ok(toResponse(taxa)))
            .orElseThrow(() -> new ExchangeRateNotFoundException(par));
    }

    @PostMapping("/integracao")
    public ResponseEntity<TaxaCambioResponse> orchestrate(
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String codigoBase,
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String codigoCotacao) {
        ParMoedas par = new ParMoedas(new CodigoMoeda(codigoBase), new CodigoMoeda(codigoCotacao));
        TaxaCambio taxa = taxaCambioOrchestrator.orchestrate(par);
        return ResponseEntity.ok(toResponse(taxa));
    }

    @PostMapping("/convert")
    public ResponseEntity<DinheiroConverterResponse> convert(@Valid @RequestBody DinheiroConverterRequest request) {
        Dinheiro valor = new Dinheiro(request.valor(), new CodigoMoeda(request.codigoMoeda()), request.escala());
        ParMoedas par = new ParMoedas(new CodigoMoeda(request.codigoBase()), new CodigoMoeda(request.codigoCotacao()));
        TaxaCambio taxa = taxaVigenteReader.read(par, Instant.now())
            .orElseThrow(() -> new ExchangeRateNotFoundException(par));
        Dinheiro converted = dinheiroConverter.convert(valor, taxa);
        return ResponseEntity.ok(new DinheiroConverterResponse(
            converted.valor(), converted.moeda().codigo(), taxa.taxa(), taxa.vigencia()));
    }

    private TaxaCambioResponse toResponse(TaxaCambio taxa) {
        return new TaxaCambioResponse(
            taxa.par().base().codigo(), taxa.par().cotacao().codigo(), taxa.taxa(), taxa.vigencia());
    }

    public record TaxaCambioUpdateRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoBase,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoCotacao,
        @NotNull @Positive BigDecimal taxa,
        @NotNull Instant vigencia) {}

    public record DinheiroConverterRequest(
        @NotNull @Positive BigDecimal valor,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoMoeda,
        int escala,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoBase,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoCotacao) {}

    public record TaxaCambioResponse(String codigoBase, String codigoCotacao, BigDecimal taxa, Instant vigencia) {}

    public record DinheiroConverterResponse(BigDecimal valor, String codigoMoeda, BigDecimal appliedTaxa, Instant vigencia) {}
}
```

---

## 8. Handler global de exceções (atualização)

`GlobalExceptionHandler` ganha mapeamentos específicos **antes** do genérico de `DomainException`:

| Exceção | HTTP |
| --- | --- |
| `UnknownCurrencyException` | `422` (genérico de `DomainException`) |
| `ExchangeRateConflictException` | `409 Conflict` |
| `ExchangeRateNotFoundException` | `404 Not Found` |
| `ExchangeRateProviderUnavailableException` | `503 Service Unavailable` |
| `IncompatibleCurrenciesException` | `422` (genérico) |

```java
@ExceptionHandler(ExchangeRateConflictException.class)
public ResponseEntity<ErrorBody> handleExchangeRateConflict(ExchangeRateConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorBody(ex.getMessage()));
}

@ExceptionHandler(ExchangeRateNotFoundException.class)
public ResponseEntity<ErrorBody> handleExchangeRateNotFound(ExchangeRateNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorBody(ex.getMessage()));
}

@ExceptionHandler(ExchangeRateProviderUnavailableException.class)
public ResponseEntity<ErrorBody> handleExchangeRateProviderUnavailable(ExchangeRateProviderUnavailableException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorBody(ex.getMessage()));
}
```

---

## 9. Configuração

### 9.1 `application.yml`

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

### 9.2 `pom.xml` — Spring Cloud BOM + OpenFeign

Boot 3.5.16 é compatível com o release train **Spring Cloud 2025.0.x** (Northfields); OpenFeign 4.3.x.

```xml
<properties>
    <java.version>21</java.version>
    <spring-cloud.version>2025.0.3</spring-cloud.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
</dependencies>
```

`@EnableFeignClients` na aplicação (o client `BcbPtaxClient` está sob o pacote base `com.srm.creditengine`, coberto pelo scan).

### 9.3 `TaxaCambioProviderConfig` (monta beans)

```java
package com.srm.creditengine.cambio.infrastructure.config;

import com.srm.creditengine.cambio.application.DinheiroConverter;
import com.srm.creditengine.cambio.application.TaxaCambioOrchestrator;
import com.srm.creditengine.cambio.application.TaxaCambioUpdater;
import com.srm.creditengine.cambio.application.TaxaVigenteReader;
import com.srm.creditengine.cambio.domain.TaxaCambioProvider;
import com.srm.creditengine.cambio.domain.MoedaRepository;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.cambio.infrastructure.adapter.out.external.BcbPtaxClient;
import com.srm.creditengine.cambio.infrastructure.adapter.out.external.BcbPtaxTaxaCambioProvider;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaxaCambioProviderConfig {

    @Bean
    public TaxaCambioProvider taxaCambioProvider(BcbPtaxClient bcbPtaxClient,
            @Value("${app.cambio.provider.quote-currency}") String quoteCurrency) {
        return new BcbPtaxTaxaCambioProvider(bcbPtaxClient, quoteCurrency, ZoneId.of("America/Sao_Paulo"), 3);
    }

    @Bean
    public TaxaCambioUpdater taxaCambioUpdater(TaxaCambioRepository repository, MoedaRepository moedaRepository) {
        return new TaxaCambioUpdater(repository, moedaRepository);
    }

    @Bean
    public TaxaVigenteReader taxaVigenteReader(TaxaCambioRepository repository) {
        return new TaxaVigenteReader(repository);
    }

    @Bean
    public DinheiroConverter dinheiroConverter() {
        return new DinheiroConverter();
    }

    @Bean
    public TaxaCambioOrchestrator taxaCambioOrchestrator(TaxaCambioProvider provider,
            TaxaCambioRepository repository, MoedaRepository moedaRepository) {
        return new TaxaCambioOrchestrator(provider, repository, moedaRepository);
    }
}
```

Dependências novas: `spring-cloud-starter-openfeign` (via BOM `spring-cloud-dependencies` 2025.0.3) e `wiremock-standalone` (teste, para o contrato HTTP do Feign). JPA/Flyway já presentes. Ajuste: `@EnableFeignClients` na aplicação.

---

## 10. Testes (JUnit + Mockito + WireMock + Testcontainers)

| Arquivo | Cobre |
| --- | --- |
| `ParMoedasTest` | aceita par válido; rejeita `null`, moedas iguais; `contem` |
| `TaxaCambioTest` | rejeita `par`/`taxa`/`vigencia` nulos; taxa não positiva; escala aplicada |
| `TaxaCambioUpdaterTest` (Mockito) | moeda inexistente → 422; vigência ocupada → 409; sucesso salva |
| `TaxaVigenteReaderTest` (Mockito) | retorna taxa quando existe; vazio quando não |
| `DinheiroConverterTest` | base→cotação (`* taxa`); cotação→base (`1/taxa`); par não contém moeda → `IncompatibleCurrenciesException` |
| `TaxaCambioOrchestratorTest` (Mockito) | provedor vazio → 503; vigência nova salva; vigência já existente não duplica |
| `BcbPtaxTaxaCambioProviderTest` (Mockito mock de `BcbPtaxClient`) | par suportado/`supports`; Fechamento; data/hora parse; par não suportado → empty; corpo vazio → empty |
| `BcbPtaxClientContractTest` (WireMock) | contrato HTTP real: URL OData, query params `@moeda`/`$format`, deserialização JSON |
| `TaxaCambioControllerTest` (MockMvc + @MockBean casos de uso) | contratos HTTP: `PUT` 200/409, `GET` vigente 200/404/400, `POST` integracao 200/503, `POST` converter 200/400; validações |
| `TaxaCambioRepositoryAdapterTest` (Testcontainers PostgreSQL) | `save`/`existsVigencia`/`obtainVigente` (par + vigência máxima ≤ referência) |
| `GlobalExceptionHandlerTest` (atualizar) | mapeamentos 409/404/503 |

**Documentação**: atualizar `docs/TechDoc.md` (criar se não existir) com o módulo `cambio`, contratos, adapter BCB PTAX e propriedades.

---

## 11. Verificação

- `cd backend && ./mvnw verify` — build, testes unitários/integração/API e gate JaCoCo ≥ 90%;
- `./mvnw verify` com Testcontainers (PostgreSQL) validando o adapter de persistência e WireMock validando o contrato Feign;
- App sobe com Flyway (schema inalterado) e `/api/health` continua `200`;
- Contratos OpenAPI refletidos no `springdoc` automaticamente (`@RestController` + records).

---

## 12. Registro de revisões

| Revisão | Alterações |
| --- | --- |
| v1 | Plano inicial do Ponto 4 (motor de câmbio, BCB PTAX). |
| v2 | Padronização de nomenclatura (palavra de domínio em PT, resto em inglês): `ProvedorTaxaCambio` → `TaxaCambioProvider`; `RepositorioTaxaCambio` → `TaxaCambioRepository`; `RepositorioMoeda` → `MoedaRepository`; `MoedaRepositorioAdapter` → `MoedaRepositoryAdapter`; `TaxaCambioRepositorioAdapter` → `TaxaCambioRepositoryAdapter`; `BcbPtaxProvedorTaxaCambio` → `BcbPtaxTaxaCambioProvider`; `ExchangeRateProviderConfig` → `TaxaCambioProviderConfig`; DTOs `AtualizarTaxaCambioRequest`/`ConverterDinheiroRequest`/`ConverterDinheiroResponse`; adapter com `supports`/`rateFor`/`parseTimestamp`/`PtaxResponse`/`PtaxQuote`/`PTAX_DATE_FORMAT`/`PTAX_TIMESTAMP_FORMAT`; variáveis técnicas `repository`/`moedaRepository`/`provider`; método bean `taxaCambioProvider`. Renomeado `ParMoedas.envolve` → `ParMoedas.contem` (sujeito correto). |
| v3 | Integração BCB PTAX via **Feign Client** (diretriz do `.agents/backend.md`): novo `BcbPtaxClient` (`@FeignClient` + `@GetMapping` OData) em `adapter/out/external/`; `BcbPtaxTaxaCambioProvider` injeta o client em vez de `RestClient`; `@EnableFeignClients` na aplicação; novo §9.2 `pom.xml` com BOM `spring-cloud-dependencies` 2025.0.3 + `spring-cloud-starter-openfeign`; §9.1 `application.yml` com timeouts `spring.cloud.openfeign.client.config.default`; §9.3 config sem bean `RestClient`; testes trocam `MockRestServiceServer` por Mockito (mock de `BcbPtaxClient`) + `BcbPtaxClientContractTest` com WireMock; `wiremock-standalone` como dependência de teste. |
| v4 | Remoção de palavras PT **fora do domínio** em identificadores técnicos (convenção: palavra reservada de domínio em PT, resto em inglês). `validar` → `validate` (TaxaCambio); portas: `existe` → `exists`, `obter` → `obtain`, `obterVigente` → `obtainVigente`, `existeVigencia` → `existsVigencia`, `salvar` → `save`, parâmetro `referencia` → `reference`; casos de uso: `executar` → `execute`, `validarMoedas` → `validateMoedas`, `validarMoeda` → `validateMoeda`, `validarVigenciaDisponivel` → `validateVigenciaAvailable`, `validarParContem` → `validateParContem`, `fatorPara` → `factorFor`, `outraMoeda` → `otherMoeda`, variável `destino` → `target`, `fator` → `factor`, `obtida` → `obtained`; `BcbPtaxClient.consultarCotacao` → `queryCotacao`, parâmetros `dataInicial` → `startDate`, `dataFinal` → `endDate`; `BcbPtaxTaxaCambioProvider`: variável `cotacao` → `quote`, `venda` → `sellRate`, parâmetro `valor` → `value`; `TaxaCambioRepositoryAdapter`: `entidade` → `entity`; controller: métodos `atualizar` → `update`, `vigente` → `current`, `integrar` → `integrate`, `converter` → `convert`, variável `convertido` → `converted`, campo `taxaAplicada` → `appliedTaxa`. Nomes de colunas/API (`codigoBase`, `codigoCotacao`, `/vigente`, `/integracao`; `/converter` renomeado para `/convert` na v7) e campos de domínio (`valor`, `moeda`, `escala`, `taxa`, `vigencia`) preservados por serem contrato/domínio. |
| v5 | Casos de uso renomeados para nomes **autoexplicativos** (palavra de domínio PT + papel técnico EN), eliminando verbos PT vagos: `AtualizarTaxaCambio` → `TaxaCambioUpdater` (método `execute` → `update`); `ObterTaxaVigente` → `TaxaVigenteReader` (método `execute` → `read`); `ConverterDinheiro` → `DinheiroConverter` (método `execute` → `convert`); `IntegrarTaxaCambio` → `TaxaCambioOrchestrator` (método `execute` → `orchestrate`). DTOs: `AtualizarTaxaCambioRequest` → `TaxaCambioUpdateRequest`, `ConverterDinheiroRequest` → `DinheiroConverterRequest`, `ConverterDinheiroResponse` → `DinheiroConverterResponse`. Controller: campos/parâmetros renomeados (`taxaCambioUpdater`, `taxaVigenteReader`, `dinheiroConverter`, `taxaCambioOrchestrator`), método do endpoint `/integracao` → `orchestrate`. Config §9.3 e tabela de testes §10 alinhadas. Convenção (§1/linha 9) atualizada para refletir casos de uso em EN com palavra de domínio PT. |
| v6 | **Implementação concluída** (após aprovação). Código gerado conforme o plano: domínio, casos de uso, adapters (web/JPA/external), config, handler global e testes. Ajustes de implementação: (1) `BcbPtaxClient.queryCotacao` passou a receber `@RequestParam Map<String,String>` porque o `SpringMvcContract` do Feign não expande nomes de parâmetro com prefixo `@` como variáveis de template (mantém `$format`/`$select` corretos); (2) `TaxaCambioController` recebeu `@Validated` para que as restrições `@Pattern` em `@RequestParam` sejam aplicadas e retornem `400`; (3) `GlobalExceptionHandler` ganhou também o handler de `ConstraintViolationException` → `400`; (4) refatoração da Fase 1 aplicada ao código (§2.1): `CodigoMoeda.validate`, `Dinheiro.add/multiply/validateSameMoeda/validateFactor` e `DinheiroTest` atualizado. `docs/TechDoc.md` criado com o módulo `cambio`. Validação: `./mvnw verify` BUILD SUCCESS, 69 testes, cobertura de linha ~99%. |
| v7 | Correções validadas contra o `$metadata` real do BCB: a função OData `CotacaoMoedaPeriodo` usa o parâmetro `dataInicial` (não `dataInicialCotacao`) — `BcbPtaxClient` e `BcbPtaxTaxaCambioProvider` ajustados; `dataHoraCotacao` do PTAX tem microssegundos (`yyyy-MM-dd HH:mm:ss.SSSSSS`) e o parse passou a normalizar para ISO (`LocalDateTime.parse(value.replace(' ', 'T'))`); o provider seleciona o `Fechamento` **mais recente** do período e mapeia `FeignException` → `ExchangeRateProviderUnavailableException` (503) para degradação graciosa. `GlobalExceptionHandler` ganhou handler de `MissingServletRequestParameterException`/`MethodArgumentTypeMismatchException` → `400` e de `NoResourceFoundException` → `404`. Endpoint de conversão renomeado: `POST /api/taxas-cambio/converter` → **`/convert`** (diretriz do usuário), com testes e docs atualizados. Validação: `./mvnw verify` BUILD SUCCESS, 74 testes. |
| v8 | **Fallback automático para o provedor nas leituras** (decisão do usuário, validada contra o desafio técnico — mantém GET *safe* e POST com efeito colateral). `TaxaVigenteReader` ganhou `TaxaCambioProvider` e o método `readOrObtain` (banco → provedor, **sem persistir**); `TaxaCambioController.current` (`GET /vigente`) usa `readOrObtain` (sem escrita → GET puro; `404` se provedor vazio; `503` se BCB falhar); `TaxaCambioController.convert` (`POST /convert`) passa a fazer `.read(...).orElseGet(() -> taxaCambioOrchestrator.orchestrate(par))` (busca no BC + persistência **idempotente** quando não há taxa armazenada; `503` orienta inserção manual via `PUT`). `TaxaCambioProviderConfig` injeta o provider no reader. Testes atualizados: `TaxaVigenteReaderTest` (4 novos casos de `readOrObtain`) e `TaxaCambioControllerTest` (fallback do convert e `503`). Validação: `./mvnw verify` BUILD SUCCESS, 78 testes. |