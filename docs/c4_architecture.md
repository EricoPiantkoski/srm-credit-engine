# Diagrama C4 de Arquitetura — SRM Credit Engine

Este documento descreve a arquitetura do SRM Credit Engine seguindo o modelo C4 (Contexto → Containers → Componentes → Código), a partir da estrutura real do código-fonte. Os diagramas usam Mermaid e são renderizados nativamente pelo GitHub.

---

## 1. Diagrama de Contexto (Nível 1)

O sistema no centro de seu ambiente: quem o utiliza e com quais sistemas externos se relaciona.

```mermaid
flowchart LR
    subgraph Operador["Operação (Mesa de Câmbio)"]
        OperadorHumano["Operador de Mesa"]
    end

    subgraph Externo["Sistemas externos"]
        BCB["BCB PTAX\n(olinda.bcb.gov.br)\nFonte de câmbio oficial"]
        Awesome["AwesomeAPI\n(economia.awesomeapi.com.br)\nFonte de câmbio (BRL-USD)"]
    end

    SRM["SRM Credit Engine\n[Software System]\nPrecifica e liquida lotes de\nrecebíveis com multimoeda,\nidempotência e auditabilidade"]

    Banco["PostgreSQL 16\n[Dados]\nFonte transacional única"]

    OperadorHumano -->|"opera via UI/API"| SRM
    SRM -->|"consulta taxas de câmbio"| BCB
    SRM -->|"consulta cotação BRL-USD"| Awesome
    SRM -->|"persiste e consulta"| Banco
```

**Escopo:** o SRM Credit Engine é o sistema-alvo; o frontend (React) e o backend (Spring Boot) são descritos internamente nos níveis seguintes. O operador de mesa é o ator principal.

---

## 2. Diagrama de Containers (Nível 2)

A divisão em aplicações/deployables executáveis e o banco de dados.

```mermaid
flowchart LR
    OperadorHumano["Operador de Mesa\n[Person]"]

    subgraph Frontend["Frontend (React SPA)"]
        UI["Web Application\nReact 18 + TypeScript + Vite\nApresenta câmbio, recebíveis,\nsimulação e liquidação"]
    end

    subgraph Backend["Backend (monolito modular)"]
        API["Web Application\nSpring Boot 3 + Java 21\nArquitetura hexagonal\nMódulos: câmbio, precificação,\nliquidação, extrato"]
    end

    Banco["PostgreSQL 16\n[Dados]\nSchema versionado por Flyway"]

    BCB["BCB PTAX\n[Sistema externo]"]
    Awesome["AwesomeAPI\n[Sistema externo]"]

    OperadorHumano -->|"HTTPS / JSON"| UI
    UI -->|"HTTPS / REST / OpenAPI"| API
    API -->|"JDBC / SQL"| Banco
    API -->|"Feign / OData"| BCB
    API -->|"Feign / REST"| Awesome
```

**Decisões estruturais:**

- **Monolito modular**: um único processo Spring Boot com bounded contexts isolados por pacote (`cambio`, `precificacao`, `liquidacao`, `extrato`), comunicando-se por portas/contratos internos — ver `architecture_decision_records-architecture_definition.md`.
- **Banco único**: PostgreSQL como fonte transacional para os dois bounded contexts (transacional + analítico), sem réplicas — ver `architecture_decision_records-db_definition.md`.
- **Frontend independente**: SPA separada consumindo a API via REST; CORS liberado por configuração (`CORS_ALLOWED_ORIGINS`).

---

## 3. Diagrama de Componentes (Nível 3)

Os componentes internos do backend, organizados por módulo hexagonal. Cada módulo segue o mesmo padrão: `domain` (entidades/VOs/portas), `application` (casos de uso) e `infrastructure` (adapters web/persistência/externos).

### 3.1 Visão geral dos módulos e dependências

