# Plano de Implementação — Scaffold Backend e Frontend

**Contexto**: Fechamento da documentação do Git Flow e início do scaffold do projeto (monorepo). Mudança de contexto detectada com alterações não commitadas (`.agents/AGENTS.md`, `.contents/`, `.github/`, `README.md`).

**Status**: AGUARDANDO APROVAÇÃO. Nenhum código será gerado até a aprovação deste plano.

---

## 1. Decisões a confirmar

| Decisão | Recomendação |
| --- | --- |
| Grupo/package Java | `com.srm.creditengine` (artefato `srm-credit-engine`) — ajustável |
| Geração das bases | Geradores oficiais (Spring Initializr + `pnpm create vite`) para versões compatíveis atuais |
| Lombok | Não — código explícito, sem annotation processor |
| PostgreSQL local | `docker-compose.yml` na raiz com `postgres:16-alpine` |
| Testes de integração | Testcontainers (PostgreSQL isolado), conforme `.agents/backend.md` |

---

## 2. Backend (`backend/`)

### 2.1 Geração da base
- `curl https://start.spring.io/starter.tgz` com `baseDir=backend`, Java 21, Maven, e dependências:
  `web, validation, data-jpa, flyway, postgresql, actuator, micrometer-prometheus`.
- Inclui Maven Wrapper (`./mvnw`), conforme exigência dos agentes (sem dependência de Maven global).

