# ADR — Controle de concorrência na Liquidação (Optimistic Locking)

## Status
Implementado no Ponto 7.

## Contexto

A liquidação de um lote de recebíveis é a operação que não pode ficar "pela metade": um lote é precificado, convertido e registrado como uma única transação ACID. Dois operadores (ou duas requisições simultâneas) podem tentar liquidar o **mesmo recebível** ao mesmo tempo — e isso não pode produzir dupla liquidação nem estado inconsistente.

Existem duas ameaças de concorrência:

1. **Mesma requisição repetida** (retry de rede, clique duplo): resolvida por **idempotência** via `chaveIdempotencia` — a mesma chave nunca cria uma segunda liquidação.
2. **Requisições diferentes sobre o mesmo recebível**: resolvida por **Optimistic Locking** — a versão do recebível é incrementada a cada liquidação; a primeira vence, a segunda recebe `409 Conflict`.

Este ADR detalha a estratégia do segundo caso e o comportamento em retry com itens já precificados.

## Decisão

### 1. Lock otimista por recebível (item), não por lote

- A unidade de concorrência é o **recebível** (`Recebivel.version`), não o lote.
- Motivo: um lote pode conter recebíveis independentes; bloquear o lote inteiro criaria contenção desnecessária e não reflete a realidade de que dois lotes distintos podem compartilhar recebíveis.
- Implementação: `@Version` no `RecebivelJpaEntity` (coluna `version` já existente no `V1`, `BIGINT NOT NULL DEFAULT 0`).

### 1.1 `version` sozinho não impede dupla liquidação sequencial

O `version` detecta apenas **concorrência simultânea** (duas transações ativas ao mesmo tempo lendo a mesma versão). Duas liquidações **sequenciais** do mesmo recebível com chaves de idempotência diferentes passavam: a segunda lê o `version` já incrementado pela primeira (0 → 1) e incrementa novamente (1 → 2). O recebível era liquidado mais de uma vez.

**Correção (V4):** o recebível ganhou um **estado de ciclo de vida** (`status`: `DISPONIVEL` → `LIQUIDADO`, migração `V4__recebivel_status.sql`). A baixa tornou-se **atômica e condicional** no mesmo UPDATE:

```sql
UPDATE recebivel SET version = version + 1, status = 'LIQUIDADO'
WHERE id = ? AND version = ? AND status = 'DISPONIVEL'
```

- **1 linha afetada** → o recebível estava disponível e foi liquidado.
- **0 linhas afetadas** → ou outra transação o modificou (concorrência) **ou** ele já está `LIQUIDADO` (dupla liquidação sequencial). Ambos → `LiquidacaoVersionConflictException` → **409**.

Assim o mesmo UPDATE garante simultaneamente o lock otimista (via `version`) e a regra de negócio "um recebível só é liquidado uma vez" (via `status`), sem race window entre duas operações.

### 2. Transação atômica do lote

- Todo o fluxo de liquidação roda em **uma única transação** (`@Transactional`): precificação → conversão → persistência de cabeçalho + itens.
- Se qualquer recebível falhar, tudo é revertido (rollback total).
- A atualização do `version` de cada recebível acontece **dentro** desta transação, no momento da baixa.

### 3. Fluxo de conflito

- Duas liquidações simultâneas sobre o mesmo recebível:
  - A **primeira** faz `UPDATE recebivel SET version = version + 1 WHERE id = ? AND version = ?` e vence;
  - A **segunda** encontra `0` linhas afetadas → `LiquidacaoVersionConflictException` → **`409 Conflict`**.
- O `GlobalExceptionHandler` traduz a exceção → `409` com orientação explícita de reprocessamento (mensagem em inglês, campo `resolution`), e o lote inteiro da segunda é revertido (rollback).
- O operador **não fica sem resposta**: recebe o conflito identificado e a sugestão de reprocessar com dados atualizados.

### 3.1 Como funciona o incremento de `version` na prática

O `version` não é um status — é um **contador que funciona como assinatura de versão** do recebível. Ele sobe a cada modificação e serve para detectar se outra transação mexeu no registro enquanto a atual trabalhava. O mecanismo, passo a passo:

