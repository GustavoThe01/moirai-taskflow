-- Índices para a listagem de tarefas do Dia 06.
-- A listagem sempre filtra por projeto; status e prazo são os filtros e ordenações mais comuns.
-- O índice simples ix_task_project fica coberto pelos compostos, que também começam por project_id.

DROP INDEX ix_task_project;

CREATE INDEX ix_task_project_status ON task (project_id, status);
CREATE INDEX ix_task_project_due_date ON task (project_id, due_date);
CREATE INDEX ix_task_project_assignee ON task (project_id, assignee_id);