### 2.2 `pom.xml` — adições manuais
- `springdoc-openapi-starter-webmvc-ui` (Swagger/OpenAPI em `/swagger-ui.html`).
- `testcontainers` (junit-jupiter + postgresql), escopo `test`.
- Plugin JaCoCo com gate de 90% de cobertura (falha no `verify` se abaixo):

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <configuration>
    <excludes>
      <exclude>**/CreditEngineApplication.class</exclude>
      <exclude>**/infrastructure/config/**</exclude>
    </excludes>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>prepare-agent</goal>
      </goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>verify</phase>
      <goals>
        <goal>report</goal>
      </goals>
    </execution>
    <execution>
      <id>coverage-check</id>
      <phase>verify</phase>
      <goals>
        <goal>check</goal>
      </goals>
      <configuration>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.90</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### 2.3 Estrutura hexagonal (esqueleto)
```
backend/src/main/java/com/srm/creditengine/
├── CreditEngineApplication.java
├── application/
│   └── port/
│       ├── in/
│       └── out/
├── domain/
│   └── model/
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   └── web/
    │   │       └── HealthController.java
    │   └── out/
    │       └── persistence/
    └── config/
        └── OpenApiConfig.java
```

### 2.4 Contrato inicial da API (único da fase de scaffold)
- `GET /api/health` → `200` com corpo `{"status":"UP"}`.
- O restante dos contratos de negócio será definido quando `.specs/domain.md` for preenchido.

```java
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP");
    }

    public record HealthResponse(String status) {
    }
}
```

### 2.5 Configuração por ambiente (nunca hardcode secrets)
`backend/src/main/resources/application.yml`:

```yaml
server:
  port: ${PORT:8080}

spring:
  application:
    name: srm-credit-engine
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/srm_credit}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

`backend/src/main/resources/db/migration/` criado vazio (`.gitkeep`), migrações `V1__...` virão com o modelo de dados.

### 2.6 OpenAPI
```java
@Configuration
@OpenAPIDefinition(info = @Info(title = "SRM Credit Engine API", version = "1.0.0"))
public class OpenApiConfig {
}
```

### 2.7 Testes base (garantem o gate do JaCoCo e o `./mvnw verify`)
- `HealthIntegrationTest` — `@SpringBootTest(RANDOM_PORT)` + Testcontainers PostgreSQL, valida `GET /api/health`.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class HealthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void healthReturnsUp() {
        ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity("/api/health", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }
}
```

---

## 3. Frontend (`frontend/`)

### 3.1 Geração da base
- `pnpm create vite frontend --template react-ts` (TypeScript strict).
- Dependências (versões resolvidas pelo `pnpm`, com `pnpm-lock.yaml` versionado):

| Tipo | Pacotes |
| --- | --- |
| runtime | `react-router-dom`, `@tanstack/react-query`, `zustand`, `react-hook-form`, `zod`, `@hookform/resolvers`, `@sentry/react`, `@sentry/vite-plugin` |
| dev | `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`, `msw`, `@playwright/test` |

### 3.2 Estrutura por features (conforme `.agents/frontend.md`)
```
frontend/src/
├── app/            # main.tsx, providers, roteamento
├── components/     # componentes visuais reutilizáveis
├── features/
│   └── health/     # api.ts, hooks (uso de TanStack Query)
├── pages/
│   └── HomePage.tsx
├── lib/
│   └── api/http.ts # cliente HTTP tipado (fetch)
├── state/          # stores Zustand (somente quando necessário)
├── styles/         # tokens, global.css
└── test/           # setup.ts, server.ts (MSW)
```

### 3.3 `package.json` — scripts
```json
"scripts": {
  "dev": "vite",
  "build": "tsc -b && vite build",
  "preview": "vite preview",
  "lint": "eslint .",
  "typecheck": "tsc -b --noEmit",
  "test": "vitest run",
  "test:e2e": "playwright test"
}
```

### 3.4 Cliente HTTP tipado (`lib/api/http.ts`)
Baseado em `VITE_API_BASE_URL`; validação de contrato no cliente é somente de experiência — o backend é a fonte autoritativa.

### 3.5 Configuração exposta ao navegador (nunca secrets)
`frontend/.env.example`:
```
VITE_API_BASE_URL=http://localhost:8080
VITE_SENTRY_DSN=
```

### 3.6 Testes base
- `src/test/setup.ts` (jest-dom + MSW), `src/test/server.ts` (handlers MSW).
- `HomePage.test.tsx` — renderiza a Home com MSW mockando `GET /api/health`.
- `vitest` configurado no `vite.config.ts` (jsdom, setupFiles, globals).

---

## 4. Infraestrutura e CI/CD

### 4.1 `docker-compose.yml` (raiz)
```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: srm_credit
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

### 4.2 `.gitignore` — adições
```
.env
.env.*
!.env.example
frontend/playwright-report/
frontend/test-results/
```

### 4.3 Workflows (`ci.yml`, `release.yml`, `create-release.yml`)
- `ci.yml`: job **`build`** (mantém o nome exigido pelo ruleset do GitHub) passa a rodar com `working-directory: backend` e `./mvnw verify`; novo job **`frontend`** com `pnpm install --frozen-lockfile`, `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build`.
- `release.yml`: passo `Build` com `working-directory: backend`; job de validação do frontend antes da tag.
- `create-release.yml`: sem mudança de código (só cria branch).

**Atenção**: o ruleset de branch protection exige o status check `build`. Com o novo job `frontend`, recomendo adicionar `frontend` aos status checks exigidos em `main` e `develop` para que um frontend quebrado bloqueie o merge.

---

## 5. Verificação

- Backend: `cd backend && ./mvnw verify` (build, testes, JaCoCo ≥ 90%).
- Frontend: `cd frontend && pnpm lint && pnpm typecheck && pnpm test && pnpm build`.
- Local: `docker compose up -d` e `cd backend && ./mvnw spring-boot:run`; `GET /api/health` retorna `{"status":"UP"}`.

---

## 6. Rollout

1. Fechar o contexto atual (documentação) com um commit em inglês, exemplo:
   `chore: add git flow workflows, readme and diagrams`
2. Após sua aprovação deste plano, gerar o scaffold (backend + frontend) na branch atual.
3. Novo commit em inglês ao final do scaffold, exemplo:
   `feat: scaffold backend and frontend apps`