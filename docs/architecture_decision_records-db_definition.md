# Da definição de Banco de Dados para o SRM Credit Engine

## 1. Contextualização técnica

A base de dados escolhida para o projeto é definida a partir de dois _bounded contexts_ inerentemente ligados mas conceitualmente distintos: o **núcleo transacional**, que depente em sua essência de ser fidedigno à fundação da integridade e forte consistência dos dados, bem como da representação aritmética, natural e fundamental de um sistema financeiro; e o **núcleo de dados analíticos**, que por sua vez tem por objetivo a representação estatística e fornecimento de parâmetros para tomada de decisão, cuja latência é um fator relevante.

A decisão então prospera a partir de uma decisão arquitetural de alto impacto, pois dela derivam propriedades não funcionais fundamentais do produto: integridade de dados, latência, disponibilidade, comportamento sob concorrência e capacidade de evolução em escala. Este documento apresenta o racional técnico que fundamenta a adoção do `PostgreSQL`, dentro do universo de sistemas gerenciadores de banco de dados relacionais (_SGBDR_), amparado em teorias consolidadas de sistemas distribuídos e em requisitos estruturais do domínio financeiro que o sistema atenderá.

A análise parte de três eixos complementares: (i) o teorema `CAP` e seu refinamento `PACELC`, que modelam os _trade-offs_ fundamentais de consistência e disponibilidade; (ii) o modelo `ACID` e os mecanismos de isolamento e concorrência, que definem a garantia de integridade transacional; e (iii) as capacidades operacionais e de escala que determinam a viabilidade da solução em produção. A conclusão dá-se pela adoção do `PostgreSQL` como fonte transacional única para os dois _bounded contexts_ — emergindo de cada um desses eixos, não de uma única justificativa isolada.

## 2. Fundamentação Teórica

### 2.1 O Teorema CAP (Consistency, Availability, Partition Tolerance)

O teorema `CAP`, formulado por Eric Brewer em 2000 e provado por Gilbert e Lynch em 2002, estabelece que, diante de uma Partição de rede (situação em que os nós de um sistema distribuído perdem conectividade entre si), é impossível garantir simultaneamente as três propriedades: Consistência (Consistency)(todos os nós observam o mesmo estado após uma escrita), Disponibilidade (Availability)(toda requisição recebe resposta) e Tolerância da partição (Partition Tolerance)(o sistema permanece operacional apesar do isolamento de nós).

Um equívoco recorrente na interpretação do teorema é tratar a tolerância da partição como uma escolha. Ela não o é: em qualquer sistema distribuído, partições de rede são eventos possíveis e devem ser assumidos no projeto. A decisão real, portanto, concentra-se em definir o comportamento do sistema no momento da partição — se ele preservará a consistência (sacrificando disponibilidade, classificação `CP`) ou a disponibilidade (sacrificando a consistência imediata, classificação `AP`).

### 2.2 O Teorema PACELC (Partition Tolerance, Availability, Latency, Consistency)

O teorema `CAP`, embora fundamental, possui uma lacuna importante: ele descreve apenas o comportamento do sistema durante uma partição de rede — um evento excepcional. No regime operacional normal, sem partição, existe um segundo _trade-off_ que o `CAP` não contempla. O teorema `PACELC`, proposto por Daniel Abadi em 2012, estende o modelo e o formaliza como:

> Se houver Partição (`P`), escolha entre Availability (`A`) e Consistency (`C`). Caso contrário (Else)(`E`), escolha entre Latency (`L`) e Consistency (`C`).

O `PACELC` decompõe, portanto, a decisão em dois eixos independentes. No primeiro, definimos o comportamento sob partição: disponibilidade imediata a qualquer custo (`PA`) ou recusa de resposta em favor da consistência (`PC`). No segundo, definimos o comportamento no estado normal: otimização de latência com relaxamento da consistência (`EL`) ou consistência forte mesmo ao custo de maior latência (`EC`).

