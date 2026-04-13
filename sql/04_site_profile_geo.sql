SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

BEGIN;

SET search_path TO stufamily, public;

ALTER TABLE site_profile ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 7);
ALTER TABLE site_profile ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7);

COMMIT;
