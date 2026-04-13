SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

BEGIN;

SET search_path TO stufamily, public;

ALTER TABLE family_member_card ADD COLUMN IF NOT EXISTS phone VARCHAR(32);

COMMIT;
