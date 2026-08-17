# Guia de Teste Manual End-to-End

Este guia valida a API local em ordem de dependência usando macOS, `curl` e `jq`.

## 1. Preparação

Instale Docker, Java 21, Node 20 ou superior, pnpm e jq. Suba o banco:

```bash
docker compose up -d
```

Em um terminal separado, suba o backend:

```bash
(cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local)
```

Para validar também a interface, suba o frontend em outro terminal:

```bash
(cd frontend && pnpm install --frozen-lockfile && pnpm dev)
```

Defina as variáveis do roteiro:

```bash
export API_BASE_URL=http://localhost:8080
export FUTURE_DATE=$(date -u -v+30d +%Y-%m-%d)
export VIGENCIA=$(date -u +%Y-%m-%dT%H:%M:%SZ)
```

No Linux, use `date -u -d '+30 days' +%Y-%m-%d` para gerar `FUTURE_DATE`.

## 2. Health

O health da aplicação é público e deve responder `200`:

```bash
curl -i "$API_BASE_URL/api/health"
```

Resposta esperada: `{"status":"UP"}`.

## 3. Autenticação

O seed do perfil local cria `admin` com a senha `admin123`. Essa credencial é exclusiva para desenvolvimento local.

Faça login e extraia o par de tokens:

```bash
LOGIN_JSON=$(curl -sS -X POST "$API_BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}')
printf '%s\n' "$LOGIN_JSON" | jq
export ACCESS_TOKEN=$(printf '%s' "$LOGIN_JSON" | jq -r '.accessToken')
export REFRESH_TOKEN=$(printf '%s' "$LOGIN_JSON" | jq -r '.refreshToken')
```

O resultado esperado é `200` com `accessToken`, `accessTokenExpiresAt`, `refreshToken` e `refreshTokenExpiresAt`.

Credenciais inválidas devem responder `401`:

```bash
curl -i -X POST "$API_BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"senha-invalida"}'
```

Renove o par de tokens. O refresh antigo é revogado:

```bash
REFRESH_JSON=$(curl -sS -X POST "$API_BASE_URL/api/auth/refresh" \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
export ACCESS_TOKEN=$(printf '%s' "$REFRESH_JSON" | jq -r '.accessToken')
export REFRESH_TOKEN=$(printf '%s' "$REFRESH_JSON" | jq -r '.refreshToken')
printf '%s\n' "$REFRESH_JSON" | jq
```

O resultado esperado é `200`. Reenviar o refresh anterior deve responder `401`. Execute o logout na seção final para manter o access token disponível durante o roteiro.

## 4. Câmbio

Grave uma taxa manual para `USD/BRL`:

```bash
curl -i -X PUT "$API_BASE_URL/api/taxas-cambio" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"codigoBase\":\"USD\",\"codigoCotacao\":\"BRL\",\"taxa\":5.25,\"vigencia\":\"$VIGENCIA\"}"
```

O resultado esperado é `200` com `codigoBase`, `codigoCotacao`, `taxa` e `vigencia`. Repetir a mesma vigência deve responder `409`.

Consulte a taxa vigente:

```bash
curl -i "$API_BASE_URL/api/taxas-cambio/vigente?codigoBase=USD&codigoCotacao=BRL" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

O resultado esperado é `200`.

Teste a integração externa. Com o provedor disponível, o resultado esperado é `200`; indisponibilidade externa deve resultar em `503` com `resolution`:

```bash
curl -i -X POST "$API_BASE_URL/api/taxas-cambio/integracao?codigoBase=BRL&codigoCotacao=USD" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Converta um valor:

```bash
curl -i -X POST "$API_BASE_URL/api/taxas-cambio/convert" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"valor":100,"codigoMoeda":"USD","escala":2,"codigoBase":"USD","codigoCotacao":"BRL"}'
```

O resultado esperado é `200` com `valor`, `codigoMoeda`, `appliedTaxa` e `vigencia`.

## 5. Recebíveis

Crie um recebível e guarde o ID retornado:

```bash
RECEIVABLE_JSON=$(curl -sS -X POST "$API_BASE_URL/api/recebiveis" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"referenciaExterna\":\"manual-$(date +%s)\",\"codigoTipo\":\"DUPLICATA_MERCANTIL\",\"valorFace\":1000,\"codigoMoeda\":\"BRL\",\"dataVencimento\":\"$FUTURE_DATE\",\"cedente\":\"Cedente Manual\"}")
printf '%s\n' "$RECEIVABLE_JSON" | jq
export RECEIVABLE_ID=$(printf '%s' "$RECEIVABLE_JSON" | jq -r '.id')
```

O resultado esperado é `201` com status `DISPONIVEL`. Liste os registros:

