# SRM Credit Engine — Domínio de Negócio e Raciocínio de Desenvolvimento

Este documento é a fonte de verdade para o entendimento do problema, a modelagem de domínio e a linha lógica de construção do produto. Ele organiza, ponto a ponto, a sequência de raciocínio que orienta as decisões de engenharia, e separa as implementações em domínios de negócio do backend e em domínios de interface do frontend, com mentalidade de desacoplamento.

---

## 1. Contexto do Negócio

A SRM Asset opera fundos de investimento, com foco em FIDCs (Fundos de Investimento em Direitos Creditórios). A operação compra ativos (duplicatas, contratos, recebíveis) de empresas cedentes e provê liquidez ao mercado. Com a globalização do portfólio, o caixa passou a ser multimoedas (BRL e USD).

O SRM Credit Engine existe para:

1. Receber um lote de recebíveis;
2. Calcular o **deságio** (desconto) com base no risco do ativo (spread) e na moeda de pagamento;
3. Registrar a transação de forma **auditável** e **consistente**.

O problema central não é apenas calcular — é calcular com precisão decimal em ambiente financeiro, sobre múltiplas moedas, e persistir cada operação de forma que nenhuma liquidação fique "pela metade".

---

## 2. Raciocínio Lógico e Sequencial de Desenvolvimento

Abaixo, a linha de raciocínio que deve guiar todas as decisões. Cada etapa justifica a anterior e habilita a seguinte. Esta ordem é deliberada: **começamos pelo que é fundamento (precisão e domínio), passamos pelo que gera valor (precificação), e terminamos pelo que amplia escala (observabilidade, eventos, análise de alto volume)**.

### 2.1 Ponto 1 — O domínio é financeiro: precisão numérica é requisito de segurança, não detalhe

Antes de qualquer linha de código, a decisão mais importante é aceitar que números monetários **não podem usar ponto flutuante** (`double`/`float`). Em Java, a resposta é `BigDecimal` com escala e arredondamento explícitos. No banco, `NUMERIC(p,s)`. Esta escolha antecede tudo porque a fundação de integridade de um sistema financeiro está na aritmética.

**Abordagem escolhida:** representar valores monetários como Value Object imutável (`Money`), carregando valor + moeda + escala, evitando "primitivism obsession" e erros de arredondamento dispersos no código.

```java
public record Money(BigDecimal amount, CurrencyCode currency, int scale) {
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        amount = amount.setScale(scale, RoundingMode.HALF_EVEN);
    }
}
```