A importância do `PACELC` reside em reconhecer que o regime sem partição é o cenário predominante de operação, e que a escolha de consistência nesse regime determina, na prática, a experiência percebida pelos usuários e a corretude dos dados. Sistemas que classificam-se apenas pelo `CAP` omitem metade da decisão.

### 2.3 O Modelo ACID e a Integridade Transacional

O modelo `ACID` (_Atomicidade, Consistência, Isolamento, Durabilidade_) define as garantias de uma transação de banco de dados. A atomicidade assegura que uma transação é indivisível: ou é integralmente aplicada, ou integralmente descartada. A consistência garante que a transação leva o banco de um estado válido a outro estado válido. O isolamento controla o grau em que transações concorrentes são visíveis entre si. A durabilidade garante que, uma vez confirmada, a transação sobrevive a falhas.

Em contraposição, o modelo `BASE` (_Basically Available, Soft state, Eventual consistency_) descreve o comportamento de sistemas que relaxam as garantias `ACID` em favor de disponibilidade e latência, tipicamente adotando consistência eventual. A escolha entre `ACID` e `BASE` não é uma disputa de superioridade técnica, senão uma decisão condicionada à natureza da operação: determinados domínios toleram, por construção, estados intermediários e convergência posterior; outros exigem que o estado persistido seja, em todo instante, fiel à realidade que representa.

### 2.4 Níveis de Isolamento e Controle de Concorrência

O controle de concorrência define como o banco gerencia operações simultâneas sobre os mesmos dados. Os níveis de isolamento do SQL — Read Uncommitted, Read Committed, Repeatable Read e Serializable — graduam o compromisso entre o paralelismo de leituras/escritas e a proteção contra anomalias de concorrência, tais como leituras sujas, leituras não repetíveis e escrita fantasma. São essas anomalias que, em aplicações sem o devido controle, materializam-se como _race conditions_ — comportamentos imprevisíveis decorrentes de escritas concorrentes que dependem da ordem de chegada.

O mecanismo de controle de concorrência multiversão (`MVCC`, _Multi-Version Concurrency Control_) é de particular relevância: ao manter múltiplas versões de um registro, permite que leituras consistentes sejam servidas sem bloquear escritores concorrentes. Essa característica é determinante para sistemas com volume significativo de escrita e leitura simultâneas, pois reduz a contenção e eleva o _throughput_ sem abrir mão da consistência das leituras.

## 3. Análise Aplicada à Natureza do Sistema

### 3.1 Requisito de Integridade Transacional Irredutível

O sistema ao qual esta decisão se destina opera sobre transações de natureza financeira. Nesse domínio, a corretude do estado persistido é um atributo inegociável e constitutivo do produto: não há espaço para estados intermediários visíveis, para operações parcialmente aplicadas ou para divergências entre o que foi registrado e o que é apresentado. Uma operação classificada como concluída deve existir por completo, de forma íntegra e reproduzível, e qualquer interrupção deve reverter o processo integralmente.

Essa exigência determina o eixo central da decisão: o núcleo de escrita do sistema adota, obrigatoriamente, o modelo `ACID` com consistência forte. A possibilidade de relaxamento desse requisito — por exemplo, via consistência eventual — não existe nesse núcleo, pois uma leitura que retorne um estado obsoleto ou parcialmente aplicado constituiria falha de negócio, não uma otimização aceitável.

### 3.1.1 Enquadramento no Modelo PACELC

Aplicando o `PACELC` ao núcleo transacional, a classificação é inequívoca: `PC/EC` — consistência sob partição e consistência no regime normal.

Sob partição (`PC`), a disponibilidade é sacrificada em favor da integridade: diante da impossibilidade de garantir que uma escrita será observada de forma consistente pelos nós, o sistema prefere recusar a operação a registrá-la de forma divergente. Em domínios financeiros, a indisponibilidade temporária é um evento administrável e limitado; a corrupção silenciosa de dados não é recuperável sem custo irreparável de confiança e conformidade.

