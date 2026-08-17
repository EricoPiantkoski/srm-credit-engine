# ADR-003 — Precisão da exponenciação na precificação

## Status
Aceito (implementado no Ponto 5).

## Contexto

A fórmula de precificação é `valorPresente = valorFace / (1 + taxaBase + spread) ^ prazoMeses`, com prazo expresso em meses e admitindo fração (`dias / 30`). `BigDecimal.pow(int)` exige expoente inteiro; potências decimais em `BigDecimal` não são suportadas nativamente.

## Decisão

- Prazo inteiro (escala 0 após `stripTrailingZeros`): `BigDecimal.pow(int)` — sem perda de precisão.
- Prazo fracionário: `Math.pow` sobre `double` apenas no expoente (`base.doubleValue()`, `exponent.doubleValue()`), com o arredondamento final controlado pela escala de `Dinheiro`/`valorFace` (HALF_EVEN) na divisão que produz o valor presente.
- A margem de erro da exponenciação em ponto flutuante é aceita **somente** na dimensão de prazo fracionário e corrigida no arredondamento final.

## Consequências

- Expoente sempre é resultado de `dias / 30` com escala 6 (HALF_EVEN), mantendo a diferença inteira/fracionária determinística.
- Fórmula de prazo inteiro (múltiplo de 30 dias) permanece exata em `BigDecimal`.
- Exemplos numéricos fixados nos testes: duplicata BRL 1000,00 / 30 dias / 1,5% → ≈ 985,22; cheque / 2,5% → ≈ 975,61; prazo 15 dias → ≈ 992,59.

## Alternativas consideradas

- `BigDecimal.pow` com escala pré-definida e divisão posterior: inviável para expoente decimal.
- Bibliotecas de aritmética decimal arbitrária (ex.: `Apfloat`): dependência externa desnecessária para o grau de precisão exigido no arredondamento final.