```mermaid
flowchart TD
    subgraph Cambio["Módulo Câmbio (com.srm.creditengine.cambio)"]
        CambioApp["Aplicação:\nTaxaCambioUpdater\nTaxaVigenteReader\nDinheiroConverter\nTaxaCambioOrchestrator"]
        CambioDom["Domínio:\nParMoedas, TaxaCambio\nTaxaCambioRepository, MoedaRepository\nTaxaCambioProvider"]
        CambioInf["Infra:\nTaxaCambioController\nTaxaCambioRepositoryAdapter\nBcbPtaxTaxaCambioProvider\nAwesomeApiBrcProvider\nTaxaCambioProviderRouter"]
    end

    subgraph Precificacao["Módulo Precificação (com.srm.creditengine.precificacao)"]
        PrecApp["Aplicação:\nPrecificacaoEngine\nRecebivelCreator, RecebivelQuery\nPrecificacaoSimulator"]
        PrecDom["Domínio:\nRecebivel, Spread, ResultadoPrecificacao\nPrecificacaoStrategy (+ implementações)\nPrecificacaoStrategyResolver\nRecebivelRepository, TipoRecebivelRepository\nMoedaCatalog, CambioGateway"]
        PrecInf["Infra:\nRecebivelController, SimulacaoController\nRecebivelRepositoryAdapter\nCambioGatewayAdapter, MoedaCatalogAdapter"]
    end

    subgraph Liquidacao["Módulo Liquidação (com.srm.creditengine.liquidacao)"]
        LiqApp["Aplicação:\nLiquidarLote\nConsultarLiquidacao"]
        LiqDom["Domínio:\nLiquidacao, ItemLiquidacao\nStatusLiquidacao\nRepositorioLiquidacao"]
        LiqInf["Infra:\nLiquidacaoController, ExtratoController\nLiquidacaoRepositoryAdapter"]
    end

    subgraph Extrato["Módulo Extrato (com.srm.creditengine.extrato)"]
        ExtApp["Aplicação:\nExtratoLiquidacoes"]
        ExtInf["Infra:\nExtratoController\nConsultaLiquidacaoAdapter (SQL nativo)"]
    end

    subgraph Shared["Módulo Compartilhado (com.srm.creditengine.shared)"]
        SharedDom["Domínio:\nDinheiro, CodigoMoeda\nDomainException"]
    end

    subgraph Infra["Infraestrutura Global (com.srm.creditengine.infrastructure)"]
        Global["GlobalExceptionHandler\nHealthController\nCorsConfig, OpenApiConfig"]
    end

    CambioInf --> CambioApp
    CambioApp --> CambioDom
    PrecApp --> PrecDom
    PrecInf --> PrecApp
    LiqApp --> LiqDom
    LiqInf --> LiqApp
    ExtApp --> ExtInf
    ExtApp --> LiqDom
    ExtApp --> LiqApp

    PrecDom -->|"usa portas do módulo Câmbio"| CambioDom
    PrecInf -->|"CambioGatewayAdapter usa TaxaVigenteReader/Updater"| CambioApp
    LiqApp -->|"reutiliza PrecificacaoEngine e resolver"| PrecApp
    LiqApp -->|"marcarLiquidado via porta RecebivelRepository"| PrecDom

    CambioDom --> SharedDom
    PrecDom --> SharedDom
    LiqDom --> SharedDom

    CambioInf --> Global
    PrecInf --> Global
    LiqInf --> Global
    ExtInf --> Global
```

### 3.2 Fluxo de uma liquidação (fluxo principal)

```mermaid
sequenceDiagram
    participant UI as Frontend React
    participant C as LiquidacaoController
    participant S as LiquidarLote (use case)
    participant PE as PrecificacaoEngine
    participant CG as CambioGateway
    participant R as RecebivelRepository
    participant D as PostgreSQL

    UI->>C: POST /api/liquidacoes {chaveIdempotencia, codigoMoedaPagamento, recebiveisIds}
    C->>S: executar(...)
    S->>D: verifica chave_idempotencia única
    alt chave duplicada
        D-->>S: conflito
        S-->>C: LiquidacaoConflictException
        C-->>UI: 409 Conflict
    end
    loop para cada recebível do lote
        S->>R: obter recebível (com version/status)
        S->>PE: precificar(recebível)
        PE-->>S: valor presente (BigDecimal)
        S->>CG: converter quando moedaPagamento ≠ moeda do ativo
        CG-->>S: valor pago na moeda de pagamento
        S->>R: marcarLiquidado(id, version) — UPDATE condicional
        R-->>S: 1 linha → ok / 0 linhas → conflito
    end
    S->>D: persiste liquidacao + itens (transação única)
    S-->>C: Liquidacao registrada
    C-->>UI: 201 Created
```