No regime normal (`EC`), o sistema paga o custo de latência associado à garantia de consistência — confirmação de escrita, aplicação de locks de concorrência e validação de invariantes — porque a alternativa, servir leituras eventualmente consistentes, violaria o requisito de fidelidade do estado apresentado.

### 3.1.2 Separação de Políticas de Consistência por Fronteira de Domínio

A classificação `PC/EC` aplica-se ao núcleo transacional. Reconhece-se, contudo, a existência de cargas de trabalho de leitura analítica — agregações, extratos e relatórios sobre grandes volumes históricos — para as quais a latência de resposta é o atributo dominante e a consistência eventual é aceitável e desejável. Para esse eixo, a classificação almejada é `EL`: otimização de latência mediante leituras sobre réplicas dedicadas ou projeções derivadas, tolerando atraso controlado entre a escrita transacional e a leitura analítica.

A coexistência dessas duas políticas distintas — consistência forte para escrita e leitura transacional, consistência eventual para leitura analítica — é viabilizada por um único `SGBDR` desde que o mecanismo escolhido ofereça suporte nativo a réplicas de leitura e a particionamento físico. A consistência eventual, quando adotada, é estritamente confinada a leituras de projeção, nunca ao núcleo de escrita.

### 3.2 O Núcleo de Dados Analíticos

O contexto analítico opera sobre representações estatísticas, agregações, extratos e projeções de grandes volumes históricos, cuja finalidade é subsidiar a tomada de decisão. Sua natureza é essencialmente distinta da do núcleo transacional: enquanto este é fonte de verdade e participa diretamente das operações financeiras em curso, o Núcleo de Dados Analíticos é derivado e orientado à consulta. Essa distinção de natureza fundamenta um tratamento arquitetural próprio e não subsidiário, pois os atributos que o qualificam — latência de resposta, disponibilidade de leitura e capacidade de agregação — não coincidem com os que regem o núcleo de escrita.

### 3.2.1 Domínio e Enquadramento no Modelo PACELC

Aplicando o `PACELC` ao núcleo analítico, a classificação é `EL`: consistência eventual tolerada em favor de latência. Sob partição, a indisponibilidade da leitura analítica não compromete a integridade do núcleo transacional, que permanece `PC/EC` intacto, pois o analítico é derivado e jamais fonte de verdade. No regime normal, aceita-se um atraso controlado entre a escrita transacional e a disponibilidade da projeção: o custo de latência é trocado por consistência forte apenas onde ela é essencial, e a fidelidade dos dados analíticos é garantida por convergência posterior, não por sincronismo imediato.

### 3.2.2 Estratégia de Abordagem

A viabilização da política `EL` apoia-se em três mecanismos complementares. Primeiro, projeções alimentadas por eventos de domínio, desacopladas da escrita transacional: a liquidação registra-se no núcleo `PC/EC` e publica eventos que alimentam as projeções analíticas sem que a operação aguarde a agregação. Segundo, réplicas de leitura dedicadas, que desviam a carga analítica do nó primário e preservam a capacidade transacional do núcleo de escrita. Terceiro, particionamento físico por critérios temporais ou dimensionais, que mantém a performance de agregações sobre volumes crescentes. A fronteira de domínio é o elemento que permite coexistirem, em um único `SGBDR`, políticas de consistência distintas e deliberadas, cada uma confinada ao eixo que a justifica.

## 4. Comparação entre SGBDRs

Todos os candidatos considerados — `PostgreSQL`, `MySQL`, `SQL Server` e `Oracle` — pertencem à classe relacional e, portanto, compartilham as garantias `ACID` e a consistência forte como propriedades centrais. A diferenciação ocorre em mecanismos de concorrência, fidelidade da aritmética decimal, capacidades operacionais e custo total de propriedade.

