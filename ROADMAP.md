# Roadmap — Moirai (20 dias / 4 semanas)

## Semana 1 — Fundação e CRUD  → `v0.1.0`

### Dia 01 — Bootstrap
- [x] Spring Boot 4.1 (Java 21, Maven, Web MVC, Validation, Actuator) — JPA, Flyway e SQLite movidos para o Dia 03
- [x] Estrutura de pacotes hexagonal; `/actuator/health` respondendo
- [x] GitHub Actions (`./mvnw verify`)
- [x] ADR 0001 — Arquitetura Hexagonal · ADR 0002 — SQLite em dev (dialeto `hibernate-community-dialects`)

### Dia 02 — Domínio puro
- [x] `Project`, `Task`, `TaskStatus` com **máquina de estados** (transições válidas)
- [x] Regras: prazo, prioridade, quem pode mover a tarefa
- [x] Testes unitários sem Spring (rápidos)

### Dia 03 — Persistência
- [x] Dependências: Data JPA, Flyway, `sqlite-jdbc`, `hibernate-community-dialects`
- [x] Migrations Flyway (`V1__init.sql`)
- [x] Entidades JPA **separadas** do domínio + mappers escritos à mão (decisão no ADR 0003)
- [x] Adapter de persistência implementando a porta de saída

### Dia 04 — Casos de uso e API de projetos
- [x] Portas de entrada (`CreateProjectUseCase`...) e serviços
- [x] `ProjectController` (CRUD), DTOs com `record`, Bean Validation
- [x] Arquivo `requests.http` para testar manualmente

### Dia 05 — API de tarefas e erros
- [x] Endpoints de tarefas (criar, mover status, atribuir)
- [x] `@RestControllerAdvice` retornando **ProblemDetail**
- [x] Tag `v0.1.0`

## Semana 2 — API madura  → `v0.2.0`

### Dia 06 — Paginação e filtros
- [x] `Pageable`, ordenação, filtros dinâmicos (status, responsável, prazo) com Specifications

### Dia 07 — Documentação
- [ ] springdoc-openapi (Swagger UI) com exemplos e descrições

### Dia 08 — Testes de camada
- [ ] `@WebMvcTest` (controllers), `@DataJpaTest` (repositórios)
- [ ] Testes de integração `@SpringBootTest` + MockMvc

### Dia 09 — Comentários e labels
- [ ] Relacionamento N:N (task ↔ label), comentários
- [ ] Detectar e corrigir **N+1** (`@EntityGraph` / fetch join); registrar no README

### Dia 10 — Auditoria
- [ ] JPA Auditing (`createdAt`, `updatedBy`), soft delete
- [ ] Tag `v0.2.0`

## Semana 3 — Segurança e infraestrutura  → `v0.3.0`

### Dia 11 — Usuários
- [ ] Cadastro e login, senha com BCrypt, Spring Security básico

### Dia 12 — JWT
- [ ] Access + refresh token, filtro de autenticação, roles

### Dia 13 — Autorização por recurso
- [ ] Só membros acessam o projeto (`@PreAuthorize` / verificação no caso de uso)
- [ ] Testes de segurança (401 vs 403)

### Dia 14 — Configuração
- [ ] Profiles `dev`/`test`/`prod`, segredos via variáveis de ambiente

### Dia 15 — Docker
- [ ] Dockerfile multi-stage, docker-compose
- [ ] Bônus: profile `prod` com PostgreSQL — **trocar o adapter prova a arquitetura**
- [ ] Preencher "Como rodar"; tag `v0.3.0`

## Semana 4 — Pronto para produção  → `v1.0.0`

### Dia 16 — Eventos
- [ ] Evento `TaskStatusChanged` + listener `@Async` (notificação simulada)

### Dia 17 — Cache e limites
- [ ] Cache com Caffeine (`@Cacheable` / `@CacheEvict`)
- [ ] Rate limiting por usuário (Bucket4j)

### Dia 18 — Observabilidade
- [ ] Logs estruturados (JSON), correlation id via MDC
- [ ] Métricas Micrometer customizadas (tarefas concluídas)

### Dia 19 — Qualidade
- [ ] JaCoCo ≥ 80%, **ArchUnit** garantindo que `domain` não depende de Spring
- [ ] SonarCloud + badges no README

### Dia 20 — Entrega
- [ ] README final: diagrama de arquitetura, fluxos, GIF/prints do Swagger
- [ ] `CHANGELOG.md`, tag `v1.0.0`, deploy opcional (Render / Fly.io)

## Ideias de evolução (opcional)
- WebSocket para atualizar o quadro em tempo real
- Testcontainers com PostgreSQL nos testes de integração
