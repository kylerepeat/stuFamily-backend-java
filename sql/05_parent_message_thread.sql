SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

BEGIN;

SET search_path TO stufamily, public;

ALTER TABLE parent_message ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE parent_message ADD COLUMN IF NOT EXISTS root_id BIGINT;
ALTER TABLE parent_message ADD COLUMN IF NOT EXISTS sender_type VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE parent_message ADD COLUMN IF NOT EXISTS viewed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE parent_message ADD COLUMN IF NOT EXISTS viewed_at TIMESTAMPTZ;
ALTER TABLE parent_message ADD COLUMN IF NOT EXISTS replied_at TIMESTAMPTZ;
ALTER TABLE parent_message ADD COLUMN IF NOT EXISTS closed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE parent_message ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE parent_message
SET sender_type = 'USER'
WHERE sender_type IS NULL;

UPDATE parent_message
SET root_id = id
WHERE root_id IS NULL;

UPDATE parent_message
SET status = 'APPROVED'
WHERE status <> 'APPROVED';

CREATE INDEX IF NOT EXISTS idx_parent_message_root ON parent_message(root_id, id);
CREATE INDEX IF NOT EXISTS idx_parent_message_user ON parent_message(user_id, id);
CREATE INDEX IF NOT EXISTS idx_parent_message_admin_list ON parent_message(sender_type, deleted, viewed, replied_at, created_at DESC);

COMMIT;
