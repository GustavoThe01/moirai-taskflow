# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [Não lançado]

### Adicionado
- Projeto Spring Boot 4.1 (Java 21) com Web MVC, Validation e Actuator.
- Estrutura de pacotes da Arquitetura Hexagonal.
- Endpoint `/actuator/health` e teste de carregamento de contexto.
- Pipeline de CI no GitHub Actions.
- ADR 0001 — Arquitetura Hexagonal; ADR 0002 — SQLite em desenvolvimento.
- Domínio puro, sem Spring: `Project`, `Task`, `TaskStatus` (máquina de estados), `TaskPriority` e ids tipados.
- Regras de membros do projeto, responsável obrigatório para iniciar a tarefa, revisão enviada só pelo responsável e prazo que não pode estar no passado.
- 38 testes unitários do domínio, com relógio fixo (`Clock`) para as regras de data.
- Persistência com Spring Data JPA, SQLite e Flyway (`V1__init.sql`), com o Hibernate apenas validando o schema.
- Portas de saída `ProjectRepository` e `TaskRepository`, implementadas pelos adapters de persistência.
- Entidades JPA separadas do domínio e `PersistenceMapper`; métodos de fábrica `restore` em `Project` e `Task`.
- ADR 0003: mappers escritos à mão em vez de MapStruct.
- 7 testes de integração contra um SQLite real.
