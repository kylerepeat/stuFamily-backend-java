SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

BEGIN;

SET search_path TO stufamily, public;

-- Use pgcrypto to generate bcrypt hash.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Initial admin account bootstrap.
-- Default login:
--   username: admin
--   password: ChangeMe@2026!
-- IMPORTANT: change password immediately after first login.
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
    created_at,
    updated_at
) VALUES (
    'A000000001',
    'ACTIVE',
    0,
    'admin',
    crypt('ChangeMe@2026!', gen_salt('bf', 10)),
    'System Admin',
    NULL,
    NULL,
    NULL,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO UPDATE SET
    status = 'ACTIVE',
    password_hash = EXCLUDED.password_hash,
    token_version = sys_admin_user.token_version + 1,
    nickname = EXCLUDED.nickname,
    updated_at = NOW();

COMMIT;
