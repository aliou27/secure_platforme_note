#!/bin/sh
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -v app_password="$APP_DB_PASSWORD" <<'EOSQL'
CREATE ROLE notes_owner NOLOGIN;
CREATE ROLE notes_app LOGIN PASSWORD :'app_password';
ALTER DATABASE secure_notes_db OWNER TO notes_owner;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
SET ROLE notes_owner;
\i /db/schema.sql
\i /db/audit_append_only.sql
RESET ROLE;

REVOKE ALL ON DATABASE secure_notes_db FROM PUBLIC;
GRANT CONNECT ON DATABASE secure_notes_db TO notes_app;
GRANT USAGE ON SCHEMA public TO notes_app;
REVOKE CREATE ON SCHEMA public FROM notes_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON app_users, notes TO notes_app;
GRANT SELECT, INSERT ON audit_logs TO notes_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO notes_app;
EOSQL
