-- Schema inicial da Moirai.
-- Escrito em SQL portável (TEXT, CHECK, REFERENCES) para rodar também no PostgreSQL no Dia 15.
-- Ids são UUIDs em texto; datas seguem ISO 8601.
-- user_id ainda não tem chave estrangeira: a tabela de usuários entra no Dia 11.

CREATE TABLE project (
    id       TEXT PRIMARY KEY,
    name     TEXT NOT NULL,
    owner_id TEXT NOT NULL
);

CREATE TABLE project_member (
    project_id TEXT NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    user_id    TEXT NOT NULL,
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE task (
    id          TEXT PRIMARY KEY,
    project_id  TEXT NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    created_by  TEXT NOT NULL,
    created_at  TEXT NOT NULL,
    title       TEXT NOT NULL,
    description TEXT NOT NULL,
    priority    TEXT NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    due_date    TEXT,
    status      TEXT NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'REVIEW', 'DONE')),
    assignee_id TEXT
);

CREATE INDEX ix_task_project ON task (project_id);