1. **Criação**: a coluna `version` nasce em `0` (`BIGINT NOT NULL DEFAULT 0` no `V1`). Na entidade JPA, o campo é anotado com `@Version` (`RecebivelJpaEntity`), e o Hibernate passa a gerenciá-lo automaticamente em todo `UPDATE` — ninguém incrementa na mão.
2. **Leitura**: ao carregar o recebível (`obtainById`), o objeto de domínio guarda o `version` atual. Ex.: `version = 0`.
3. **Baixa**: antes de registrar a liquidação, o `LiquidarLote` chama `tryBumpVersion(recebivel.id(), recebivel.version())`, que executa um UPDATE atômico:
   ```sql
   UPDATE recebivel SET version = version + 1 WHERE id = ? AND version = ?
   ```
   O segredo está no `WHERE version = ?` (versão esperada, capturada no passo 2).
4. **Dois cenários de resultado**:
   - **1 linha afetada** → a versão esperada bateu com a do banco; ninguém mexeu no recebível. A liquidação pode prosseguir e o `version` passa a ser `1`.
   - **0 linhas afetadas** → entre a leitura e o UPDATE, **outra transação já liquidou** o recebível e incrementou o `version` para `1`. A condição `version = 0` deixou de ser verdadeira, o UPDATE não afetou nada e o sistema lança `LiquidacaoVersionConflictException` → **`409 Conflict`**. O lote inteiro é revertido.

**Por que isso importa:** sem o `version`, duas requisições simultâneas liquidando o mesmo recebível poderiam ambas "passar" e gerar duas liquidações para o mesmo ativo — pagamento duplicado ao cedente. O `version` garante que **apenas a primeira vence**; a segunda recebe `409` e o operador reprocessa com os dados atuais. O mesmo mecanismo vale para o caso do `UPDATE` falhar por concorrência de outro tipo de modificação no recebível, não apenas liquidação.

### 4. Retry com itens já precificados

- A precificação é **stateless e read-only** (não persiste nada); o retry **recalcula** os itens sobre os dados atuais do recebível.
- Num retry após conflito, o valor presente/spread/prazo são **recomputados** na transação do retry — nunca reaproveitados de um cálculo obsoleto — garantindo que a liquidação reflita o estado vigente do recebível e da taxa de câmbio.
- Consequência: o item de retry pode apresentar valores ligeiramente diferentes se a taxa de câmbio vigente mudou entre as tentativas — comportamento **correto** (a liquidação usa a taxa vigente no momento em que é registrada) e auditável (cada item registra a taxa aplicada).

### 5. Idempotência como camada complementar

- `chaveIdempotencia` (única no banco) é verificada **antes** da transação; duplicata → `409` sem tocar os recebíveis.
- Retry de rede com a **mesma** chave não dispara o Optimistic Locking nem duplica itens.
- Chaves diferentes sobre o mesmo recebível → fluxo de concorrência descrito acima.

## Consequências

- Conflito real em operação de mesa é raro; o custo do otimista (uma coluna `version` + falha rápida) é baixo e mais escalável que lock pessimista.
- Requisito de auditoria atendido: cada item registra valor face, spread, prazo, valor presente, moeda, taxa e valor líquido; nada é sobrescrito, apenas registrado.
- Se o spread passar a ser parametrizado por contrato (decisão em aberto do ADR-004), o retry recalcularia sobre o spread vigente — revisitar este ADR nesse cenário.

## Alternativas consideradas

- **Lock pessimista** (`SELECT ... FOR UPDATE`): justificável se conflito fosse frequente; segura locks de banco por operação sem ganho para a baixa taxa de conflito da mesa. Descartado.
- **Lock por lote** (sessão/série do lote): contenção desnecessária — recebíveis de lotes diferentes precisam concorrer livremente. Descartado.
- **Rejeitar sem orientação**: conflito silencioso deixaria o operador sem caminho de recuperação. Descartado — a aplicação reage de forma controlada com `409` + reprocessamento.