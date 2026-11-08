<div align="center">

<img src="docs/assets/banner.svg" alt="Moirai — API de projetos e tarefas" width="100%" />

# ♾ Moirai
### Tecer, medir e cortar o fio de cada tarefa

**API REST de gestão de projetos e tarefas (estilo Kanban) com Spring Boot 4 e Arquitetura Hexagonal.**
Domínio isolado do framework, segurança com JWT, autorização por recurso, eventos assíncronos,<br/>
cache, observabilidade e testes em todas as camadas, do domínio puro à API completa.

[![CI](https://github.com/GustavoThe01/moirai-taskflow/actions/workflows/ci.yml/badge.svg)](https://github.com/GustavoThe01/moirai-taskflow/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![Hibernate](https://img.shields.io/badge/JPA-Hibernate-59666C?style=flat-square&logo=hibernate&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-planejado-2496ED?style=flat-square&logo=docker&logoColor=white)
![Roadmap](https://img.shields.io/badge/roadmap-dia_07_de_20-7e22ce?style=flat-square)
![Testes](https://img.shields.io/badge/testes-90_passando-15803d?style=flat-square)

</div>

---

## 👨‍💻 Autor

<table>
  <tr>
    <td width="62%">

Eu sou o **Gustavo Osterno Thé**, e a **Moirai** é um dos cinco projetos do meu portfólio público de Desenvolvedor Pleno. Idealizei, desenvolvo e mantenho este repositório.

No [Nidavellir](https://github.com/GustavoThe01/nidavellir-inventory) eu construo tudo à mão; aqui o desafio é outro: **usar o Spring sem deixar o Spring tomar conta do domínio**. Planejei a API para ficar pronta para produção em 4 semanas, com segurança, Docker e observabilidade.

    </td>
    <td width="38%">

| | |
|---|---|
| 🌐 Site | [site-gustavothe-dev.web.app](https://site-gustavothe-dev.web.app/) |
| 💼 LinkedIn | [gustavoosterno](https://www.linkedin.com/in/gustavoosterno/) |
| 🐙 GitHub | [@GustavoThe01](https://github.com/GustavoThe01) |
| 📸 Instagram | [@gustavo.osterno](https://www.instagram.com/gustavo.osterno) |

    </td>
  </tr>
</table>

---

## 📑 Sumário

- [Em números](#-em-números)
- [A origem do nome](#-a-origem-do-nome)
- [O que a API faz](#-o-que-a-api-faz)
- [Endpoints planejados](#-endpoints-planejados)
- [Arquitetura Hexagonal](#️-arquitetura-hexagonal)
- [O ciclo de vida de uma tarefa](#-o-ciclo-de-vida-de-uma-tarefa)
- [Segurança](#-segurança)
- [Competências demonstradas](#-competências-demonstradas)
- [Metodologias e decisões](#-metodologias-e-decisões)
- [Roadmap](#️-roadmap)
- [Como rodar](#-como-rodar)
- [Testes](#-testes)
- [Estrutura do projeto](#-estrutura-do-projeto)
- [O que ainda falta](#-o-que-ainda-falta)
- [Projetos relacionados](#-projetos-relacionados)

---

## 📊 Em números

<div align="center">

| 🌐 API | 🏛️ Arquitetura | 🛡️ Produção | 🗓️ Processo |
|:---:|:---:|:---:|:---:|
| **16** endpoints planejados | Hexagonal (Ports & Adapters) | JWT access + refresh | **20** dias / **4** semanas |
| ProblemDetail (RFC 9457) | domínio **sem** Spring/JPA | cache · rate limiting | **3** ADRs registrados |
| OpenAPI / Swagger | regra verificada com ArchUnit | Docker · métricas · logs JSON | **4** releases (`v0.1` → `v1.0`) |

<sub>Metas para o fim do roadmap. O estado atual está nos badges, na seção <a href="#-testes">Testes</a> e em <a href="#-o-que-ainda-falta">O que ainda falta</a>.</sub>

</div>

---

## ♾ A origem do nome

> *Na mitologia grega, as Moiras eram três irmãs que controlavam o fio da vida: Cloto o tecia, Láquesis o media e Átropos o cortava.*

Escolhi o nome porque toda tarefa tem o mesmo destino: ela **nasce** (é criada), **é medida** (está em andamento, tem prazo e responsável) e **é cortada** (é concluída). A Moirai é a API que tece esse fio e garante que nenhuma tarefa pule etapas do seu destino.

| Moira | Papel no mito | Papel na API |
|---|---|---|
| 🧵 **Cloto** | Tece o fio | `TODO` — a tarefa é criada |
| 📏 **Láquesis** | Mede o fio | `IN_PROGRESS` / `REVIEW` — prazo, responsável, revisão |
| ✂️ **Átropos** | Corta o fio | `DONE` — a tarefa é encerrada |

---

## ✨ O que a API faz

| Módulo | O que resolve |
|---|---|
| 👤 **Usuários** | Cadastro, login com senha BCrypt e tokens JWT (access + refresh) |
| 📁 **Projetos** | Criação, membros e permissões: só quem participa enxerga o projeto |
| ✅ **Tarefas** | Criação, atribuição, prioridade, prazo e **máquina de estados** |
| 🏷️ **Labels** | Etiquetas reutilizáveis (relação N:N com tarefas) |
| 💬 **Comentários** | Discussão dentro de cada tarefa |
| 🔎 **Busca** | Paginação, ordenação e filtros dinâmicos (status, responsável, prazo) |
| 🔔 **Eventos** | Mudança de status dispara notificação assíncrona |
| 📈 **Operação** | Health check, métricas, logs estruturados e correlation id |

---

## 🌐 Endpoints planejados

| Método | Rota | Descrição | Dia |
|---|---|---|:---:|
| `GET` | `/actuator/health` | Saúde da aplicação | ✅ 01 |
| `POST` | `/api/v1/auth/register` | Cadastro de usuário | 11 |
| `POST` | `/api/v1/auth/login` | Login, retorna access + refresh token | 12 |
| `POST` | `/api/v1/auth/refresh` | Renova o access token | 12 |
| `GET` | `/api/v1/projects` | Lista projetos do usuário | ✅ 04 |
| `POST` | `/api/v1/projects` | Cria projeto | ✅ 04 |
| `GET` | `/api/v1/projects/{id}` | Detalhe do projeto | ✅ 04 |
| `PUT` | `/api/v1/projects/{id}` | Renomeia o projeto (só o dono) | ✅ 04 |
| `DELETE` | `/api/v1/projects/{id}` | Exclui o projeto (só o dono) | ✅ 04 |
| `POST` | `/api/v1/projects/{id}/members` | Adiciona membro | 13 |
| `GET` | `/api/v1/projects/{id}/tasks` | Lista tarefas com filtros, ordenação e paginação | ✅ 06 |
| `POST` | `/api/v1/projects/{id}/tasks` | Cria tarefa | ✅ 05 |
| `PATCH` | `/api/v1/tasks/{id}/status` | Move a tarefa no fluxo | ✅ 05 |
| `PATCH` | `/api/v1/tasks/{id}/assignee` | Atribui responsável | ✅ 05 |
| `POST` | `/api/v1/tasks/{id}/comments` | Comenta na tarefa | 09 |
| `PUT` | `/api/v1/tasks/{id}/labels` | Define labels | 09 |

> Até o Dia 12, o usuário é informado pelo cabeçalho `X-User-Id`, de forma temporária. Depois, o JWT o substitui. Deixei exemplos de todas as chamadas em [`requests.http`](requests.http).

Exemplo de erro padronizado (**ProblemDetail, RFC 9457**):

```json
{
  "type": "/problems/invalid-transition",
  "title": "Transição de status inválida",
  "status": 422,
  "detail": "Uma tarefa em TODO não pode ir para DONE.",
  "instance": "/api/v1/tasks/42/status"
}
```

---

## 🏛️ Arquitetura Hexagonal

O domínio fica no centro e **não conhece Spring, JPA nem HTTP**. O mundo externo conversa com ele por **portas** (interfaces) e **adaptadores** (implementações). Registrei essa decisão no [ADR 0001](docs/adr/0001-arquitetura-hexagonal.md).

```mermaid
flowchart LR
    subgraph IN["Adapters de entrada"]
        WEB["adapter/in/web<br/>Controllers REST · DTOs<br/>ProblemDetail"]
    end
    subgraph CORE["Núcleo da aplicação"]
        PIN["port/in<br/>Casos de uso"]
        SVC["service<br/>Implementação dos casos de uso"]
        DOM["domain<br/>Project · Task · TaskStatus<br/>regras puras"]
        POUT["port/out<br/>Repositórios · Notificações"]
    end
    subgraph OUT["Adapters de saída"]
        JPA["adapter/out/persistence<br/>Entidades JPA · Mappers"]
        NOTIF["Notificações<br/>@Async"]
    end
    DB[(SQLite dev<br/>PostgreSQL prod)]

    WEB --> PIN
    PIN --> SVC
    SVC --> DOM
    SVC --> POUT
    JPA -. implementa .-> POUT
    NOTIF -. implementa .-> POUT
    JPA --> DB
```

> 🔄 **A prova da arquitetura:** no Dia 15, vou trocar o banco de produção de SQLite para PostgreSQL **mexendo apenas no adapter de persistência**, sem tocar em domínio nem em casos de uso.

### Por dentro de uma requisição

```mermaid
sequenceDiagram
    actor C as Cliente
    participant F as JwtFilter
    participant W as TaskController
    participant UC as MoveTaskUseCase
    participant D as Task (domínio)
    participant P as TaskPersistenceAdapter
    participant E as EventPublisher

    C->>F: PATCH /tasks/42/status (Bearer token)
    F->>W: usuário autenticado
    W->>UC: moveTask(42, IN_PROGRESS, user)
    UC->>P: loadTask(42)
    P-->>UC: Task
    UC->>D: moveTo(IN_PROGRESS, user)
    alt transição válida e usuário é membro
        D-->>UC: ok + TaskStatusChanged
        UC->>P: save(task)
        UC->>E: publish(TaskStatusChanged)
        W-->>C: 200 OK
    else regra violada
        D-->>UC: exceção de domínio
        W-->>C: 422 ProblemDetail
    end
```

---

## 🔄 O ciclo de vida de uma tarefa

```mermaid
stateDiagram-v2
    [*] --> TODO : 🧵 Cloto tece
    TODO --> IN_PROGRESS : alguém assume
    IN_PROGRESS --> REVIEW : pede revisão
    REVIEW --> IN_PROGRESS : ajustes solicitados
    REVIEW --> DONE : ✂️ Átropos corta
    IN_PROGRESS --> TODO : devolvida
    DONE --> [*]
```

---

## 🔐 Segurança

| Camada | Como |
|---|---|
| **Senhas** | BCrypt, nunca em texto puro |
| **Autenticação** | JWT de curta duração (access) + refresh token |
| **Autorização** | Por papel (`USER`, `ADMIN`) e **por recurso**: só membros acessam o projeto |
| **Erros** | `401` sem token válido, `403` sem permissão, sem vazar detalhes internos |
| **Segredos** | Variáveis de ambiente por profile (`dev`, `test`, `prod`), nada no repositório |
| **Abuso** | Rate limiting por usuário (Bucket4j) |

---

## 🧠 Competências demonstradas

| Área | Conceitos |
|---|---|
| **Arquitetura** | Hexagonal, portas e adaptadores, separação domínio × JPA, mappers explícitos |
| **API REST** | Verbos e status corretos, paginação, filtros (Specifications), ProblemDetail, OpenAPI |
| **Persistência** | Spring Data JPA, Flyway, relacionamentos N:N, **problema N+1** e `@EntityGraph`, auditoria |
| **Segurança** | Spring Security, JWT, BCrypt, `@PreAuthorize`, autorização por recurso |
| **Assíncrono** | Eventos de domínio, `ApplicationEventPublisher`, `@Async` |
| **Performance** | Cache com Caffeine, rate limiting |
| **Observabilidade** | Logs JSON, correlation id (MDC), Micrometer, Actuator |
| **Testes** | Unitários puros, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`, ArchUnit |
| **DevOps** | Maven Wrapper, Docker multi-stage, docker-compose, GitHub Actions, SonarCloud |

---

## 🧭 Metodologias e decisões

- **Domínio primeiro**: escrevo as regras em Java puro e testo sem subir o Spring.
- **Roadmap diário público** com uma release por semana, registrado no [ROADMAP.md](ROADMAP.md).
- **Conventional Commits**, versionamento semântico e **ADRs**.

| ADR | Decisão | Status |
|---|---|---|
| [0001](docs/adr/0001-arquitetura-hexagonal.md) | Arquitetura Hexagonal (Ports & Adapters) | ✅ Aceita |
| [0002](docs/adr/0002-sqlite-em-desenvolvimento.md) | SQLite em desenvolvimento, PostgreSQL em produção | ✅ Aceita |
| [0003](docs/adr/0003-mappers-escritos-a-mao.md) | Mappers escritos à mão em vez de MapStruct | ✅ Aceita |

---

## 🗺️ Roadmap

| Semana | Dias | Tema | Release | Progresso |
|---|---|---|:---:|---|
| 1 | 01–05 | Fundação, domínio puro, persistência, CRUD de projetos e tarefas | `v0.1.0` | 🟪🟪🟪🟪🟪 |
| 2 | 06–10 | Paginação, OpenAPI, testes de camada, labels/comentários, auditoria | `v0.2.0` | 🟪🟪⬜⬜⬜ |
| 3 | 11–15 | Usuários, JWT, autorização por recurso, profiles, Docker | `v0.3.0` | ⬜⬜⬜⬜⬜ |
| 4 | 16–20 | Eventos, cache, rate limiting, observabilidade, qualidade e entrega | `v1.0.0` | ⬜⬜⬜⬜⬜ |

Detalhes no [ROADMAP.md](ROADMAP.md) · Mudanças no [CHANGELOG.md](CHANGELOG.md).

---

## 🚀 Como rodar

**Pré-requisito:** JDK 21 ([Temurin](https://adoptium.net/)). O Maven vem pelo wrapper.

```bash
git clone https://github.com/GustavoThe01/moirai-taskflow.git
```

```bash
cd moirai-taskflow
```

Subir a API (Linux/macOS/Git Bash; no Windows use `mvnw.cmd`):

```bash
./mvnw spring-boot:run
```

Verificar se está no ar:

```bash
curl http://localhost:8080/actuator/health
```

Resposta esperada: `{"groups":["liveness","readiness"],"status":"UP"}`

Na primeira execução, o Flyway cria o banco `moirai.db` na raiz do projeto e aplica as migrations.

> 🐳 A partir do Dia 15: `docker compose up` sobe a API com PostgreSQL.
> 📘 Documentação interativa em `http://localhost:8080/swagger-ui.html` e o contrato OpenAPI em `http://localhost:8080/v3/api-docs`. Cada endpoint tem descrição, exemplos e os erros possíveis, todos no formato ProblemDetail.

---

## 🧪 Testes

```bash
./mvnw verify
```

| Tipo | Ferramentas | Situação |
|---|---|---|
| Contexto da aplicação | `@SpringBootTest` | ✅ 1 teste (contexto carrega) |
| Domínio puro | JUnit + AssertJ, sem Spring | ✅ 38 testes (Task, Project, TaskStatus) |
| Casos de uso | JUnit + Mockito, sem Spring | ✅ 13 testes |
| Tipos de consulta | JUnit, sem Spring (página, filtro, limites) | ✅ 6 testes |
| API de ponta a ponta | `@SpringBootTest` + MockMvc + SQLite | ✅ 19 testes |
| Contrato OpenAPI | O documento publicado tem os endpoints, os erros e os exemplos | ✅ 5 testes |
| Controllers | `@WebMvcTest` + MockMvc | ⏳ Dia 08 |
| Persistência | `@SpringBootTest` + SQLite real | ✅ 8 testes (migrations, mapeamento, chave estrangeira, busca paginada) |
| Repositórios | `@DataJpaTest` | ⏳ Dia 08 |
| Segurança | testes `401` × `403` | ⏳ Dia 13 |
| Arquitetura | ArchUnit | ⏳ Dia 19 |
| Cobertura | JaCoCo (meta ≥ 80%) + SonarCloud | ⏳ Dia 19 |

---

## 📁 Estrutura do projeto

```
moirai-taskflow/
├── .github/workflows/ci.yml                 # ✅ build + testes a cada push
├── .mvn/wrapper/                            # ✅ Maven Wrapper
├── docs/
│   ├── adr/                                 # ✅ decisões de arquitetura
│   └── assets/                              # ✅ banner e imagens
├── src/main/java/dev/gustavo/moirai/
│   ├── MoiraiApplication.java               # ✅ ponto de entrada
│   ├── domain/                              # ✅ Project, Task, TaskStatus, ids e exceções
│   ├── application/
│   │   ├── port/in/                         # ✅ casos de uso de projetos e tarefas
│   │   ├── port/out/                        # ✅ ProjectRepository, TaskRepository
│   │   ├── query/                           # ✅ filtro, página e resultado, sem Spring Data
│   │   └── service/                         # ✅ ProjectService, TaskService
│   ├── adapter/
│   │   ├── in/web/                          # ✅ controllers, DTOs e erros padronizados
│   │   └── out/persistence/                 # ✅ entidades JPA, mapper, adapters, Specifications
│   └── config/                              # ✅ relógio e OpenAPI · ⏳ Dia 11 — segurança
├── src/main/resources/application.yml       # ✅ configuração, datasource e Actuator
├── src/main/resources/db/migration/         # ✅ migrations do Flyway
├── src/test/java/...                        # ✅ testes de domínio, contexto e persistência
├── requests.http                            # ✅ exemplos de chamadas à API
├── pom.xml
├── mvnw · mvnw.cmd
├── CHANGELOG.md
└── ROADMAP.md
```

---

## 🧩 O que ainda falta

Mantenho aqui um checklist vivo do que ainda vou incrementar neste repositório.

**Projeto**
- [x] Domínio puro com máquina de estados e testes (Dia 02)
- [x] JPA, Flyway, SQLite e adapter de persistência (Dia 03)
- [x] Casos de uso e API de projetos (Dia 04)
- [x] API de tarefas e erros padronizados com ProblemDetail (Dia 05) → `v0.1.0`
- [x] Paginação, filtros e ordenação com Specifications (Dia 06)
- [x] OpenAPI com springdoc e Swagger UI (Dia 07)
- [ ] Testes de camada com `@WebMvcTest` e `@DataJpaTest` (Dia 08)
- [ ] Labels, comentários, N+1 e auditoria (Dias 09–10) → `v0.2.0`
- [ ] Usuários, JWT e autorização por recurso (Dias 11–13)
- [ ] Profiles e Docker com PostgreSQL (Dias 14–15) → `v0.3.0`
- [ ] Eventos, cache, rate limiting, observabilidade (Dias 16–18)
- [ ] ArchUnit, JaCoCo, SonarCloud e release final (Dias 19–20) → `v1.0.0`

**README**
- [ ] Prints do Swagger UI e GIF de um fluxo completo (login → criar tarefa → mover)
- [ ] Coleção de requisições (`requests.http` / Postman) linkada aqui
- [ ] Badges de cobertura e de qualidade (SonarCloud)
- [ ] Link do deploy, se publicado (Render / Fly.io)
- [ ] Atualizar "Em números", endpoints e testes com valores reais
- [ ] Definir e adicionar uma licença (`LICENSE`)
- [ ] Versão em inglês do README

---

## 🔗 Projetos relacionados

| Projeto | Stack | Relação |
|---|---|---|
| ☿ [Mercurium](https://github.com/GustavoThe01/mercurium-sql) | SQLite | Fundamentos de modelagem e SQL |
| ⚒ [Nidavellir](https://github.com/GustavoThe01/nidavellir-inventory) | Java 21 | **O par deste projeto em Java puro**: o que lá faço à mão, aqui fica com o framework |
| ⚖ [Plutus](https://github.com/GustavoThe01/plutus-ledger) | C# + Dapper | Modelagem de domínio em .NET |
| ♾ **Moirai** | Spring Boot | *você está aqui* |
| ✦ [Bifrost](https://github.com/GustavoThe01/bifrost-orders) | ASP.NET Core | A versão .NET de uma API pronta para produção, com Clean Architecture |

<div align="center">

<sub>Fio tecido por mim · <a href="https://github.com/GustavoThe01">Gustavo Osterno Thé</a></sub>

</div>
