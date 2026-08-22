---
description: React, TypeScript, UI, estado, consumo de API. Use para frontend.
mode: subagent
---

# Frontend architecture and operation methodology

## Stack

- Framework principal: React com TypeScript em modo strict.
- Build e desenvolvimento: Vite.
- Roteamento: React Router.
- Estado de servidor: TanStack Query.
- Estado global de cliente: Zustand somente quando necessário.
- Formulários: React Hook Form com Zod.
- Contratos de API: tipos e cliente gerados a partir do OpenAPI do backend.
- Estilos: CSS Modules, CSS variables e tokens de design reutilizáveis.
- Testes: Vitest, React Testing Library, MSW e Playwright.
- Acessibilidade: validação automatizada com axe quando aplicável e testes de teclado.
- Gerenciamento de pacotes: pnpm com `pnpm-lock.yaml`.
- Observabilidade: Sentry para erros de runtime e Web Vitals; Prometheus e Grafana permanecem destinados à observabilidade do backend.
- Vue, Angular e Svelte não devem ser usados simultaneamente com React. São alternativas ao framework principal e não se complementam neste projeto.

## Prioridade de regras

1. Priorize requisitos explícitos do usuário, correção, segurança e acessibilidade.
2. Preserve as convenções existentes do projeto e os contratos definidos pelo backend.
3. Priorize simplicidade, testabilidade e desempenho medido antes de adicionar abstrações.

## Implementações

- Se a alteração solicitada for complexa, a sugestão de implementação deve ser exibida em detalhes no arquivo .docs/implementation_plan.md, que deve ser sempre atualizado em sua totalidade com a nova solicitação. Se esse for o caso, não atue e aguarde o usuário aprovar o implementation_plan.md
  - Considere complexa uma alteração que envolva múltiplos módulos, contratos de API, autenticação, navegação principal, gerenciamento de estado global, design system ou risco relevante de regressão.
- Não bloqueie tarefas simples nem crie arquivos de planejamento sem necessidade.
- Aplique os seguintes princípios de forma pragmática, sem criar complexidade ou abstrações desnecessárias:
  - Codebase & Sincronização:
    - NUNCA faça commit ou push sem solicitação explícita.
    - Antes de propor ou gerar qualquer nova alteração de código, você DEVE executar o seguinte protocolo:
            1. Monitoramento Semântico de Escopo:
                - Rastreie ativamente o objetivo da janela atual de conversa. Se o usuário introduzir um novo requisito arquitetural, mudar de domínio, ou pedir para iniciar uma funcionalidade não relacionada, classifique isso como uma Mudança de Contexto.
            2. Validação de Estado:
                - Antes de aceitar a mudança de contexto, verifique o estado do repositório com `git status` e `git diff`.
            3. Intervenção de Sincronização (Obrigatório):
                - Ao detectar uma Mudança de Contexto, PARE e não gere código para a nova solicitação. Em vez disso, crie/edite um arquivo .docs/implementation_plan.md com as alterações sugeridas para a solicitação e crie aviso claro e direto, estruturado da seguinte forma, após resumir no chat a proposta da solicitação:
                "Notei uma mudança de contexto de [Tópico Anterior] para [Novo Tópico]. A implementação de [Tópico Anterior] foi concluída?"
                - Assuma que o usuário realmente pode ter mudado de contexto, e sugira a criação de um novo commit: "Baseado no diff atual, sugiro o seguinte commit: git commit -m \"(feat|fix|refactor|docs|test|chore|etc...): ...\"" usando o padrão Conventional Commits para facilitar o fechamento do pacote.
            4. O usuário pode negar a mudança de contexto ou aceitá-la. Se ele recusar a alteração de contexto, assuma o tópico anterior como parte do contexto atual e siga com a solicitação. Se o usuário aceitar a solicitação, aguarde uma solicitação explícita para agir.
            5. Resolução e Facilitação:
                - Se houver código órfão (não commitado) da tarefa anterior, resuma o que foi feito na última janela de contexto e gere 2 sugestões de git commit -m "(feat|fix|refactor|docs|test|chore|etc...): ..." usando o padrão Conventional Commits para facilitar o fechamento do pacote. Só prossiga com a nova implementação após o usuário solicitar.
            6. Só emitir o aviso quando a solicitação for claramente independente da tarefa atual e existirem alterações não commitadas.
