# Architecture and operation methodology

## Stack

- Linguagem: Java 21
- Framework: Spring Boot 3
- Banco de dados: PostgreSQL
- Testes: JUnit, Mockito
- Build: Maven
- Containerização: Docker
- CI/CD: GitHub Actions
- Arquitetura hexagonal
- Documentação: Swagger e OpenAPI
- Observabilidade: Prometheus e Grafana
- Migração: Flyway
- Integrações com Feign Client

## Prioridade de regras

1. Priorize requisitos explícitos do usuário, correção e segurança.
2. Preserve as convenções existentes do projeto.

## Implementações

- Se a alteração solicitada for complexa, a sugestão de implementação deve ser exibida em detalhes no arquivo .docs/implementation_plan.md, que deve ser sempre atualizado em sua totalidade com a nova solicitação. Se esse for o caso, não atue e aguarde o usuário aprovar o implementation_plan.md
  - Considere complexa uma alteração que envolva múltiplos módulos, contratos de API, modelo de dados, autenticação, integrações externas, migrações ou risco relevante de regressão.
- Não bloqueie tarefas simples nem crie arquivos de planejamento sem necessidade.
- Aplique os seguintes princípios de forma pragmática, sem criar complexidade
ou abstrações desnecessárias:
  - Codebase & Sincronização:
    - NUNCA faça commit ou push sem solicitação explícita.
    - Antes de propor ou gerar qualquer nova alteração de código, você DEVE executar o seguinte protocolo:
            1. Monitoramento Semântico de Escopo:
                - Rastreie ativamente o objetivo da janela atual de conversa (ex: "Construindo o endpoint de login"). Se o usuário introduzir um novo requisito arquitetural, mudar de domínio, ou pedir para iniciar uma funcionalidade não relacionada, você deve classificar isso como uma Mudança de Contexto.
            2. Validação de Estado:
                - Antes de aceitar a mudança de contexto, verifique o estado do repositório com a verificação do estado local com `git status` e `git diff`.
            3. Intervenção de Sincronização (Obrigatório):
                - Ao detectar uma Mudança de Contexto, PARE e não gere código para a nova solicitação. Em vez disso, crie/edite um arquivo .docs/implementation_plan.md com as alterações sugeridas para a solicitação e crie aviso claro e direto, estruturado da seguinte forma, após resumir no chat a proposta da solicitação:
                "Notei uma mudança de contexto de [Tópico Anterior] para [Novo Tópico]. A implementação de [Tópico Anterior] foi concluída?"
                - Assuma que o usuário realmente pode ter mudado de contexto, e sugira a criação de um novo commit: "Baseado no diff atual, sugiro o seguinte commit: git commit -m "(feat|fix|refactor|docs|test|chore|etc...): ..." usando o padrão Conventional Commits para facilitar o fechamento do pacote.
            4. O usuário pode negar a mudança de contexto ou aceitá-la. Se ele recusar a alteração de contexto, assuma o tópico anterior como parte do contexto atual e siga com a solicitação. Se o usuário aceitar a solicitação, ele simplesmente commitará e solicitará que você siga com a nova implementação. Aguarde essa solicitação explicita antes de agir.
            5. Resolução e Facilitação:
                - Se houver código órfão (não commitado) da tarefa anterior, resuma o que foi feito na última janela de contexto e gere 2 sugestões de git commit -m "(feat|fix|refactor|docs|test|chore|etc...): ..." usando o padrão Conventional Commits para facilitar o fechamento do pacote. Só prossiga com a nova implementação após o usuário solicitar.
            6. Só emitir o aviso quando a solicitação for claramente independente da tarefa atual e existirem alterações não commitadas.
- Declare as dependências e versões no `pom.xml`, mantenha o Maven Wrapper atualizado e não dependa de ferramentas ou pacotes instalados globalmente.
- Leia configurações e credenciais por variáveis de ambiente ou por um mecanismo externo de configuração. Nunca inclua secrets no código ou no Git.
- Valide as variáveis obrigatórias no início da aplicação e falhe rapidamente com mensagens claras quando estiverem ausentes ou inválidas.
- Isole integrações externas por adapters e configuração apropriada. Não crie abstrações para fornecedores hipotéticos sem necessidade concreta.
- Separe build, release e execução. Não altere o código ou a configuração durante a execução de uma release.
- Mantenha os processos stateless. Não use memória local ou disco local como fonte permanente de sessões, uploads, jobs ou dados de negócio.
- Faça o servidor escutar em uma porta configurável e seja executável sem depender de um servidor de aplicação instalado manualmente.
- Separe processamento HTTP de tarefas assíncronas ou demoradas. Use workers ou filas quando apropriado.
- Garanta inicialização rápida e encerramento gracioso. Trate SIGTERM, finalize conexões e não aceite novas requisições durante o shutdown.
- Mantenha paridade entre desenvolvimento, testes e produção: versões, comandos e serviços devem ser o mais semelhantes possível.
- Escreva logs estruturados em stdout/stderr. Não grave logs em arquivos locais e nunca exponha senhas, tokens ou dados sensíveis.
- Execute migrações, seeds e tarefas administrativas por comandos explícitos, versionados, reproduzíveis e seguros para execução controlada.
- Prefira soluções simples e compatíveis com a infraestrutura existente.
- Não introduza microsserviços, filas, cache ou abstrações apenas para cumprir o 12-Factor.
- Use adapters quando houver necessidade real de trocar uma integração externa.
- Mantenha a lógica de negócio independente de framework, banco ou provedor de infraestrutura sempre que isso não aumentar complexidade injustificada.
- Priorize código legível, coeso e simples. Evite refatorações ou abstrações fora do escopo da tarefa.
- Quando houver mudança relevante na arquitetura, API, modelo de dados, configuração ou operação, (crie se não existir) atualize o arquivo /docs/TechDoc.md com as alterações realizadas, de forma técnica e coesa.
- Idioma de código: identificadores, mensagens de erro, mensagens de validação e logs sempre em inglês (exceto palavras reservadas de domínio em português). Prosa de documentação e comunicação com o usuário permanecem em PT-BR.

