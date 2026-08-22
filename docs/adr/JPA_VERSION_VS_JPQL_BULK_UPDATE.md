# ADR-009: JPA @Version vs JPQL Bulk Update

## Status
Accepted

## Context
A entidade `RecebivelJpaEntity` possui anotação `@Version` para optimistic locking. O método `marcarLiquidado` no `RecebivelRepository` usa uma JPQL bulk update para marcar o recebível como liquidado:

```java
@Modifying
@Query("UPDATE RecebivelJpaEntity r SET r.liquidado = true WHERE r.id = :id AND r.version = :version")
int marcarLiquidado(@Param("id") Long id, @Param("version") Long version);
```

O problema é que **JPQL bulk updates não disparam o incremento automático do campo `@Version` do Hibernate**. O Hibernate apenas incrementa a versão quando a entidade é gerenciada (carregada no contexto de persistência e modificada via setter).

## Comportamento Atual
- O JPQL bulk update **não incrementa** o campo `version`
- A verificação `WHERE r.version = :version` garante que o update só ocorra se a versão corresponder ao esperado
- Isso **funciona** para detecção de concorrência no momento do update
- MAS: se a entidade estiver no contexto de persistência (cache de primeiro nível), ela ficará **desatualizada** (stale) com a versão antiga

## Riscos
1. **Entidade stale no cache**: Se a entidade foi carregada antes do bulk update, o objeto em memória terá versão desatualizada
2. **Inconsistência silenciosa**: Próximas operações na mesma transação podem usar dados desatualizados
3. **Flush subsequente**: Se a entidade for modificada depois e houver flush, o Hibernate tentará incrementar a versão baseada no valor stale

## Mitigação Atual
- O método `marcarLiquidado` é chamado dentro de uma transação onde a entidade **não foi carregada** anteriormente no contexto de persistência
- O fluxo: `RecebivelRepository.obtainById` (carrega) → `marcarRecebivelLiquidado` (bulk update) → fim da transação
- Como a entidade não é reutilizada após o bulk update na mesma transação, não há problema prático hoje

## Decisão
**Manter a implementação atual** com as seguintes salvaguardas:
1. Documentar que `marcarLiquidado` **não deve** ser usado quando a entidade já está no contexto de persistência
2. Adicionar teste de integração verificando que concurrent updates são rejeitados corretamente
3. Considerar migrar para `LockModeType.PESSIMISTIC_WRITE` + entity update se o padrão de uso mudar

## Alternativas Consideradas
1. **Remover @Version e usar lock pessimista**: Mais complexo, requer SELECT FOR UPDATE
2. **Carregar entidade + setter + save**: Mais lento (2 queries), mas mantém cache consistente
3. **Usar @Version + native query com RETURNING**: Específico de PostgreSQL, menos portável

## Consequências
- **Positivo**: Performance (single query), detecção de concorrência funciona
- **Negativo**: Risco teórico de entidade stale se padrão de uso mudar
- **Neutro**: Requer disciplina da equipe para não carregar entidade antes do bulk update

## Revisão
Revisar em 6 meses ou quando houver mudança no padrão de uso de `marcarLiquidado`.