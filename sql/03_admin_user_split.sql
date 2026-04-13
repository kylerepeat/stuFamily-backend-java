SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

BEGIN;

SET search_path TO stufamily, public;

CREATE TABLE IF NOT EXISTS sys_admin_user (
    id BIGSERIAL PRIMARY KEY,
    user_no VARCHAR(32) NOT NULL UNIQUE,
    status user_status NOT NULL DEFAULT 'ACTIVE',
    token_version BIGINT NOT NULL DEFAULT 0,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    avatar_url TEXT,
    phone VARCHAR(32),
    email VARCHAR(128),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sys_admin_user_status ON sys_admin_user (status);
CREATE INDEX IF NOT EXISTS idx_sys_admin_user_phone ON sys_admin_user (phone);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sys_admin_user_updated_at ON sys_admin_user;
CREATE TRIGGER trg_sys_admin_user_updated_at BEFORE UPDATE ON sys_admin_user FOR EACH ROW EXECUTE FUNCTION set_updated_at();

INSERT INTO sys_admin_user (
    user_no,
    status,
    token_version,
    username,
    password_hash,
    nickname,
    avatar_url,
    phone,
    email,
    last_login_at,
    created_at,
    updated_at
)
SELECT
    su.user_no,
    su.status,
    COALESCE(su.token_version, 0),
    su.username,
    su.password_hash,
    su.nickname,
    su.avatar_url,
    su.phone,
    su.email,
    su.last_login_at,
    su.created_at,
    su.updated_at
FROM sys_user su
WHERE su.username IS NOT NULL
  AND su.password_hash IS NOT NULL
  AND su.user_type IN ('ADMIN', 'HYBRID')
ON CONFLICT (username) DO UPDATE SET
    status = EXCLUDED.status,
    token_version = EXCLUDED.token_version,
    password_hash = EXCLUDED.password_hash,
    nickname = EXCLUDED.nickname,
    avatar_url = EXCLUDED.avatar_url,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    last_login_at = EXCLUDED.last_login_at,
    updated_at = NOW();

COMMIT;