## Arquitetura Hexagonal

- Separe as responsabilidades em camadas de aplicação, negócio e persistência.
- Controllers devem atuar como adapters de entrada; casos de uso pertencem à camada de aplicação; persistência é acessada por portas e adapters de saída.
- Relatórios simples podem omitir a camada de negócio, mas devem passar pela camada de aplicação para preservar autorização, contratos e controle da consulta.

## Tratamento de Exceções

- Trate erros esperados com respostas HTTP apropriadas e erros inesperados com logging contextual, sem expor detalhes internos. Nunca ignore exceções nem mantenha o fluxo após uma falha que comprometa a consistência da operação

## Testes

- Defina a estratégia de testes conforme o tipo e o risco da alteração.
- Cubra regras de negócio e casos de erro com testes unitários em JUnit.
- Use Mockito apenas para isolar dependências externas ao componente testado.
- Cubra controllers, validações, códigos HTTP, contratos de resposta e regras de autorização com testes de API.
- Cubra repositories, transações, migrações e consultas relevantes com testes de integração usando PostgreSQL isolado, preferencialmente via Testcontainers, quando a alteração envolver persistência ou SQL.
- Teste respostas para entradas inválidas, recursos inexistentes, acesso não autorizado, conflitos e falhas de dependências externas quando aplicável.
- Teste timeout, retry, idempotência e encerramento gracioso quando a alteração envolver essas responsabilidades.
- Garanta que os testes sejam determinísticos, independentes e não dependam da ordem de execução ou de serviços externos não controlados.
- Para alterações críticas de desempenho, defina carga e meta de latência antes de implementar e execute testes de carga apropriados.
- Não considere a alteração concluída enquanto testes relevantes introduzidos ou modificados estiverem falhando. Falhas preexistentes e fora do escopo devem ser reportadas com evidências, sem serem mascaradas.
- Verifique que os testes relevantes foram criados ou atualizados e que `./mvnw verify` foi executado com sucesso. Se o comando não puder ser executado, informe o motivo, as verificações realizadas e as lacunas restantes.

## Critérios de Aceite

- Antes de declarar a tarefa concluída, verifique cada critério aplicável abaixo. Se algum critério falhar, corrija a implementação e repita a verificação sem ampliar o escopo sem autorização. Não entregue a tarefa apenas reportando falhas conhecidas; informe somente limitações que não puderem ser resolvidas no escopo atual.
- Usabilidade: endpoints possuem contratos OpenAPI atualizados para todos os endpoints, códigos HTTP consistentes e formato padronizado de erro; entradas inválidas retornam mensagens associadas aos campos incorretos.
- Segurança: autenticação e autorização são aplicadas conforme o recurso; entradas são validadas; consultas são parametrizadas; secrets e dados sensíveis não aparecem no código, nas respostas ou nos logs; erros não expõem detalhes internos.
- Desempenho: consultas não apresentam N+1 nem retornam coleções sem limite; índices, paginação e limites de payload são usados quando aplicáveis; chamadas externas possuem timeout; alterações críticas possuem metas de carga e latência definidas e validadas por testes.
- Escalabilidade: a aplicação permanece stateless e pode ser executada em múltiplas instâncias; quando existirem estado persistente, arquivos ou tarefas assíncronas, eles usam serviços apropriados; há health checks, encerramento gracioso e ausência de dependência de armazenamento local.
- Observabilidade: logs estruturados possuem contexto suficiente para troubleshooting; métricas de erro, latência e volume estão disponíveis para as tecnologias de observabilidade da stack no início deste documento; tracing distribuído é usado quando houver comunicação entre serviços.
- dependências e versões estão declaradas no `pom.xml` e que o Maven Wrapper está disponível e atualizado.
- O encerramento é gracioso e possui tratamento de SIGTERM, bem como o fechamento das conexões durante o shutdown é garantido.
- Novas dependências, serviços, abstrações ou componentes possuem justificativa técnica e são necessários para atender aos requisitos.
- Idempotência garantida para operações sujeitas a retry, reenvio ou processamento duplicado.
