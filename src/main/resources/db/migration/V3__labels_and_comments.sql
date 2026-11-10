-- Etiquetas (N:N com tarefas) e comentários.

CREATE TABLE label (
    id         TEXT PRIMARY KEY,
    project_id TEXT NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    name       TEXT NOT NULL,
    color      TEXT NOT NULL CHECK (length(color) = 7 AND color LIKE '#%')
);

-- O nome é único dentro do projeto, sem diferenciar maiúsculas: "Bug" e "bug" são a mesma etiqueta.
CREATE UNIQUE INDEX ux_label_project_name ON label (project_id, lower(name));

-- Tabela de junção da relação N:N. Apagar a tarefa ou a etiqueta apaga o vínculo.
CREATE TABLE task_label (
    task_id  TEXT NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    label_id TEXT NOT NULL REFERENCES label (id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, label_id)
);

-- Para responder "quais tarefas têm esta etiqueta" sem varrer a junção inteira.
CREATE INDEX ix_task_label_label ON task_label (label_id);

CREATE TABLE comment (
    id         TEXT PRIMARY KEY,
    task_id    TEXT NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    author_id  TEXT NOT NULL,
    text       TEXT NOT NULL CHECK (length(text) BETWEEN 1 AND 2000),
    created_at TEXT NOT NULL
);

CREATE INDEX ix_comment_task_created ON comment (task_id, created_at);