**Por que HALF_EVEN:** regra bancária padrão (banker's rounding), evita viés sistemático ao arredondar sempre para o lado ímpar mais próximo. Para ambiente financeiro com muitas operações, é o padrão mais equilibrado.

### 2.2 Ponto 2 — Extrair os domínios de negócio antes de pensar em código

O problema foi decomposto em **bounded contexts** (Domain-Driven Design), cada um com linguagem própria e fronteiras claras. Esta é a base do desacoplamento: cada domínio evolui, é testado e é substituído independentemente.

| Domínio de negócio | Responsabilidade | Por que é separado |
| --- | --- | --- |
| **Currency** | Câmbio: taxas, conversão, histórico | Muda por integração externa e por agenda de mercado; não deve contaminar a precificação |
| **Pricing** | Precificação: deságio, spreads por tipo de recebível | É a regra de maior variação (novos produtos entram com frequência) |
| **Settlement** | Liquidação: registrar operações, integridade ACID, auditoria | É onde há concorrência e risco de inconsistência; exige transações atômicas |
| **Analytics** | Consultas de alto volume (extrato de liquidação) | Tem requisito de performance diferente (leituras pesadas, agregações) |

**Decisão importante (e deliberada):** estes domínios vivem em **módulos independentes dentro de um mesmo deployable** (modular monolith), e não em microserviços. Justificativa: para a volumetria inicial e a necessidade de consistência transacional imediata entre precificar e liquidar, o custo de uma saga distribuída seria maior que o benefício. A separação em módulos preserva a evolução futura para microserviços **sem** reescrever o domínio, pois as portas já isolam as fronteiras.

### 2.3 Ponto 3 — Arquitetura hexagonal como esqueleto

Cada domínio segue arquitetura hexagonal (ports & adapters), com regras de negócio no centro e tecnologia na borda. Esta decisão garante:

- **Testabilidade:** regras de precificação são testadas sem Spring, sem banco, sem HTTP.
- **Independência de framework:** o domínio não conhece JPA, Spring ou HTTP.
- **Troca de adapters:** banco, fila ou API externa são substituíveis sem tocar no núcleo.

```
domain/    -> entidades, value objects, regras de negócio, ports (interfaces)
application -> casos de uso, orquestração, serviços de aplicação
infrastructure -> adapters (web, persistence, fila), configuração
```

### 2.4 Ponto 4 — Currency Engine primeiro: nada de cross-currency sem câmbio

A precificação cross-currency (título em BRL, pagamento em USD) **depende** de taxas de câmbio. Logo, o domínio de câmbio é pré-requisito lógico. Ele precisa:

- Armazenar taxas por par de moedas;
- Prover a taxa vigente (pelo menos a mais recente);
- Permitir atualização manual e integração (mockada por enquanto);
- Manter **histórico** de taxas (essencial para auditoria e reprocessamento).

**Abordagem escolhida:** taxa como entidade versionada por data/hora de vigência. A busca "a taxa vigente" é uma consulta pelo par + data máxima ≤ referência. O adapter de integração externa é isolado por interface (`ExchangeRateProvider`), para que o mock de hoje possa ser trocado por um provedor real (BCB, Reuters) sem tocar no domínio.

```java
public interface ExchangeRateProvider {
    Optional<ExchangeRate> fetch(CurrencyPair pair);
}
```

**Decisão sobre atualização:** a atualização manual é um caso de uso que valida moedas existentes, taxa positiva e persistência com conflito detectável (se duas atualizações concorrem, vence a mais recente por data — ou rejeita com `409` se o operador tentar sobrescrever uma vigência já ocupada).

### 2.5 Ponto 5 — Pricing Engine com Strategy Pattern: a regra que mais varia

O motor de precificação é o coração do produto. A fórmula base:

> Valor Presente = Valor Face / (1 + Taxa Base + Spread) ^ Prazo

O spread varia **por tipo de recebível**:

| Tipo de recebível | Spread (a.m.) |
| --- | --- |
| Duplicata Mercantil | 1,5% |
| Cheque Pré-datado | 2,5% |

**Abordagem escolhida — Strategy Pattern:** cada tipo de recebível é uma estratégia (`PricingStrategy`) que encapsula sua regra de risco. O cálculo base é único; a estratégia fornece o spread. Isso cumpre o **Open/Closed Principle**: para adicionar um novo produto, cria-se uma nova estratégia sem modificar o motor.

```java
public interface PricingStrategy {
    Spread spreadFor(Receivable receivable);
}
```

O motor (`PricingEngine`) recebe a estratégia resolvida por tipo, calcula o valor presente e aplica a conversão cambial **no final**, quando cross-currency.

**Por que a conversão no final:** o deságio é calculado sobre a moeda do título; a conversão é uma etapa de apresentação/liberação do valor em moeda de pagamento. Calcular o spread sobre o valor convertido mudaria o resultado (e o risco), pois o spread é uma taxa do ativo, não da moeda.

**Decisão sobre arredondamento intermediário:** a fórmula usa potência com expoente decimal (prazo em meses fracionado). Potências decimais em `BigDecimal` exigem `Math.pow` sobre `double` — que reintroduz imprecisão. Abordagem: o prazo em dias é convertido para meses com escala definida e a potência é calculada via `BigDecimal.pow` para prazo inteiro ou `StrictMath.pow` apenas no expoente, com arredondamento final controlado pela escala de `Money`. A margem de erro da exponenciação é aceita **somente** na dimensão de prazo fracionário e corrigida no arredondamento final, documentada como decisão de precisão (ver ADR-003).

### 2.6 Ponto 6 — Caso de uso de simulação: valor líquido em tempo real

O painel do operador precisa mostrar o valor líquido em tempo real enquanto o usuário digita. Isto não é uma transação — é uma **simulação**. 

**Abordagem escolhida:** endpoint dedicado e stateless `POST /api/simulations/pricing` que recebe os parâmetros e retorna o valor líquido calculado. Não persiste nada. O frontend chama com debounce (a cada pausa de digitação) e renderiza o resultado.

**Por que endpoint no backend e não cálculo no frontend:** o frontend deve permanecer "burro" (ver agentes frontend). Duplicar a fórmula de precificação no cliente criaria duas fontes de verdade e risco de divergência de arredondamento. A regra de negócio vive no backend; o cliente apenas apresenta o resultado.

### 2.7 Ponto 7 — Settlement com integridade ACID e proteção contra concorrência

Liquidação é a operação que **não pode ficar pela metade**. Um lote de recebíveis deve ser precificado, convertido e registrado como uma transação única, com propriedades ACID.

**Abordagem escolhida — transação de banco (Spring `@Transactional`) para o registro:** todo o fluxo de liquidação de um lote roda em uma única transação. Se qualquer recebível falhar, tudo é revertido.

**Proteção contra race condition — Optimistic Locking:** a versão de um recebível é incrementada a cada liquidação. Duas liquidações simultâneas sobre o mesmo recebível: a primeira vence, a segunda recebe `409 Conflict` e a aplicação reage de forma controlada (não deixa o operador sem resposta — informa o conflito e sugere reprocessamento).

```java
public class Receivable extends AggregateRoot {
    @Version
    private Long version;
}
```

**Por que Optimistic Locking e não Locking Pessimista:** em operação de mesa com baixa taxa de conflito real, o custo de segurar locks de banco por operação é desnecessário. O otimista detecta o conflito e falha rápido, o que é mais escalável e mais simples de raciocinar. A escolha pessimista seria justificável se o conflito fosse frequente (ver ADR-005).

**Idempotência:** o registro de liquidação aceita um `liquidationRequestId` gerado pelo cliente; o sistema rejeita requisições duplicadas, garantindo que retries de rede não criem liquidações duplicadas.

### 2.8 Ponto 8 — API First: contratos antes da implementação

O design da API é definido antes do código, com OpenAPI/Swagger como contrato vivo. Os endpoints seguem verbos HTTP corretos e códigos de status semânticos:

| Operação | Método | Código de sucesso | Código de erro |
| --- | --- | --- | --- |
| Criar recebível | `POST /api/receivables` | `201 Created` | `400`, `409` |
| Listar recebíveis | `GET /api/receivables` | `200 OK` | — |
| Simular precificação | `POST /api/simulations/pricing` | `200 OK` | `400`, `422` |
| Liquidação em lote | `POST /api/liquidations` | `201 Created` | `400`, `409`, `422` |
| Atualizar taxa de câmbio | `PUT /api/currency-rates` | `200 OK` | `400`, `409` |
| Extrato de liquidação | `GET /api/liquidations/extract` | `200 OK` | `400` |

**Decisão sobre `422`:** validação semântica de negócio (ex.: spread não configurado para o tipo) retorna `422 Unprocessable Entity`, separando "requisição malformada" (`400`) de "requisição válida porém não processável por regra de negócio".

### 2.9 Ponto 9 — Persistência: ORM para CRUD, SQL nativo para análise

Dois requisitos opostos coexistem:

- **Operações transacionais** (recebíveis, liquidação): precisam de mapeamento e consistência — **JPA** é adequado.
- **Consultas analíticas de alto volume** (extrato): precisam de performance e agregação — **ORM puro é inadequado**.

**Abordagem escolhida — duas estratégias de persistência:** JPA para a escrita/domínio (com `ddl-auto: validate` e Flyway como fonte do schema) e **JdbcTemplate/SQL nativo** para o extrato, passando pela camada de aplicação (preservando autorização e contrato) mas **sem** passar pela camada de negócio — exatamente como a diretriz do backend descreve: "relatórios podem ser organizados em duas camadas apenas, sem necessidade de passar pela de negócios".

**Por que Query Builder/SQL nativo em vez de JPA para relatórios:** consultas de agregação geradas por JPA frequentemente produzem SQL ineficiente, com joins desnecessários e dificuldade de controle de índice. SQL nativo parametrizado permite: `GROUP BY` por período, `FILTER` por cedente/moeda, paginação por cursor para grandes volumes e garantia de uso de índice.

### 2.10 Ponto 10 — Tratamento de exceções global e resiliente

**Abordagem escolhida — `@ControllerAdvice` global:** um handler central converte exceções de domínio em respostas HTTP padronizadas:

- **DomainException** → `4xx` específico com mensagem amigável;
- **ValidationException** → `400`/`422` com lista de campos inválidos;
- **OptimisticLockException** → `409` com orientação de reprocessamento;
- **Exceção inesperada** → `500` com corpo genérico, **sem detalhes internos**, e logging contextual estruturado (com `requestId` para rastreio).

Nenhuma exceção é ignorada. O fluxo nunca segue após uma falha que comprometa a consistência da operação.

### 2.11 Ponto 11 — Observabilidade: logs estruturados, métricas e rastreio

- **Logs estruturados** em JSON (via Logback), sempre em stdout/stderr, com `requestId` e contexto de negócio (lote, recebível, moeda). Nunca expõem segredos ou dados sensíveis.
- **Métricas** com Micrometer expostas em `/actuator/prometheus` e coletadas por Prometheus/Grafana (latência, taxa de erro, volume por endpoint).
- **Health checks** em `/actuator/health` para liveness/readiness.

### 2.12 Ponto 12 — Mensageria e eventos: quando e por quê

A diretriz do backend é explícita: **não introduzir filas apenas para cumprir 12-Factor**. Então a decisão é contextual:

**Não usar mensageria agora para:** precificar e liquidar (precisam ser síncronos e atômicos — uma saga distribuída aumentaria o risco sem retorno).

**Usar mensageria (fase de escala/evolução) para:** publicar **eventos de domínio** (`ReceivablePriced`, `SettlementExecuted`, `ExchangeRateUpdated`) consumidos por domínios que não exigem consistência imediata — ex.: Analytics alimentando o extrato de forma assíncrona, notificações, integrações externas. Isto desacopla a escrita transacional da leitura analítica: a liquidação não espera pela agregação.

Esta separação é deliberada e documentada (ver ADR-006): o modelo transacional escreve no banco OLTP; os eventos alimentam projeções analíticas (CQRS leve) quando a volumetria justificar.

### 2.13 Ponto 13 — Escala: design para 1 milhão de transações/minuto

A arquitetura-alvo para alta escala (documentada como design de evolução, não como primeira implementação):

- **Caching de taxas de câmbio** em cache distribuído (Redis): taxas são leituras de alta frequência e baixa mutação — benefício de cache imediato.
- **Sharding por moeda e/ou por cedente**: o agregado de liquidação é naturalmente particionável por chave de negócio; sharding por cedente mantém co-locadas as operações de um mesmo cliente.
- **Leituras analíticas em réplicas de leitura** ou em armazenamento analítico dedicado (data warehouse/OLAP), alimentado por eventos.
- **Consistência eventual aceita apenas** para leituras analíticas de projeção; o núcleo transacional permanece fortemente consistente (ACID).
- **Idempotência e retries** como padrão em toda operação que toca rede ou banco.
- **Circuit breaker** em chamadas externas (provedor de câmbio, por exemplo): falha controlada e degradação graciosa.

### 2.14 Ponto 14 — Frontend: UI burra, domínio da interação no cliente

O frontend é React (com TypeScript strict), conforme agentes. As decisões:

- **UI Components puros:** recebem props, emitem eventos; não contêm regras de negócio nem chamadas HTTP.
- **TanStack Query:** gerencia estado de servidor (cache, invalidação, mutações). **Zustand:** somente para estado de cliente compartilhado (ex.: preferências de interface, estado de sessão visual).
- **Filtros, paginação e ordenação no estado da URL:** permitem compartilhamento e persistência de navegação.
- **Validação de formulário com Zod + React Hook Form:** feedback imediato no cliente, mas o backend permanece autoritativo.

---

## 3. Implementações de Domínio — Backend

A seguir, cada domínio de negócio detalhado com suas portas, regras e decisões, sob a mentalidade de desacoplamento.

### 3.1 Domínio Currency (Câmbio)

**Responsabilidade:** armazenar e prover taxas de câmbio por par de moedas, com histórico e atualização manual ou integrada.

**Modelo:**
- `CurrencyCode` (BRL, USD) — enum/VO;
- `CurrencyPair` (base, quote) — VO;
- `ExchangeRate` (par, taxa, vigência em `Instant`) — entidade;
- `Money` — VO monetário.

**Regras:**
- Moeda deve existir antes de aceitar taxa;
- Taxa deve ser positiva;
- Um par pode ter várias taxas em vigências distintas (histórico);
- Conversão: `base * rate` para `quote`; operação inversa usa `1 / rate` com arredondamento controlado.

**Portas de saída:**
- `ExchangeRateProvider` (adapter externo, hoje mockado);
- `ExchangeRateRepository` (persistência).

**Casos de uso (application):**
- `UpdateExchangeRate` (manual, com detecção de conflito de vigência);
- `GetCurrentRate` (taxa vigente para o par);
- `ConvertMoney` (conversão com arredondamento e histórico).

**Decisão de desacoplamento:** a conversão nunca é calculada dentro do domínio Pricing; o Pricing **consulta** o Currency via porta de saída. Assim, uma mudança de fornecedor de câmbio não toca o motor de precificação.

### 3.2 Domínio Pricing (Precificação)

**Responsabilidade:** calcular o valor presente (deságio) de um recebível, aplicando spread por tipo de produto e conversão cambial quando cross-currency.

**Modelo:**
- `Receivable` (valor face, moeda do título, vencimento, tipo);
- `ReceivableType` (DuplicataMercantil, ChequePreDatado);
- `Spread` (taxa por período);
- `PricingStrategy` — interface; implementações por tipo;
- `PricingResult` (valor presente, spread aplicado, valor convertido quando aplicável).

**Regra de cálculo:**
```
valorPresente = valorFace / (1 + taxaBase + spread) ^ prazoMeses
se pagamento != moeda do título:
    valorLiquido = converter(valorPresente, moedaPagamento)
```

**Estratégias:**
- `DuplicataMercantilStrategy` — spread 1,5% a.m.;
- `ChequePreDatadoStrategy` — spread 2,5% a.m.

**Portas de saída:**
- `ExchangeRateProvider`/`CurrencyService` (para cross-currency);
- `ReceivableRepository`.

**Decisão de desacoplamento — Strategy:** o motor `PricingEngine` depende da interface `PricingStrategy`, resolvida por tipo via um `StrategyResolver` (factory). Adicionar produto = nova estratégia + registro no resolver. Nenhuma mudança no motor.

```java
public class PricingEngine {
    public PricingResult price(Receivable receivable, PricingStrategy strategy, Money payoutCurrency) { ... }
}
```

### 3.3 Domínio Settlement (Liquidação)

**Responsabilidade:** registrar a liquidação de um lote de recebíveis como transação atômica e auditável, com valor líquido final em moeda de pagamento.

**Modelo:**
- `Liquidation` (agregado raiz: lote, status, idempotency key);
- `LiquidationItem` (recebível, valores calculados, spread aplicado);
- `LiquidationStatus` (PROCESSING, SETTLED, FAILED).

**Regras:**
- Todo o lote liquida em uma única transação ACID;
- Recebível tem `version` (Optimistic Locking) — conflito retorna `409`;
- Idempotência via `liquidationRequestId` — requisições duplicadas não geram liquidações novas;
- Auditabilidade: cada item registra valor face, spread, prazo, valor presente, moeda, taxa e valor líquido final — nada é apagado, apenas registrado.

**Portas de saída:**
- `LiquidationRepository`;
- `ReceivableRepository` (com lock otimista).

**Decisão de desacoplamento:** o Settlement orquestra Pricing e Currency **por portas**; não conhece suas implementações. Se um dia a liquidação virar microserviço, as portas são os contratos de integração.

### 3.4 Domínio Analytics (Extrato de Liquidação)

**Responsabilidade:** prover consultas de alto volume sobre liquidações, com filtros por período, cedente e moeda.

**Abordagem:** camada de aplicação + adaptador de persistência com SQL nativo (JdbcTemplate). Não passa pela camada de negócio, mas passa pela de aplicação (autorização e contrato).

**Decisões:**
- **Paginação por cursor** (`WHERE id > :lastId ORDER BY id`) para volumes grandes — mais estável que `OFFSET` em grandes conjuntos;
- **Filtros opcionais** montados dinamicamente com SQL parametrizado (sem injeção);
- **Índices compostos** (data, cedente, moeda) definidos em migração Flyway;
- **Agregação** (soma por período/moeda) em SQL, não em memória.

**Porta de saída:**
- `LiquidationQuery` (interface de leitura dedicada, separada do repositório de escrita — segregação CQRS leve).

---

## 4. Implementações de Domínio — Frontend

### 4.1 Domínio de Interface — Painel do Operador (Simulação)

**Responsabilidade:** capturar dados do recebível (valor, vencimento, tipo) e exibir em tempo real o valor líquido simulado.

**Abordagem:**
- Formulário com React Hook Form + Zod (validação de experiência: valor positivo, vencimento futuro, tipo válido);
- Ao mudar qualquer campo, chama `POST /api/simulations/pricing` com **debounce** (ex.: 300–400ms);
- O resultado (valor líquido, spread aplicado, valor convertido) é apresentado em um painel de resultado;
- Estados: carregando (indicador), sucesso (resultado), erro (mensagem clara), vazio (aguardando input válido).

**Por que simulação via API e não cálculo local:** o backend é a fonte autoritativa da regra de precificação (ver §2.6). O frontend não replica fórmulas nem arredondamentos.

**Desacoplamento:** o componente visual de formulário e o de resultado são puros; um hook de aplicação (`usePricingSimulation`) encapsula a chamada via adaptador `pricingApi` (cliente tipado). A lógica de apresentação não conhece HTTP.

### 4.2 Domínio de Interface — Grid de Transações

**Responsabilidade:** exibir o histórico de liquidações com paginação server-side e filtros dinâmicos.

**Abordagem:**
- **Paginação server-side:** o grid envia `page`/`cursor` + filtros; o backend retorna o slice e um indicador de "tem mais";
- **Filtros dinâmicos** (período, cedente, moeda) no **estado da URL** — compartilháveis e persistíveis;
- TanStack Query gerencia cache e invalidação (filtro trocado → nova query; resultados cacheados por chave de filtro);
- Estados de carregamento/vazio/erro adequados;
- Colunas com formatação monetária respeitando a moeda (Intl.NumberFormat por `CurrencyCode`).

**Desacoplamento:** componentes de tabela e filtros são puros; hook `useLiquidations` encapsula a query do TanStack Query; o adaptador `liquidationApi` traduz o contrato do backend.

### 4.3 Arquitetura de Estado

- **Estado de servidor (dados do backend):** TanStack Query — cache, invalidação, mutações, retry controlado.
- **Estado de cliente compartilhado:** Zustand, somente quando necessário (ex.: preferências do painel, filtros de sessão visual que não precisam ir para a URL).
- **Estado de URL:** filtros, paginação, ordenação, busca.
- **Estado de formulário:** local ao formulário, validado com Zod.

Regra transversal: nenhum componente visual importa TanStack Query, Zustand ou o cliente HTTP diretamente.

---

## 5. Modelagem de Dados

### 5.1 Diagrama ER (conceitual)

```
CURRENCY (moedas)
  1 -----< CURRENCY_RATE (par, taxa, vigência) >----- 1 (par de moedas)

RECEIVABLE_TYPE (tipos de recebível + spread)
  1 -----< RECEIVABLE (valor face, moeda, vencimento, tipo, versão) >----- 1 (cedente)
      ^
      | (item do lote)
LIQUIDATION (lote, status, idempotency key)
  1 -----< LIQUIDATION_ITEM (valor presente, spread aplicado, valor líquido, moeda, taxa, prazo)
```

### 5.2 DDL essencial (Flyway)

```sql
CREATE TABLE currency (
    code        VARCHAR(3)  PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    scale       INT         NOT NULL DEFAULT 2
);

CREATE TABLE currency_rate (
    id           BIGSERIAL  PRIMARY KEY,
    base_code    VARCHAR(3) NOT NULL REFERENCES currency(code),
    quote_code   VARCHAR(3) NOT NULL REFERENCES currency(code),
    rate         NUMERIC(19, 8) NOT NULL CHECK (rate > 0),
    effective_at TIMESTAMPTZ NOT NULL,
    UNIQUE (base_code, quote_code, effective_at)
);

CREATE TABLE receivable_type (
    code   VARCHAR(32) PRIMARY KEY,
    name   VARCHAR(64) NOT NULL,
    spread NUMERIC(9, 6) NOT NULL
);

CREATE TABLE receivable (
    id                BIGSERIAL  PRIMARY KEY,
    external_ref      VARCHAR(64) NOT NULL UNIQUE,
    type_code         VARCHAR(32) NOT NULL REFERENCES receivable_type(code),
    face_value        NUMERIC(19, 4) NOT NULL,
    currency_code     VARCHAR(3) NOT NULL REFERENCES currency(code),
    maturity_date     DATE NOT NULL,
    assignor          VARCHAR(128),
    version           BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE liquidation (
    id                    BIGSERIAL  PRIMARY KEY,
    liquidation_request_id VARCHAR(64) NOT NULL UNIQUE,
    status                VARCHAR(16) NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE liquidation_item (
    id             BIGSERIAL PRIMARY KEY,
    liquidation_id BIGINT NOT NULL REFERENCES liquidation(id),
    receivable_id  BIGINT NOT NULL REFERENCES receivable(id),
    present_value  NUMERIC(19, 4) NOT NULL,
    spread_applied NUMERIC(9, 6) NOT NULL,
    term_months    NUMERIC(9, 6) NOT NULL,
    payout_amount  NUMERIC(19, 4) NOT NULL,
    payout_currency VARCHAR(3) NOT NULL REFERENCES currency(code),
    rate_applied   NUMERIC(19, 8),
    UNIQUE (liquidation_id, receivable_id)
);

CREATE INDEX idx_liquidation_item_created ON liquidation_item (liquidation_id);
CREATE INDEX idx_receivable_assignor ON receivable (assignor);
CREATE INDEX idx_liquidation_period ON liquidation (created_at);
```

### 5.3 Índices para o extrato

- `(created_at)` para filtro por período;
- `(assignor)` para filtro por cedente (joins a partir da liquidação);
- Índice composto na moeda de pagamento para filtro por moeda, definido conforme o plano de consulta real após carga de teste.

---

## 6. Requisitos Não Funcionais e Critérios de Aceite

### 6.1 Tratamento de exceções
- Erros esperados → códigos HTTP apropriados com mensagens claras;
- Erros inesperados → `500` genérico + log estruturado com `requestId`;
- Nenhuma falha comprometedora deixa a operação "pela metade".

### 6.2 Segurança
- Entradas validadas (bean validation + validação semântica de domínio);
- Consultas parametrizadas (sem injeção SQL);
- Segredos apenas por variáveis de ambiente;
- Erros sem detalhes internos; logs sem dados sensíveis.

### 6.3 Desempenho
- Sem N+1 (fetch estratégico em JPA);
- Extrato com paginação por cursor e SQL nativo com índices;
- Simulação com debounce (sem sobrecarga de requisições).

### 6.4 Escalabilidade
- Aplicação stateless; múltiplas instâncias;
- Optimistic Locking para concorrência;
- Health checks e shutdown gracioso.

### 6.5 Observabilidade
- Logs estruturados JSON;
- Métricas Micrometer + Prometheus;
- Health endpoints.

### 6.6 Critérios de aceite por recurso

| Recurso | Critérios |
| --- | --- |
| Câmbio | Criar/atualizar taxa (validando moeda e taxa positiva); obter vigente; histórico preservado; conflito de vigência → 409 |
| Precificação | Valor presente correto para duplicata (1,5%) e cheque (2,5%); cross-currency converte no final; casos de bordo (prazo zero, valor zero) validados |
| Liquidação | Lote atômico; conflito de versão → 409; idempotência (mesmo requestId não duplica); registro completo para auditoria |
| Extrato | Filtros por período/cedente/moeda; paginação estável; agregações corretas |
| Painel (FE) | Simulação em tempo real com debounce; estados de carregamento/sucesso/erro/vazio |
| Grid (FE) | Paginação server-side; filtros na URL; formatação monetária por moeda |

---

## 7. Decisões de Arquitetura (ADRs)

| ADR | Decisão | Motivo |
| --- | --- | --- |
| ADR-001 | Modular monolith por bounded context, com portas prontas para microserviços | Consistência transacional imediata sem custo de saga; evolução sem reescrita |
| ADR-002 | `BigDecimal` + `Money` VO + `NUMERIC` no banco | Precisão decimal obrigatória em ambiente financeiro |
| ADR-003 | Expoente decimal com precisão controlada | Potência decimal não é nativa em BigDecimal; erro limitado e corrigido no arredondamento final |
| ADR-004 | Strategy Pattern para spreads por tipo de recebível | Open/Closed; novos produtos sem tocar o motor |
| ADR-005 | Optimistic Locking para liquidação | Conflito raro; falha rápida; mais escalável que lock pessimista |
| ADR-006 | Escrita OLTP forte + projeções analíticas via eventos (CQRS leve) quando a volumetria justificar | Desacopla transação de leitura analítica sem complexidade antecipada |
| ADR-007 | JPA para domínio, SQL nativo (JdbcTemplate) para extrato | Performance de agregação; relatórios em 2 camadas |
| ADR-008 | SQL Server/RDBMS como fonte transacional única | ACID exigido pelo domínio financeiro |

---

## 8. Ordem de Implementação Sugerida (Roadmap)

1. **Fase 1 — Fundação:** modelagem de domínio, `Money`, esqueleto hexagonal, migrações Flyway, tratamento global de exceções.
2. **Fase 2 — Câmbio:** entidades, repos, casos de uso, endpoints de taxa, mock de provedor.
3. **Fase 3 — Precificação:** estratégias, motor, simulação, testes unitários da fórmula (duplicata/cheque, cross-currency).
4. **Fase 4 — Liquidação:** agregado, transação ACID, optimistic locking, idempotência, auditoria.
5. **Fase 5 — Extrato:** SQL nativo, cursor, filtros, índices.
6. **Fase 6 — Frontend:** painel do operador (simulação), grid de transações, arquitetura de estado.
7. **Fase 7 — Observabilidade e escala:** métricas, eventos de domínio, projeções analíticas, cache de taxas.
