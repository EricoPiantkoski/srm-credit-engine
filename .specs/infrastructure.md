# ARCHITECTURE & ENVIRONMENT CONTEXT

- **Ambiente**: O usuário utiliza a IDE IntelliJ. Use seu potencial máximo, não economize tokens e aja como especialista. O projeto roda no Mac
- **Gestão de Segredos**: É ESTRITAMENTE PROIBIDO hardcodar senhas, tokens ou chaves de API no código-fonte. Utilize sempre variáveis de ambiente e configure o application.yml para ler essas variáveis

# Design Patterns
- Aplique o Design Pattern KISS para aplicar uma solução mais simples que resolve o problema, a fim de evitar complexidade desnecessária: Keep It Simple, Stupid
- Aplique o Design Pattern SOLID para alcançar os 5 níveis de design de classes /objetos adequada:
    - S - Single Responsibility Principle (Princípio da Responsabilidade Única)
    - O - Open/Closed Principle (Princípio Aberto/Fechado)
    - L - Liskov Substitution Principle (Princípio da Substituição de Liskov)
    - I - Interface Segregation Principle (Princípio da Segregação de Interfaces)
    - D - Dependency Inversion Principle (Princípio da Inversão de Dependência)
- Aplique o design pattern DRY (Don't Repeat Yourself) buscando constantemente NUNCA duplicar lógica/código. Se duas partes fazem a mesma coisa, extraia