- Mantenha dependências e versões declaradas no `package.json`, preserve o `pnpm-lock.yaml` e não dependa de pacotes instalados globalmente.
- Use TypeScript strict e não introduza `any` sem justificativa técnica explícita.
- Leia configurações públicas por variáveis de ambiente ou configuração externa. Variáveis expostas ao navegador nunca podem conter secrets.
- O backend é a fonte autoritativa para regras de negócio, permissões, cálculos, persistência e consistência de dados.
- Valide dados no cliente para fornecer feedback imediato, mas nunca substitua a validação e autorização do backend.
- Não coloque chamadas HTTP diretamente em componentes visuais. Use adapters ou hooks de aplicação baseados no cliente tipado da API.
- Não introduza uma biblioteca de estado, UI ou formulário quando a stack existente já resolver o problema.
- Evite refatorações, abstrações e alterações visuais fora do escopo da tarefa.
- Não use `useMemo`, `useCallback` ou memoização indiscriminadamente. Meça o problema e use otimizações apropriadas, como `startTransition`, `useDeferredValue` ou carregamento tardio quando necessário.
- Quando houver mudança relevante na arquitetura, API, fluxo de navegação, estado global, design system ou configuração, atualize o arquivo .docs/TechDoc.md de forma técnica e coesa.

## Frontend burro

- Componentes de UI devem apresentar dados, receber props e emitir eventos; não devem conter regras de negócio, chamadas HTTP ou acesso direto ao estado global.
- Regras de negócio devem permanecer no backend. O frontend pode conter apenas lógica de apresentação, interação, formatação, validação de experiência e composição de estado.
- Não replique no frontend cálculos, permissões ou invariantes cuja fonte de verdade seja o backend.
- Páginas, componentes de rota e hooks de aplicação devem orquestrar carregamento, mutações, navegação e estado; componentes visuais devem permanecer reutilizáveis e testáveis.
- Separe estado de servidor, estado global de cliente, estado de URL, estado de formulário e estado local de componente.
- Prefira composição de componentes e props explícitas a heranças, prop drilling excessivo ou contextos globais indiscriminados.

## Arquitetura

- Organize o código por features e responsabilidades, evitando um diretório global com componentes, hooks ou serviços sem contexto.
- Separe componentes visuais reutilizáveis, páginas, hooks de aplicação, adapters de API, estado e estilos.
- Uma estrutura preferencial é:

  ```text
  src/
  ├── app/            # bootstrap, providers, roteamento e configuração
  ├── components/     # componentes visuais reutilizáveis
  ├── features/       # funcionalidades organizadas por domínio de interface
  ├── pages/          # composição de telas e rotas
  ├── lib/            # cliente HTTP, utilidades e integrações
  ├── state/          # estado global de cliente, somente quando necessário
  └── styles/         # tokens, temas e estilos globais
  ```

- Componentes de apresentação não devem importar TanStack Query, Zustand ou cliente HTTP diretamente.
- Hooks e módulos de aplicação devem encapsular acesso a dados, mutações e efeitos necessários à feature.
- Adapters devem traduzir contratos externos para modelos consumíveis pela aplicação sem espalhar detalhes HTTP pela UI.
- O estado derivável deve ser calculado, não armazenado duplicadamente.
- Evite componentes monolíticos; extraia apenas quando houver responsabilidade, reutilização ou testabilidade claras.

## Gerenciamento de Estado

