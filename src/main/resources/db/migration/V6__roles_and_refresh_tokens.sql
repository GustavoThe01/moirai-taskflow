-- Papel do usuário no sistema. Quem já tinha conta continua como USER.
ALTER TABLE app_user ADD COLUMN role TEXT NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN'));

-- Refresh tokens. Só o hash SHA-256 é gravado: com acesso ao banco, ninguém consegue usar os tokens daqui.
-- family_id liga os tokens nascidos do mesmo login. Se um token já trocado for usado de novo, a família inteira é revogada.
CREATE TABLE refresh_token (
    id         TEXT PRIMARY KEY,
    user_id    TEXT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL,
    family_id  TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP
);

CREATE UNIQUE INDEX ux_refresh_token_hash ON refresh_token (token_hash);
CREATE INDEX ix_refresh_token_family ON refresh_token (family_id);
