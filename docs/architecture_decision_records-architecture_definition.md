# Da definição de Topologia de Aplicação para o SRM Credit Engine

## 1. Contextualização técnica

O SRM Credit Engine opera sobre a cessão de direitos creditórios, com precificação de ativos, liquidação multimoedas e consultas analíticas de alto volume. A decisão estrutural trespassa a escolha de framework, banco ou ferramenta, se embasando na escolha entre **microserviços** ou **monolito modular**, definindo a topologia de _deploy_, a estratégia de governança de dados, o custo operacional e a complexidade do fluxo de entrega.

Decisão essa da qual se é impedida de tomar por aderência a modismos ou pela leitura apressada de que "sistemas modernos são microserviços"; portanto foi levado em consideração a natureza do domínio de negócios, o estágio de maturidade do produto e as restrições reais do ambiente de desenvolvimento e entrega.

A análise parte de dois eixos complementares: (i) o exame das topologias candidatas (_microserviços_, _monolito modular_ e _monolito único sem fronteiras_) avaliadas contra os critérios que determinam a corretude e a entrega do sistema; e (ii) a decisão de versionamento, que situa o código em um **repositório único** com componentes independentes. A conclusão dá-se pela adoção do monolito modular como topologia de aplicação, em um repositório único — emergindo de cada um desses eixos, não de uma única justificativa isolada.

## 2. Fundamentação Teórica

### 2.1 Topologias Candidatas

A decomposição de um sistema pode assumir formas distintas, que variam no grau de independência de processo, de dados e de ciclo de vida. Três configurações foram consideradas.

A primeira é a de **microserviços**: a decomposição do sistema em serviços independentes, cada um com seu próprio ciclo de vida, _deploy_, armazenamento de dados e equipe de manutenção, comunicando-se por protocolos de rede (tipicamente `HTTP`/`REST` ou mensageria assíncrona) e correspondendo, em geral, a um _bounded context_.

A segunda é a do **monolito modular**: uma única aplicação _deployable_, internamente organizada em módulos coesos que respeitam as fronteiras de domínio (_bounded contexts_). Os módulos comunicam-se por contratos explícitos (interfaces de aplicação) dentro do mesmo processo, sem o _overhead_ de rede, mas com a mesma disciplina de desacoplamento que se esperaria de serviços independentes.

A terceira é a do **monolito único sem fronteiras**: uma aplicação única onde os conceitos de domínio misturam-se livremente, sem definição de módulos ou contratos. Foi descartada de imediato, pois não atende ao requisito fundamental de desacoplamento e coesão, independentemente da decisão entre as outras duas configurações.

### 2.2 Critérios de Avaliação

As topologias candidatas foram avaliadas sob critérios ponderados pela natureza do projeto. A consistência transacional figura em primeiro plano, pois operações de natureza financeira não toleram consistência eventual no núcleo de escrita. A complexidade de entrega e o custo operacional vêm em seguida: o projeto é avaliado integralmente, e a entrega precisa ser demonstrável e verificável, o que **não se compadece com a operação de múltiplos serviços em estágio inicial**. Coesão e desacoplamento são tratados como requisito não negociável, independentemente da topologia. Por fim, evolução futura e observabilidade completam o quadro: a arquitetura não deve impedir a migração para serviços distribuídos se a escala a justificar, e a capacidade de observação é necessária em ambos os cenários, ainda que mais complexa quando distribuída.

## 3. Análise Aplicada à Natureza do Sistema

### 3.1 Microserviços: vantagens e custos

As vantagens dos microserviços são reais e bem documentadas: escalabilidade independente por domínio, equipes autônomas, isolamento de falhas e liberdade tecnológica por serviço. Todas essas vantagens, contudo, pressupõem um contexto de operação maduro: equipes múltiplas, plataforma de orquestração, infraestrutura de observabilidade distribuída, estratégia de versionamento de contratos e governança de dados descentralizada.

Os custos, no estágio atual do sistema, são desproporcionais ao benefício. Em primeiro lugar, a **consistência transacional**: operações de liquidação exigem propriedades `ACID` sobre múltiplas entidades; em um cenário distribuído, essa garantia exigiria _sagas_ e transações compensatórias, introduzindo complexidade e risco justamente onde o domínio não tolera falha silenciosa. Em segundo lugar, o **custo operacional**: cada serviço implica _pipeline_, monitoramento, gestão de segredos e estratégia de dados próprios, multiplicando a superfície de manutenção antes de o produto validar sua regra de negócio central. Em terceiro lugar, o **custo de entrega**: a comunicação entre serviços adiciona latência, versionamento de contratos e testes de integração distribuída — sobrecarga que drena esforço do que realmente diferencia o produto: a corretude da precificação.

### 3.2 Monolito modular: vantagens e custos