- Use estado local por padrão para interações restritas a um componente ou tela.
- Use TanStack Query para cache, carregamento, invalidação, sincronização e mutações de dados vindos do backend.
- Não copie dados do TanStack Query para Zustand ou outro estado global sem necessidade concreta.
- Use Zustand apenas para estado de cliente compartilhado entre partes não relacionadas da árvore, como preferências de interface ou estado de sessão visual.
- Use o estado da URL para filtros, paginação, ordenação, busca e qualquer estado que precise ser compartilhável ou persistir na navegação.
- Mantenha estado de formulário próximo ao formulário e valide-o com schemas Zod quando houver regras de entrada.
- Defina chaves de query estáveis e invalide somente os recursos afetados por uma mutação.
- Use atualizações otimistas apenas quando houver rollback seguro e benefício perceptível para a experiência.

## UI e UX

- Use HTML semântico, hierarquia visual clara, labels associados aos campos e navegação completa por teclado.
- Garanta foco visível, contraste adequado, mensagens acessíveis, suporte a leitores de tela e respeito a `prefers-reduced-motion`.
- A interface deve funcionar em telas pequenas e grandes, com comportamento responsivo definido para os principais estados.
- Toda operação assíncrona deve possuir estados de carregamento, sucesso, vazio e erro adequados ao contexto.
- Mensagens de erro devem explicar o problema e, quando possível, indicar como corrigi-lo; não use mensagens genéricas quando houver informação útil.
- Ações destrutivas devem exigir confirmação proporcional ao risco e nunca depender apenas de cor ou posição visual.
- Preserve entradas do usuário durante erros de validação ou falhas recuperáveis.
- Use tokens consistentes para cores, tipografia, espaçamento, bordas, sombras e estados interativos.
- Não introduza componentes visualmente diferentes para resolver o mesmo padrão sem justificativa de UX.
- Não use texto, ícones ou cores como único meio de transmitir informação.
- Não implemente dark patterns, feedback enganoso ou bloqueios de navegação sem justificativa de negócio.

## Segurança

- Nunca trate guards de rota ou estado de autenticação no frontend como autorização; o backend deve validar toda operação protegida.
- Não armazene tokens sensíveis em `localStorage` ou `sessionStorage`; use a estratégia de autenticação definida pelo backend, preferencialmente cookies `HttpOnly`, `Secure` e `SameSite` quando aplicáveis.
- Nunca inclua secrets, chaves privadas ou credenciais em código, variáveis expostas ao bundle ou mensagens de erro.
- Evite HTML arbitrário e `dangerouslySetInnerHTML`; quando for indispensável, sanitize o conteúdo com uma estratégia aprovada.
- Não registre tokens, dados pessoais ou payloads sensíveis em logs, analytics ou ferramentas de observabilidade.
- Configure tratamento de CORS, CSRF e Content Security Policy em conjunto com o backend quando aplicável.
- Dependências devem ser atualizadas e verificadas contra vulnerabilidades conhecidas.

## 12-Factor Frontend

- Declare dependências no `package.json` e mantenha o `pnpm-lock.yaml` versionado.
- Separe build, release e execução. O build deve ser reproduzível e os artefatos devem ser imutáveis após a publicação.
- Configure a URL da API e demais configurações públicas externamente; nunca trate configuração exposta ao navegador como secret.
- Consuma backend, storage, analytics e outros serviços por URLs ou adapters configuráveis, sem hardcode de ambientes.
- Não use o navegador como fonte autoritativa de persistência para dados de negócio, permissões ou sessões.
- Prefira processos stateless; persistência local deve ser limitada a preferências ou dados explicitamente autorizados pelo produto.
- Cancele requisições e efeitos obsoletos durante mudanças de rota, desmontagem de componentes ou novas buscas.
- Erros de runtime e métricas relevantes devem ser enviados ao Sentry, sem dados sensíveis.
- Mantenha paridade entre desenvolvimento, testes e produção quanto a versões, contratos de API e configurações públicas.

## Tratamento de Erros

