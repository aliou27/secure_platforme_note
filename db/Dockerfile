FROM postgres:17

COPY --chmod=644 schema.sql audit_append_only.sql /db/
COPY --chmod=755 docker/init.sh /docker-entrypoint-initdb.d/init.sh
