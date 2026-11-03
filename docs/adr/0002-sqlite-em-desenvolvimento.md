# ADR 0002 — SQLite em desenvolvimento

- **Status:** Aceita
- **Data:** 2026-11-03

## Contexto
O projeto precisa rodar com um único comando, sem Docker, nas duas primeiras semanas.
O Hibernate não traz dialeto SQLite no núcleo.

## Decisão
Usar **SQLite** (`org.xerial:sqlite-jdbc`) com o dialeto de `hibernate-community-dialects`
nos profiles `dev` e `test`. O schema é versionado com **Flyway**. No Dia 15, o profile `prod`
passa a usar PostgreSQL.

## Alternativas consideradas
- **H2 em memória** — fácil, mas o comportamento difere de bancos reais e não persiste.
- **PostgreSQL desde o início** — mais realista, porém exige Docker já no Dia 01.

## Consequências
- ✅ Zero infraestrutura para começar.
- ✅ Migrations SQL escritas de forma portável facilitam a troca futura.
- ⚠️ SQLite tem um único escritor e tipos limitados; testes de concorrência ficam para o PostgreSQL.
