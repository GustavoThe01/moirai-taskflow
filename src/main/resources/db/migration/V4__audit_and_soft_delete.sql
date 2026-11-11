-- Auditoria (quem e quando alterou) e exclusão lógica de projetos.
-- As datas de auditoria são declaradas como TIMESTAMP, o tipo que o Hibernate confere para Instant;
-- no SQLite o nome do tipo não muda o armazenamento, mas a validação do schema (ddl-auto: validate) depende dele.

ALTER TABLE project ADD COLUMN created_at TIMESTAMP;
ALTER TABLE project ADD COLUMN created_by TEXT;
ALTER TABLE project ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE project ADD COLUMN updated_by TEXT;

-- Projeto excluído continua no banco, com a data da exclusão. As consultas da aplicação ignoram essas linhas.
ALTER TABLE project ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE task ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE task ADD COLUMN updated_by TEXT;

-- A listagem de projetos filtra os não excluídos; o índice parcial cobre só as linhas que importam.
CREATE INDEX ix_project_active ON project (id) WHERE deleted_at IS NULL;
