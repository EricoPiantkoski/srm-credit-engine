# Precificação e Liquidação

Este documento reúne as decisões de arquitetura do núcleo de precificação e liquidação do SRM Credit Engine: o uso do padrão `Strategy` para as regras de risco por tipo de recebível, a precisão da exponenciação na fórmula de precificação e o controle de concorrência na liquidação.

## 1. Strategy Pattern para precificação por tipo de recebível

### Contexto

A precificação varia por `tipo_recebivel` (duplicata mercantil, cheque pré-datado). O motor de precificação deve aplicar a regra correta sem conhecer os tipos concretos, e novos tipos podem ser adicionados sem alterar o motor ou a API.

### Decisão

- Porta de domínio `PrecificacaoStrategy` com `spreadFor(Recebivel)`.
- Implementações concretas por tipo: `DuplicataMercantilStrategy` (spread 1,5%) e `ChequePreDatadoStrategy` (spread 2,5%).
- `PrecificacaoStrategyResolver` faz a resolução por `codigoTipo` e lança `UnknownReceivableTypeException` para tipos desconhecidos.
- O motor (`PrecificacaoEngine`) recebe a estratégia resolvida e não conhece os tipos; a fórmula de desconto é comum e a diferença entre tipos é o spread.
- O catálogo de estratégias é composto em `PrecificacaoConfig` (beans), independente do banco; `tipo_recebivel` no banco é dado de referência.

### Consequências

- Novo tipo de recebível = nova implementação de `PrecificacaoStrategy` registrada no resolver/config, sem mudanças no motor, controller ou banco.
- `codigoTipo` desconhecido ou vazio → 422 (via `UnknownReceivableTypeException`).
- O spread é componente estático da estratégia (regra de negócio), não configurado por linha do banco — decisão a ser revista se o spread passar a ser parametrizado por contrato.

### Alternativas consideradas

- Switch/if por código no motor: acoplamento e violação do Open/Closed; descartado.
- Configuração de spread por tabela/linha: mais flexível, porém antecipa parametrização ainda não exigida pelo domínio.

## 2. Precisão da exponenciação na precificação

### Contexto

A fórmula de precificação é `valorPresente = valorFace / (1 + taxaBase + spread) ^ prazoMeses`, com prazo expresso em meses e admitindo fração (`dias / 30`). `BigDecimal.pow(int)` exige expoente inteiro; potências decimais em `BigDecimal` não são suportadas nativamente.

### Decisão

- Prazo inteiro (escala 0 após `stripTrailingZeros`): `BigDecimal.pow(int)` — sem perda de precisão.
- Prazo fracionário: `Math.pow` sobre `double` apenas no expoente (`base.doubleValue()`, `exponent.doubleValue()`), com o arredondamento final controlado pela escala de `Dinheiro`/`valorFace` (HALF_EVEN) na divisão que produz o valor presente.
- A margem de erro da exponenciação em ponto flutuante é aceita **somente** na dimensão de prazo fracionário e corrigida no arredondamento final.

### Consequências

- Expoente sempre é resultado de `dias / 30` com escala 6 (HALF_EVEN), mantendo a diferença inteira/fracionária determinística.
- Fórmula de prazo inteiro (múltiplo de 30 dias) permanece exata em `BigDecimal`.
- Exemplos numéricos fixados nos testes: duplicata BRL 1000,00 / 30 dias / 1,5% → ≈ 985,22; cheque / 2,5% → ≈ 975,61; prazo 15 dias → ≈ 992,59.

### Alternativas consideradas

- `BigDecimal.pow` com escala pré-definida e divisão posterior: inviável para expoente decimal.
- Bibliotecas de aritmética decimal arbitrária (ex.: `Apfloat`): dependência externa desnecessária para o grau de precisão exigido no arredondamento final.

## 3. Controle de concorrência na Liquidação (Optimistic Locking)

### Contexto

A liquidação de um lote de recebíveis é a operação que não pode ficar "pela metade": um lote é precificado, convertido e registrado como uma única transação ACID. Dois operadores (ou duas requisições simultâneas) podem tentar liquidar o **mesmo recebível** ao mesmo tempo — e isso não pode produzir dupla liquidação nem estado inconsistente.

Existem duas ameaças de concorrência:

1. **Mesma requisição repetida** (retry de rede, clique duplo): resolvida por **idempotência** via `chaveIdempotencia` (UUID gerado pelo cliente) — a mesma chave nunca cria uma segunda liquidação.
2. **Requisições diferentes sobre o mesmo recebível**: resolvida por **Optimistic Locking** — a versão do recebível é incrementada a cada liquidação; a primeira vence, a segunda recebe `409 Conflict`.

Esta seção detalha a estratégia do segundo caso e o comportamento em retry com itens já precificados.

### Decisão

#### 3.1 Lock otimista por recebível (item), não por lote

- A unidade de concorrência é o **recebível** (`Recebivel.version`), não o lote.
- Motivo: um lote pode conter recebíveis independentes; bloquear o lote inteiro criaria contenção desnecessária e não reflete a realidade de que dois lotes distintos podem compartilhar recebíveis.
- Implementação: `@Version` no `RecebivelJpaEntity` (coluna `version` existente desde o `V1`, `BIGINT NOT NULL DEFAULT 0`).

#### 3.2 `version` sozinho não impede dupla liquidação sequencial

O `version` detecta apenas **concorrência simultânea** (duas transações ativas ao mesmo tempo lendo a mesma versão). Duas liquidações **sequenciais** do mesmo recebível com chaves de idempotência diferentes passavam: a segunda lê o `version` já incrementado pela primeira (0 → 1) e incrementa novamente (1 → 2). O recebível era liquidado mais de uma vez.