No tocante à aritmética decimal, o domínio financeiro exige precisão exata em valores monetários, sem os erros de representação de ponto flutuante. O tipo numérico de precisão arbitrária com arredondamento explícito é um requisito. A implementação do `PostgreSQL` nesse aspecto é madura e estável, alinhando-se à representação decimal de alta precisão adotada na camada de aplicação.

No tocante à concorrência, o `MVCC` do `PostgreSQL` permite leituras consistentes sem bloqueio de escritores, mitigando contenção sob carga mista de leitura e escrita. Esse mecanismo é essencial para o comportamento de detecção de conflitos de escrita concorrente — estratégia preferida sobre o bloqueio preventivo — pois a detecção de conflito só é operacionalmente viável em um motor que não degrade o _throughput_ sob concorrência moderada, sem que a aplicação precise conviver com _race conditions_ não detectadas.

No tocante a custo de propriedade, candidatos proprietários adicionam custo de licenciamento sem vantagem funcional que compense, dentro do perfil de requisitos estabelecido. A escolha recai, portanto, sobre o candidato de código aberto que melhor atende aos três eixos, o `PostgreSQL`.

## 5. Capacidades Operacionais e de Escala

A decisão considera também a trajetória de evolução da carga, não apenas o estado atual. O `SGBDR` escolhido deve acomodar crescimento de volume sem exigir troca de tecnologia, sob pena de comprometer investimentos realizados em modelagem e integrações. Nesse sentido, três capacidades são determinantes:

- **Particionamento físico de tabelas**: a segmentação de dados por critérios temporais ou dimensionais é o mecanismo primário para manter a performance de consultas analíticas sobre volumes crescentes, sem degradação do custo de manutenção de índices.
- **Réplicas de leitura**: o desvio de cargas analíticas para réplicas dedicadas preserva a capacidade transacional do nó primário e viabiliza a política de consistência eventual confinada a leituras de projeção.
- **Tipos e extensões**: a capacidade de estender o modelo de dados para representações semiestruturadas e operações específicas de domínio, quando necessário, amplia a longevidade da solução sem exigir `SGBDR` adicional.

A conjugação dessas capacidades em um único sistema elimina a complexidade operacional de operar múltiplas tecnologias de persistência para o mesmo domínio, reduzindo a superfície de falhas e o custo de operação.

## 6. Conclusão

A escolha do `PostgreSQL` como `SGBDR` fundamenta-se na convergência de três linhas de raciocínio independentes e complementares.

A primeira, de natureza teórica, decorre da aplicação do teorema `CAP` e do refinamento `PACELC`: o núcleo de escrita do sistema classifica-se como `PC/EC`, exigindo consistência forte tanto sob partição quanto no regime normal, e o `PostgreSQL` atende a essa classificação sem ressalvas. A segunda, de natureza funcional, decorre do modelo `ACID` e dos mecanismos de controle de concorrência: a integridade transacional e a detecção de conflitos de escrita são propriedades inerentes e maduras no sistema escolhido. A terceira, de natureza operacional, decorre das capacidades de particionamento, réplicas de leitura e precisão decimal, que garantem a evolução da carga sem troca de tecnologia.

A classificação `PC/EC` do núcleo transacional não é uma limitação, mas uma decisão deliberada condicionada à natureza do domínio: a integridade do estado persistido é um requisito inegociável, e qualquer trade-off que a relaxasse trocaria uma falha controlada e administrável por uma falha silenciosa e irreparável. O relaxamento de consistência é admitido, exclusivamente, no eixo de leitura analítica, onde é seguro e desejável, e é confinado por fronteiras de domínio claramente definidas.

A adoção do `PostgreSQL` atende, portanto, simultaneamente, aos requisitos de integridade transacional, de comportamento sob concorrência, de precisão aritmética e de evolução em escala, constituindo a solução tecnicamente justificada dentro do universo relacional.
