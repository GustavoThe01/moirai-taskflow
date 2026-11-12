# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [Não lançado]

### Adicionado
- Usuários: `POST /api/v1/auth/register` (201, 400 ou 409) e `POST /api/v1/auth/login` (200 ou 401).
- Entidade de domínio `User`, com e-mail normalizado e único, e as portas `UserRepository` e `PasswordHasher`.
- Senhas com BCrypt (custo 10) pelo `BCryptPasswordHasher`; o banco guarda só o hash.
- Login com a mesma resposta e o mesmo tempo para e-mail desconhecido e senha errada.
- Senha limitada a 72 bytes, o máximo que o BCrypt considera.
- Spring Security: cadeia de filtros sem sessão, sem CSRF, sem formulário e sem HTTP Basic, com as rotas públicas declaradas.
- Migration `V5` com a tabela `app_user` e o índice único do e-mail.
- Respostas `Unauthorized` e `Conflict` documentadas no OpenAPI.
- 23 testes novos (144 no total).

## [0.2.0] - 2026-11-11

Segunda semana do roadmap: consultas, documentação da API, testes de camada, etiquetas, comentários e auditoria.

### Adicionado
- `GET /api/v1/projects/{id}/tasks`: lista as tarefas do projeto com paginação, ordenação (`createdAt`, `dueDate`, `title`) e filtros por status, prioridade, responsável e intervalo de prazo.
- Filtros montados com Specifications do Spring Data JPA; só os filtros preenchidos entram na consulta.
- Tipos de consulta (`TaskQuery`) na camada de aplicação, sem dependência do Spring Data: o adapter traduz para `Pageable`.
- Migration `V2` com índices compostos para a listagem por projeto.
- 12 testes novos.
- Documentação OpenAPI com springdoc 3.2: Swagger UI em `/swagger-ui.html` e contrato em `/v3/api-docs`.
- Cada endpoint com resumo, descrição, exemplos e os erros possíveis; as respostas de erro ProblemDetail são definidas uma vez em `components/responses`.
- Descrição automática do cabeçalho provisório `X-User-Id` em todas as operações.
- 5 testes que conferem o contrato publicado.
- Testes de fatia web (`@WebMvcTest`) para os controllers de projetos e tarefas: rota, validação, conversão de parâmetros e status de erro, com os casos de uso simulados por `@MockitoBean`.
- Testes de fatia JPA (`@DataJpaTest`) com o SQLite e as migrations reais: consulta JPQL de membros, Specifications combinadas e índices do Flyway.
- Dependência `spring-boot-starter-data-jpa-test` (no Spring Boot 4, cada fatia de teste tem o próprio módulo).
- 12 testes novos.
- Etiquetas por projeto (`Label`), com nome único sem diferenciar maiúsculas e cor `#RRGGBB`, e a relação N:N com tarefas em `task_label`.
- `PUT /api/v1/tasks/{id}/labels`: troca as etiquetas da tarefa, só do mesmo projeto e no máximo 10.
- Comentários imutáveis nas tarefas (`POST` e `GET /api/v1/tasks/{id}/comments`), só de membros do projeto.
- Migration `V3` com `label`, `task_label` e `comment`, chaves estrangeiras em cascata e índices.
- Correção de dois N+1, medidos antes e depois pelas estatísticas do Hibernate: tarefas com etiquetas (22 → 3 comandos, com `@BatchSize`) e projetos com membros (16 → 1, com `@EntityGraph`).
- 15 testes novos.
- JPA Auditing: `created_at` e `created_by` no projeto (sem mudar depois da criação), `updated_at` e `updated_by` em projetos e tarefas.
- `AuditorAware` com o usuário do cabeçalho `X-User-Id` e `DateTimeProvider` com o `Clock` da aplicação.
- Migration `V4` com as colunas de auditoria, `deleted_at` e o índice parcial `ix_project_active`.
- 4 testes de auditoria e exclusão lógica (121 no total).

### Alterado
- `DELETE /api/v1/projects/{id}` passa a fazer exclusão lógica: grava `deleted_at`, mantém a linha e as tarefas, e o projeto some de todas as consultas (`@SQLRestriction`).

## [0.1.0] - 2026-11-06

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
- Portas de entrada de projetos e `ProjectService`.
- API REST de projetos: criar, listar, detalhar, renomear e excluir, com DTOs e Bean Validation.
- Arquivo `requests.http` para testar a API à mão.
- 13 testes novos: 6 de casos de uso e 7 da API de ponta a ponta.
- API de tarefas: criar, atribuir responsável e mover no fluxo, com `TaskService` e `TaskController`.
- Erros padronizados com ProblemDetail (RFC 9457) em um único `ApiExceptionHandler`, incluindo validação campo a campo.
- `ForbiddenOperationException`, que separa falta de permissão (403) de regra de negócio violada (422).
- 14 testes novos: 5 de casos de uso e 9 da API de ponta a ponta.
