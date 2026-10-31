DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'notes_owner') THEN
        CREATE ROLE notes_owner NOLOGIN;
    END IF;
END
$$;

REASSIGN OWNED BY notes_app TO notes_owner;

GRANT CONNECT ON DATABASE secure_notes_db TO notes_app;
GRANT USAGE ON SCHEMA public TO notes_app;
REVOKE CREATE ON SCHEMA public FROM notes_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON app_users, notes TO notes_app;
GRANT SELECT, INSERT ON audit_logs TO notes_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO notes_app;
