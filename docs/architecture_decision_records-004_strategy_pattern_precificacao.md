# ADR-004 — Strategy Pattern para precificação por tipo de recebível

## Status
Aceito (implementado no Ponto 5).

## Contexto

A precificação varia por `tipo_recebivel` (duplicata mercantil, cheque pré-datado, e futuramente outros). O motor de precificação deve aplicar a regra correta sem conhecer os tipos concretos, e novos tipos devem poder ser adicionados sem alterar o motor nem a API.

## Decisão

- Porta de domínio `PrecificacaoStrategy` com `spreadFor(Recebivel)`.
- Implementações concretas por tipo: `DuplicataMercantilStrategy` (spread 1,5%) e `ChequePreDatadoStrategy` (spread 2,5%).
- `PrecificacaoStrategyResolver` faz a resolução por `codigoTipo` e lança `UnknownReceivableTypeException` para tipos desconhecidos.
- O motor (`PrecificacaoEngine`) recebe a estratégia resolvida e não conhece os tipos; a fórmula de desconto é comum e a diferença entre tipos é o spread.
- O catálogo de estratégias é composto em `PrecificacaoConfig` (beans), independente do banco; `tipo_recebivel` no banco é dado de referência.

## Consequências

- Novo tipo de recebível = nova implementação de `PrecificacaoStrategy` registrada no resolver/config, sem mudanças no motor, controller ou banco.
- `codigoTipo` desconhecido ou vazio → 422 (via `UnknownReceivableTypeException`).
- O spread é componente estático da estratégia (regra de negócio), não configurado por linha do banco — decisão revista no Ponto 6/7 se o spread passar a ser parametrizado por contrato.

## Alternativas consideradas

- Switch/if por código no motor: acoplamento e violação do Open/Closed; descartado.
- Configuração de spread por tabela/linha: mais flexível, porém antecipa parametrização ainda não exigida pelo domínio.