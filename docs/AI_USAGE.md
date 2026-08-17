# AI Usage

Este documento tem como objetivo elucidar o uso de AI durante o ciclo de desenvolvimento do projeto.

## Workflow

Com o objetivo de garantir o uso de modelos de IA de forma responsável e controlada, o primeiro passo, que funciona bem para mim, foi a criação de dois parâmetros fundamentais:

- Agentes
- Diretrizes

Essa pré definição garante alucinação quase zero, ativa revisão manual permitindo que o modelo de IA se sustente e esteja alinhado com os objetivos do projeto. Portanto, para 30% do tempo disponível para entregar o projeto, me debrucei sobre a criação de competências que ainda não existiam, e otimização de outras que eu já utilizava no dia-a-dia

### Agentes

Inicialmente me soa de bom tom utilizar agentes de IA específicos a cada tarefa. Entretanto, jogar arquivos .md com instruções soltos e sem qualquer padronização de nomenclatura, não só é uma má prática de organização como dificulta com que o modelo encontre as diretrizes.

Naturalmente muitas IDE's possuem contextos para a engine de seus modelos e para a minha sorte, a maioria segue o padrão AGENTS.md, buscando ativamente por esse documento antes de um processamento.

A lógica foi: Codar com uma IA que lê ativamente os documentos necessários com as premissas é melhor do que ela simplesmente cachear essas informações? A resposta é sim, pois dessa forma oa quantidade de tokens gasta é menor do que a refatoração por alucinação, dado que ela vai perdendo a força do contexto durante o desenvolvimento. Forçar o modelo a acompanhar as diretrizes faz total sentido.

Por tanto a estrutura para agentes é a seguinte para este projeto:

#### AGENTS.md

Garante premissas básicas e direciona o modelo às diretrizes, infraestrutura e domínio, que são premissas básicas de skills, bem como para os agentes específicos de cada tarefa: backend e frontend

#### BACKEND.md e FRONTEND.md

Contém diretrizes de desenvolvimento específicas que delimitam a stack, arquitetura e padrões de desenvolvimento da mais alta qualidade de mercado. Aqui eu confesso que gastei algumas horas.

Cada projeto precisa de um conjunto de especificações próprio, inerente à natureza do domínio, embasada nas regras de negócio. Posso até ter um esqueleto pré-pronto de algumas coisas que me levam à qualidade, como testes, nomenclaturas, global exception handler, etc. Mas a essência do projeto, necessita de ferramentas que conversem com a natureza de negócio.

Dito isso, fluxo de trabalho consiste basicamente em sugestão de escrita de código, documentada em um implementation_plan.md, minuciosamente revisado e aprovado por mim, para então ser executado pelo modelo. Evidentemente o modelo sabe mais do que eu, então algumas diretrizes o impedem de concordar com o que eu quero, e sempre criticar ideias, o que é excelente para aumentar ainda mais a qualidade com assuntos que fogem ao meu arcabolço.

Acredito que a cereja do bolo seja um auto-review com altos padrões de entrega para cada atuação. Testes são efetuados e por fim tudo se finaliza como em uma sintonia, mas sem aplausos no final. O que vem após é uma intensa bateria de testes manuais, depois automatizados. As críticas geram novos códigos, novos testes, novas revisões, e o ciclo se perpetua saudável.

### Diretrizes

As diretrizes, por sua vez, permitem que o modelo siga padrões, com o objetivo de evitar controles inadequados (e.g. solicitar um código, e ao invés de ele criar um plano para aquilo, simplesmente mexer no código sem autorização, fazendo o que der na telha e, pior, commitando isso de maneira automática para o remoto).

O exemplo entre parênteses é só um daqueles casos que todos já passamos de lidar com a frustração, acreditar que o modelo é burro, que eu ter feito por mim mesmo seria absolutamente mais rápido, melhor e recompensador. Não é o caso.

Nos dias de hoje, codar virou mais uma preferência na minha humilde opnião. Tento me munir então de conceitos, de todas as áreas que vão desde a concepção de um projeto, como refinamentos e escrita de histórias, bem como da escrita de código (padrões de arquitetura, design patterns, testes, git flow, etc), até a entrega efetiva, observability, CI/CD, troubleshooting e sustentação. A visão mais ampla me anima mais do que uma simples linha de código, EXATAMENTE por eu ser apaixonado pela linha de código e ver até onde ela pode nos levar com resultados tangíveis.

### Domain

Para atingir os prósitos determinados, eu gosto de criar um arquivo semi-manual chamado domain.md. Prefiro que o modelo gere esse documento, para tentar entender o ponto de vista e enfim impor o meu próprio. Com isso, surge esse documento e maneira completa, que serve como embasamento das regras de negócio.

Qualquer contradição aqui com os resquisitos previamente levantados pode ser um problema no projeto, então eu trabalho aqui revisando e escrevendo absolutamente todas as linhas para que o entendimento não escape em qualquer situação. Antes de uma diretriz para a IA, a é para mim!

Apesar disso, é importante notar que a criação de todos esses documentos trespassa o que eu penso ou meu interesse. A ideia é com que eles sejam otimizados para a leitura e processamento do LLM consumindo a menor quantidade de tokens quanto possível, os guardando para o processamento, que deve efetivamente ser pesado e agregar valor e qualidade de software, de fato.

## Do fim

Por fim, preciso declarar então os pontos onde eu me frusto com o desenvolvimento com IA, e sou sincero em dizer que é apenas um: Ao longo do tempo, mesmo esse AGENTS.md acaba por perder valor, dado o alto volume de processamento e parâmetros armazenados de contexto. Isso não permite a IA alucinar de toda forma, mas a permite "aceitar" desrespeitar alguns parâmetros, ou considerar mais uns em detrimento de outros.

De toda forma isso é inaceitável, vai estragar meu estado zen! Portanto, um caminho é simplesmente um parâmetro que força a leitura de todos os parâmetros por parte do modelo, começando por AGENTS.md, antes de qualquer coisa. O que eu construo para isso é um /d com instruções específicas para o modelo voltar aos eixos.

Na verdade isso tem um objetivo mais profundo que é o de instruir novas instâncias a trabalhos específicos, por meio de sub-agents, e aí tudo depende da oruestração. É aqui o verdadeiro ganho de tempo, qualidade e performance.

Esse texto inteiro, bem como o Readme, foram escritos de maneira orgânica (incluindo o desenho). Os documentos de agentes e diretrizes também, apesar de constantemente revisados por outro modelo. Os documentos técnicos foram escritos por um modelo (maioria salvo alguns) e incansávemente revisados por mim (é, gosto mesmo de escrever e ainda não achei um modelo que tenha habilidade para isso). Espero que se divirtam com esse projeto como foi divertido para mim, e espero que me considerem para a vaga. Estou super animado com a oportunidade em poder contribuir com vocês.
