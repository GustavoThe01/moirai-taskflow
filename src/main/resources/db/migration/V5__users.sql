-- Usuários. A tabela se chama app_user porque "user" é palavra reservada no PostgreSQL (Dia 15).
-- O e-mail chega normalizado (minúsculo, sem espaços) pela aplicação; o índice único é a garantia final.
-- A senha nunca é gravada: só o hash BCrypt, que já carrega o sal e o custo.

CREATE TABLE app_user (
    id            TEXT PRIMARY KEY,
    email         TEXT NOT NULL,
    name          TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    created_at    TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX ux_app_user_email ON app_user (email);

-- As colunas user_id, owner_id e assignee_id das outras tabelas ainda não apontam para app_user:
-- até o JWT (Dia 12), o usuário vem do cabeçalho X-User-Id e pode não ter conta.
-- As chaves estrangeiras entram quando o id passar a vir de um token, que só existe para quem tem conta.
