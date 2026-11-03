# Moirai · taskflow-api

> *As Moiras tecem, medem e cortam o fio do destino — como uma tarefa de TODO a DONE.*

API REST de **gestão de projetos e tarefas** (estilo quadro Kanban) com **Spring Boot**,
**Arquitetura Hexagonal** (Ports & Adapters), JPA/Hibernate e SQLite.

## Competências demonstradas
- **Arquitetura Hexagonal:** domínio isolado do framework, portas de entrada/saída, adapters
- API REST bem desenhada: status codes, paginação, filtros, **ProblemDetail (RFC 9457)**
- **Spring Data JPA** + Flyway, relacionamentos N:N, combate ao problema **N+1**
- **Spring Security + JWT** (access/refresh token), autorização por recurso
- Eventos de domínio, processamento assíncrono, cache (Caffeine), rate limiting
- Observabilidade: logs estruturados, correlation id, Micrometer, Actuator
- Testes: unitários, `@WebMvcTest`, `@DataJpaTest`, integração, **ArchUnit**
- Docker multi-stage, docker-compose, GitHub Actions, OpenAPI/Swagger

## Domínio
`User` · `Project` (membros) · `Task` (TODO → IN_PROGRESS → REVIEW → DONE) · `Comment` · `Label`

## Arquitetura planejada
```
src/main/java/dev/gustavo/moirai/
├── domain/                 # modelo puro (sem anotações Spring/JPA)
├── application/
│   ├── port/in/            # casos de uso (interfaces)
│   ├── port/out/           # repositórios, notificações (interfaces)
│   └── service/            # implementações dos casos de uso
├── adapter/
│   ├── in/web/             # controllers, DTOs, handlers de erro
│   └── out/persistence/    # entidades JPA, mappers, repositórios
└── config/                 # segurança, beans, OpenAPI
```

## Como rodar
_A preencher no Dia 15 (Docker)._

## Status
Veja o [ROADMAP.md](ROADMAP.md).