```bash
curl -i "$API_BASE_URL/api/recebiveis?cedente=Cedente%20Manual&page=0&size=20" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

O resultado esperado é `200` com uma lista contendo o recebível.

## 6. Simulação

Simule a precificação sem persistência:

```bash
curl -i -X POST "$API_BASE_URL/api/simulacoes/precificacao" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"codigoTipo\":\"DUPLICATA_MERCANTIL\",\"valorFace\":1000,\"codigoMoeda\":\"BRL\",\"dataVencimento\":\"$FUTURE_DATE\",\"codigoMoedaPagamento\":\"BRL\"}"
```

O resultado esperado é `200` com `valorPresente`, `spreadAplicado`, `prazoMeses`, `valorLiquido` e os campos de moeda/taxa.

Um tipo inexistente deve responder `422`:

```bash
curl -i -X POST "$API_BASE_URL/api/simulacoes/precificacao" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"codigoTipo\":\"TIPO_INEXISTENTE\",\"valorFace\":1000,\"codigoMoeda\":\"BRL\",\"dataVencimento\":\"$FUTURE_DATE\",\"codigoMoedaPagamento\":\"BRL\"}"
```

## 7. Liquidação

Crie uma liquidação com chave UUID nova:

```bash
export IDEMPOTENCY_KEY=$(uuidgen | tr '[:upper:]' '[:lower:]')
LIQUIDATION_JSON=$(curl -sS -X POST "$API_BASE_URL/api/liquidacoes" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"chaveIdempotencia\":\"$IDEMPOTENCY_KEY\",\"codigoMoedaPagamento\":\"BRL\",\"recebiveisIds\":[$RECEIVABLE_ID]}")
printf '%s\n' "$LIQUIDATION_JSON" | jq
export LIQUIDATION_ID=$(printf '%s' "$LIQUIDATION_JSON" | jq -r '.id')
```

O resultado esperado é `201`, status `LIQUIDADA` e um item associado ao recebível.

Consulte a liquidação:

```bash
curl -i "$API_BASE_URL/api/liquidacoes/$LIQUIDATION_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

## 8. Extrato

Consulte o extrato com filtros e paginação:

```bash
curl -i "$API_BASE_URL/api/liquidacoes/extrato?dataInicial=2020-01-01&dataFinal=2099-12-31&status=LIQUIDADA&cedente=Cedente%20Manual&codigoMoedaPagamento=BRL&limit=50" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

O resultado esperado é `200` com os itens. Use o maior `itemId` como `lastId` na próxima consulta. `limit=501` deve responder `400`.

## 9. Auditoria

Não existe endpoint HTTP de consulta para `audit_log`; a consulta é feita diretamente no PostgreSQL local:

```bash
docker compose exec -T postgres psql -U postgres -d srm_credit -c "SELECT id, username, acao, recurso, resultado, chave_idempotencia, request_id, created_at FROM audit_log ORDER BY created_at DESC LIMIT 20;"
```

As operações executadas devem aparecer com `resultado` `SUCESSO` ou `FALHA` e com `request_id`.

## 10. Segurança e observabilidade

Sem token, uma rota protegida deve responder `401`:

```bash
curl -i "$API_BASE_URL/api/recebiveis"
```

Para testar `403` no banco local, altere temporariamente a role, faça login novamente, use o token retornado e restaure `ADMIN`:

```bash
docker compose exec -T postgres psql -U postgres -d srm_credit -c "UPDATE usuario SET role = 'OPERADOR' WHERE username = 'admin';"
curl -i -X POST "$API_BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
docker compose exec -T postgres psql -U postgres -d srm_credit -c "UPDATE usuario SET role = 'ADMIN' WHERE username = 'admin';"
```

O token emitido enquanto a role era `OPERADOR` deve responder `403` em `/api/recebiveis`.

Para testar `429`, use mais escritas do que a capacidade configurada. O filtro deve retornar `429` e o header `Retry-After`:

```bash
for i in $(seq 1 110); do curl -sS -o /dev/null -w '%{http_code}\n' -X POST "$API_BASE_URL/api/simulacoes/precificacao" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{}'; done | sort | uniq -c
curl -i -X POST "$API_BASE_URL/api/simulacoes/precificacao" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{}'
```

Para testar `503`, reinicie temporariamente o backend com `BCB_PTAX_BASE_URL=http://127.0.0.1:9` e consulte um par sem taxa persistida. O corpo deve conter `message` e `resolution`.

Consulte os endpoints do Actuator com um token `ADMIN`:

```bash
curl -i "$API_BASE_URL/actuator/health" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -i "$API_BASE_URL/actuator/info" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -i "$API_BASE_URL/actuator/metrics" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -i "$API_BASE_URL/actuator/prometheus" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Os quatro resultados esperados são `200`; o Prometheus deve conter métricas `jvm_` e `http_server_requests_`. Sem token, `/actuator/prometheus` deve responder `401`.

Consulte OpenAPI e Swagger:

```bash
curl -i "$API_BASE_URL/v3/api-docs" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -i "$API_BASE_URL/swagger-ui.html" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Os resultados esperados são `200`. No perfil local, `SECURITY_EXPOSE_DOCS=true` também permite acesso sem token.

## 11. Logout

Revogue o refresh token ao final:

```bash
curl -i -X POST "$API_BASE_URL/api/auth/logout" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

O resultado esperado é `204`. Reutilizar o refresh token deve responder `401`.

## 12. Gate de cobertura do frontend

Execute no diretório `frontend`:

```bash
pnpm typecheck
pnpm lint
pnpm test
pnpm test:coverage
pnpm build
```

`pnpm test:coverage` deve terminar com todos os testes verdes e os mínimos de 80% de linhas, 70% de funções, 65% de branches e 80% de statements.
