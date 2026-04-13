# Database SQL (PostgreSQL 17+)

## Production Script

- `production.sql`: **production-ready full initialization script**.
  - Includes full schema.
  - Includes minimal baseline master data only.
  - Excludes all Postman/demo/testing data.

### Production Execution (new environment)

```bash
psql -h <host> -p <port> -U <user> -d postgres -f sql/00_init_database.sql
psql -h <host> -p <port> -U <user> -d stufamily -f sql/production.sql
```

## Other SQL Files (for development/migration)

- `01_schema.sql`: clean schema-only script.
- `02_postman_seed.sql`: deterministic Postman/manual test data (do not run in production).
- `03_admin_user_split.sql`: migration for splitting admin account to `sys_admin_user`.
- `04_site_profile_geo.sql`: migration for `site_profile.latitude/longitude`.
- `05_parent_message_thread.sql`: migration for threaded parent messages + read/reply/close flags.
- `06_family_member_phone.sql`: migration for `family_member_card.phone`.
- `07_create_initial_admin.sql`: optional bootstrap for first production admin account (`admin`).
- `08_site_profile_contact_upgrade.sql`: migration for `site_profile.contact_person/contact_wechat_qr_url`.
- `09_family_checkin_member_upgrade.sql`: migration for `family_checkin.family_member_id`.

## Notes

- All scripts use UTF-8 settings.
- `production.sql` is idempotent for baseline data via `ON CONFLICT` and `WHERE NOT EXISTS`.
- `production.sql` should be treated as the preferred one-shot initialization script for deployment.
- After `production.sql`, you can run `07_create_initial_admin.sql` to create/reset initial admin password.
