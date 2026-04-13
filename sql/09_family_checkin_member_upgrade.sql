SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

BEGIN;

SET search_path TO stufamily, public;

ALTER TABLE family_checkin
    ADD COLUMN IF NOT EXISTS family_member_id BIGINT REFERENCES family_member_card(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_family_checkin_member_checked
    ON family_checkin (family_member_id, checked_in_at DESC);

COMMIT;
