# ADR 0001 — Arquitetura Hexagonal (Ports & Adapters)

- **Status:** Aceita
- **Data:** 2026-11-03

## Contexto
A Moirai expõe uma API REST hoje, mas pode ganhar outras entradas (WebSocket, mensageria)
e trocar de banco (SQLite em dev, PostgreSQL em produção). Quero que o domínio de
projetos e tarefas não conheça Spring, JPA nem HTTP.

## Decisão
Adotar **Arquitetura Hexagonal**:

```
adapter/in/web  ──►  application/port/in  ──►  application/service  ──►  domain
                                                     │
                                                     ▼
adapter/out/persistence  ◄──implements──  application/port/out
```

- `domain` — modelo puro, sem anotações de framework.
- `application/port/in` — casos de uso que o mundo externo pode chamar.
- `application/port/out` — o que a aplicação precisa do mundo externo (repositórios, notificações).
- `adapter` — implementações concretas (controllers, JPA).

## Alternativas consideradas
- **MVC em camadas tradicional** (controller/service/repository) — familiar, porém entidades JPA
  viram o modelo de domínio e o framework vaza para as regras.
- **Clean Architecture** — conceitualmente equivalente; é a abordagem que aplico no projeto Bifrost (C#),
  então aqui pratico o vocabulário de portas e adaptadores.

## Consequências
- ✅ Domínio testável sem subir o contexto Spring.
- ✅ Troca SQLite → PostgreSQL afeta apenas o adapter de persistência (prova no Dia 15).
- ⚠️ Duplicação controlada: entidades JPA separadas do domínio + mappers.
- ⚠️ A regra será garantida com ArchUnit no Dia 19.
