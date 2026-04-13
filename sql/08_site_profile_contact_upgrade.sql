SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

BEGIN;

SET search_path TO stufamily, public;

ALTER TABLE site_profile ADD COLUMN IF NOT EXISTS contact_person VARCHAR(64);
ALTER TABLE site_profile ADD COLUMN IF NOT EXISTS contact_wechat_qr_url TEXT;

COMMIT;