**Correção (V4):** o recebível ganhou um **estado de ciclo de vida** (`status`: `DISPONIVEL` → `LIQUIDADO`, migração `V4__recebivel_status.sql`). A baixa tornou-se **atômica e condicional** no mesmo UPDATE:

```sql
UPDATE recebivel SET version = version + 1, status = 'LIQUIDADO'
WHERE id = ? AND version = ? AND status = 'DISPONIVEL'
```

- **1 linha afetada** → o recebível estava disponível e foi liquidado.
- **0 linhas afetadas** → ou outra transação o modificou (concorrência) **ou** ele já está `LIQUIDADO` (dupla liquidação sequencial). Ambos → `LiquidacaoVersionConflictException` → **409**.

Assim o mesmo UPDATE garante simultaneamente o lock otimista (via `version`) e a regra de negócio "um recebível só é liquidado uma vez" (via `status`), sem race window entre duas operações.

#### 3.3 Como funciona o incremento de `version` na prática

O `version` não é um status — é um **contador que funciona como assinatura de versão** do recebível. Ele sobe a cada modificação e serve para detectar se outra transação mexeu no registro enquanto a atual trabalhava. O mecanismo, passo a passo:

1. **Criação**: a coluna `version` nasce em `0`. Na entidade JPA, o campo é anotado com `@Version`, e o Hibernate passa a gerenciá-lo automaticamente em todo `UPDATE` — ninguém incrementa na mão.
2. **Leitura**: ao carregar o recebível (`obtainById`), o objeto de domínio guarda o `version` atual. Ex.: `version = 0`.
3. **Baixa**: antes de registrar a liquidação, o `LiquidarLote` chama `marcarLiquidado(recebivel.id(), recebivel.version())`, que executa o UPDATE atômico da seção 3.2. O segredo está no `WHERE version = ? AND status = 'DISPONIVEL'` (versão e estado esperados, capturados no passo 2).
4. **Dois cenários de resultado**:
   - **1 linha afetada** → a versão esperada bateu com a do banco e o recebível estava disponível; a liquidação prossegue e o `version` passa a ser `1` com `status = 'LIQUIDADO'`.
   - **0 linhas afetadas** → entre a leitura e o UPDATE, **outra transação liquidou** o recebível (versão ou status divergentes). O sistema lança `LiquidacaoVersionConflictException` → **`409 Conflict`**. O lote inteiro é revertido.

**Por que isso importa:** sem o `version`, duas requisições simultâneas liquidando o mesmo recebível poderiam ambas "passar" e gerar duas liquidações para o mesmo ativo — pagamento duplicado ao cedente. O `version` garante que **apenas a primeira vence**; a segunda recebe `409` e o operador reprocessa com os dados atuais.

#### 3.4 Transação atômica do lote

- Todo o fluxo de liquidação roda em **uma única transação** (`@Transactional`): precificação → conversão → persistência de cabeçalho + itens.
- Se qualquer recebível falhar, tudo é revertido (rollback total).
- A atualização do `version`/`status` de cada recebível acontece **dentro** desta transação, no momento da baixa.

#### 3.5 Fluxo de conflito

- Duas liquidações simultâneas sobre o mesmo recebível:
  - A **primeira** executa o UPDATE condicional e vence;
  - A **segunda** encontra `0` linhas afetadas → `LiquidacaoVersionConflictException` → **`409 Conflict`**.
- O `GlobalExceptionHandler` traduz a exceção → `409` com orientação explícita de reprocessamento (mensagem em inglês, campo `resolution`), e o lote inteiro da segunda é revertido (rollback).
- O operador **não fica sem resposta**: recebe o conflito identificado e a sugestão de reprocessar com dados atualizados.

#### 3.6 Retry com itens já precificados

- A precificação é **stateless e read-only** (não persiste nada); o retry **recalcula** os itens sobre os dados atuais do recebível.
- Num retry após conflito, o valor presente/spread/prazo são **recomputados** na transação do retry — nunca reaproveitados de um cálculo obsoleto — garantindo que a liquidação reflita o estado vigente do recebível e da taxa de câmbio.
- Consequência: o item de retry pode apresentar valores ligeiramente diferentes se a taxa de câmbio vigente mudou entre as tentativas — comportamento **correto** (a liquidação usa a taxa vigente no momento em que é registrada) e auditável (cada item registra a taxa aplicada).

#### 3.7 Idempotência como camada complementar

- `chaveIdempotencia` (UUID, única no banco) é verificada **antes** da transação; duplicata → `409` sem tocar os recebíveis.
- Retry de rede com a **mesma** chave não dispara o Optimistic Locking nem duplica itens.
- Chaves diferentes sobre o mesmo recebível → fluxo de concorrência descrito acima.

### Consequências

- Conflito real em operação de mesa é raro; o custo do otimista (uma coluna `version` + falha rápida) é baixo e mais escalável que lock pessimista.
- Requisito de auditoria atendido: cada item registra valor face, spread, prazo, valor presente, moeda, taxa e valor líquido; nada é sobrescrito, apenas registrado.
- Se o spread passar a ser parametrizado por contrato, o retry recalcularia sobre o spread vigente — revisitar esta decisão nesse cenário.

### Alternativas consideradas

- **Lock pessimista** (`SELECT ... FOR UPDATE`): justificável se conflito fosse frequente; segura locks de banco por operação sem ganho para a baixa taxa de conflito da mesa. Descartado.
- **Lock por lote** (sessão/série do lote): contenção desnecessária — recebíveis de lotes diferentes precisam concorrer livremente. Descartado.
- **Rejeitar sem orientação**: conflito silencioso deixaria o operador sem caminho de recuperação. Descartado — a aplicação reage de forma controlada com `409` + reprocessamento.