- Normalize erros da API em um formato consumível pela interface, preservando códigos HTTP e mensagens apropriadas.
- Trate erros esperados com feedback contextual e erros inesperados com uma tela ou estado de recuperação apropriado.
- Use Error Boundaries para falhas de renderização e ofereça recuperação sem mascarar o erro.
- Faça retry automático somente para operações idempotentes e falhas transitórias, com limite e backoff.
- Não ignore rejeições de Promise, erros de mutations ou falhas de carregamento.
- Evite exibir detalhes técnicos, stack traces ou informações internas ao usuário.

## Testes

- Defina a estratégia de testes conforme o tipo, risco e impacto da alteração.
- Cubra componentes com testes orientados ao comportamento do usuário usando React Testing Library.
- Cubra hooks, adapters e regras de apresentação com testes unitários quando possuírem comportamento relevante.
- Use MSW para simular contratos de API nos testes de integração sem acoplar a UI a uma implementação HTTP específica.
- Cubra carregamento, sucesso, estado vazio, erro, validação, autorização e mutações quando aplicável.
- Use Playwright para fluxos críticos de ponta a ponta, como autenticação, navegação principal e operações de negócio.
- Teste acessibilidade, navegação por teclado e estados responsivos para componentes e fluxos relevantes.
- Para alterações críticas de desempenho, defina carga, metas de Web Vitals ou latência antes de implementar e valide-as com ferramentas apropriadas.
- Garanta que os testes sejam determinísticos, independentes e não dependam da ordem de execução ou de serviços externos não controlados.
- Evite snapshots extensos como substitutos de testes comportamentais.
- Execute `pnpm lint`, `pnpm typecheck`, `pnpm test` e `pnpm build` antes de concluir; execute `pnpm test:e2e` quando houver alteração em fluxo crítico.
- Não considere uma alteração concluída enquanto testes relevantes introduzidos ou modificados estiverem falhando. Falhas preexistentes e fora do escopo devem ser reportadas com evidências, sem serem mascaradas.

## Critérios de Aceite

- Antes de declarar a tarefa concluída, verifique cada critério aplicável abaixo. Se algum critério falhar, corrija a implementação e repita a verificação sem ampliar o escopo sem autorização. Não entregue a tarefa apenas reportando falhas conhecidas; informe somente limitações que não puderem ser resolvidas no escopo atual.
- Separação: componentes de UI não contêm regras de negócio, chamadas HTTP ou acesso direto ao estado global; a lógica de aplicação e os adapters possuem responsabilidades claras.
- Usabilidade: fluxos possuem estados de carregamento, sucesso, vazio e erro; mensagens são claras; formulários preservam entradas inválidas e a navegação é previsível.
- Acessibilidade: a interface possui semântica adequada, foco visível, navegação por teclado, labels, contraste e suporte aos principais estados assistivos aplicáveis.
- Responsividade: as telas e componentes alterados funcionam nos breakpoints relevantes e não dependem de uma largura ou dispositivo específico.
- Estado: dados do backend são gerenciados por TanStack Query; estado global só existe quando necessário; filtros, paginação e busca usam a URL quando aplicável.
- Segurança: o frontend não contém secrets, não expõe dados sensíveis, não substitui autorização do backend e evita injeção de HTML e armazenamento inseguro de tokens.
- Desempenho: não existem requisições duplicadas ou waterfalls evitáveis; listas extensas possuem paginação ou virtualização quando necessário; assets e rotas são carregados sob demanda quando aplicável.
- Contrato: os tipos e adapters da API correspondem ao OpenAPI atual; erros e códigos HTTP são tratados sem depender de detalhes internos do backend.
- Observabilidade: erros de runtime e métricas relevantes podem ser coletados pela infraestrutura sem expor dados sensíveis.
- Testes: testes relevantes foram criados ou atualizados; `pnpm lint`, `pnpm typecheck`, `pnpm test` e `pnpm build` foram executados com sucesso; fluxos críticos possuem cobertura E2E quando aplicável.
- Nenhuma dependência, estado global, abstração, componente visual ou ferramenta nova foi adicionada sem justificativa técnica e necessidade concreta.