O monolito modular concentra toda a complexidade em um único _deployable_, eliminando o custo de comunicação inter-processos e simplificando drasticamente a operação e a entrega. A consistência transacional é garantida de forma nativa pelo banco de dados, com transações `ACID` sobre múltiplos módulos, sem a necessidade de _sagas_. Os testes de integração exercitam o sistema como um todo, refletindo com fidelidade o comportamento real em produção.

O risco apontado para o monolito — o acoplamento inadvertido entre módulos — é endereçado por disciplina arquitetural, e não por topologia. Ao organizar a aplicação em _bounded contexts_ com contratos explícitos e dependências direcionadas para o núcleo de domínio, o monolito preserva o desacoplamento que se exigiria de serviços, sem pagar o custo da distribuição. O custo reconhecido é a impossibilidade de escalar e realizar _deploy_ de módulos individualmente — limitação aceitável enquanto o gargalo de escala for a própria aplicação como um todo.

### 3.3 O ponto crítico: a natureza do projeto

O sistema não é um legado em migração, nem uma plataforma com equipes múltiplas e tráfego validado. É um produto em construção, no qual o valor está na corretude das regras de negócio e na demonstração de maturidade de engenharia. Nesse contexto, o risco dominante não é a escala ou a autonomia de equipes, e sim a **complexidade prematura**: microserviços introduzem complexidade distribuída antes que exista volume de tráfego ou equipes que a justifiquem, trocando um problema gerenciável — organizar código em módulos — por vários problemas novos — orquestração, consistência distribuída, contratos versionados — sem ganho mensurável nesta fase.

### 3.4 A questão do repositório único

A decisão de topologia aplicacional é complementada pela decisão de topologia de versionamento: todo o código reside em um **repositório único**, com os aplicativos do sistema organizados como componentes independentes. Essa escolha é deliberada e alinhada à natureza do projeto:

- **Coesão do entregável**: o projeto é avaliado como um todo; o repositório único permite navegar pelo sistema de forma contínua, sem saltar entre repositórios e sem fragmentar a compreensão da solução.
- **Rastreabilidade do histórico**: o fluxo de versionamento (_branching_, _pull requests_, _tags_ semânticas) aplica-se uniformemente a todos os componentes, produzindo um histórico rastreável e coerente que evidencia o controle sobre o ciclo de vida do software.
- **Simplicidade de consistência**: alterações que atravessam a fronteira de módulos ou de aplicativos são introduzidas em um único fluxo de revisão, garantindo que o contrato entre eles evolua de forma atômica.
- **Baixo acoplamento preservado**: um único repositório não implica acoplamento; as fronteiras são mantidas por organização de módulos e contratos explícitos, e não pela separação física de repositórios. O repositório único é uma decisão de versionamento, independente e ortogonal à decisão de topologia de _deploy_.

## 4. Decisão

O sistema adota o **monolito modular** como topologia de aplicação, organizado em _bounded contexts_ com contratos explícitos, residente em um **repositório único** com componentes independentes.

A decisão é registrada como ADR (_Architecture Decision Record_) e assume caráter de referência para alterações futuras: qualquer proposta de decomposição em serviços distribuídos deve ser avaliada contra os critérios deste documento e deve demonstrar que os custos operacionais e de consistência foram considerados.

## 5. Consequências

### 5.1 Consequências positivas

- Consistência transacional nativa para operações que atravessam módulos;
- Entrega e operação simplificadas: um único _deploy_, um único _pipeline_, um único conjunto de observabilidade;
- Testes de integração fiéis ao comportamento real, exercitando o sistema completo;
- Desacoplamento garantido por disciplina de módulos, com portas de aplicação prontas para evoluir;
- Histórico de versionamento único e rastreável, alinhado à natureza avaliativa do projeto.

### 5.2 Consequências negativas

- Impossibilidade de escalar ou realizar _deploy_ de módulos individualmente;
- Impossibilidade de isolar falhas por módulo em nível de processo;
- Liberdade tecnológica limitada à _stack_ do monolito.

### 5.3 Mitigações

O monolito é **modular por contrato**: as fronteiras entre _bounded contexts_ são expressas por interfaces de aplicação, e as dependências apontam do adaptador para o domínio, nunca em sentido contrário. Essa disciplina é o que torna a evolução possível: se a escala exigir, um módulo pode ser extraído para um serviço independente — preservando suas portas como contratos de integração — sem reescrita do núcleo de negócio. O desacoplamento, portanto, não é postergado; é praticado desde o início, mesmo dentro de um único processo.

## 6. Conclusão

A adoção do monolito modular em repositório único não é uma abdicação da arquitetura distribuída, mas uma escolha calibrada ao estágio do produto e aos critérios de avaliação do projeto. Ela maximiza a consistência transacional e a simplicidade de entrega — os atributos que determinam a corretude e a demonstrabilidade da solução — enquanto preserva, pela disciplina de módulos e contratos, o desacoplamento e a coesão que sustentariam uma eventual evolução para serviços distribuídos. O custo da distribuição, quando a escala o justificar, será pago naquele momento, com a segurança de que as fronteiras de domínio já estarão demarcadas.