### 3.3 Fluxo de consulta de extrato

```mermaid
sequenceDiagram
    participant UI as Frontend React
    participant E as ExtratoController
    participant A as ExtratoLiquidacoes (use case)
    participant L as ConsultaLiquidacaoAdapter
    participant D as PostgreSQL

    UI->>E: GET /api/liquidacoes/extrato?dataInicial=&dataFinal=&status=&cedente=&lastId=&limit=
    E->>A: consultar(filtros)
    A->>L: consultar(filtros)
    L->>D: SQL nativo (li.id > lastId ORDER BY li.id LIMIT limit)
    D-->>L: página de itens
    L-->>A: página
    A-->>E: ExtratoLiquidacao[]
    E-->>UI: 200 OK (com lastId para próxima página)
```

---

## 4. Diagrama de Código (Nível 4)

No nível 4, o detalhe mais útil é o padrão hexagonal aplicado de forma consistente em **todos** os módulos. Um exemplo representativo do módulo Câmbio:

```mermaid
flowchart TD
    subgraph Web["adapter/in/web"]
        Ctrl["TaxaCambioController"]
    end
    subgraph App["application"]
        Updater["TaxaCambioUpdater"]
        Reader["TaxaVigenteReader"]
        Converter["DinheiroConverter"]
        Orchestrator["TaxaCambioOrchestrator"]
    end
    subgraph Dom["domain"]
        PortaIn["Portas de saída:\nTaxaCambioRepository\nMoedaRepository\nTaxaCambioProvider"]
        Entidades["ParMoedas, TaxaCambio"]
    end
    subgraph Pers["adapter/out/persistence"]
        PersAdapter["TaxaCambioRepositoryAdapter\nMoedaRepositoryAdapter"]
        Jpa["TaxaCambioJpaEntity\nMoedaJpaEntity"]
    end
    subgraph Ext["adapter/out/external"]
        BcbProvider["BcbPtaxTaxaCambioProvider"]
        AwesomeProvider["AwesomeApiBrcProvider"]
        Router["TaxaCambioProviderRouter"]
        Clients["BcbPtaxClient (Feign)\nAwesomeApiBrcClient (Feign)"]
    end
    subgraph Db["PostgreSQL"]
        Tb["tabelas taxa_cambio / moeda"]
    end
    subgraph Remote["Serviços remotos"]
        BcbRemote["BCB PTAX OData"]
        AwesomeRemote["AwesomeAPI REST"]
    end

    Ctrl --> Updater
    Ctrl --> Reader
    Ctrl --> Converter
    Orchestrator --> PortaIn
    Updater --> PortaIn
    Reader --> PortaIn
    Converter --> PortaIn
    PortaIn --> PersAdapter
    PortaIn --> Router
    PersAdapter --> Jpa --> Db
    Router --> BcbProvider --> Clients
    Router --> AwesomeProvider --> Clients
    Clients --> BcbRemote
    Clients --> AwesomeRemote
```

A regra é uniforme nos demais módulos: **a aplicação depende de portas (interfaces) no domínio; a infraestrutura implementa essas portas** — o fluxo de dependências aponta sempre para o núcleo, nunca do domínio para a infraestrutura.

---

## 5. Referências

- [TechDoc](TechDoc.md) — documentação técnica completa por módulo (contratos, persistência, configuração).
- [Decisões de Arquitetura](architecture_decision_records-architecture_definition.md) — racional da topologia (monolito modular × microserviços) e do repositório único.
- [Banco de Dados](architecture_decision_records-db_definition.md) — decisão do PostgreSQL (CAP/PACELC, ACID).
- [Decisões de Precificação e Liquidação](architecture_decision_records-precificacao_liquidacao.md) — Strategy, precisão e optimistic locking.
- [Modelo de Dados](database_model.md) — ER, DDL e convenções do schema.