# Secure Notes Platform

A REST API for private notes, built with security as the main requirement. Users register, log in with a JWT, and manage notes that only they can see. Every security-relevant action is recorded in an append-only audit log, and notes can be exported as a PDF.

Built with Java 21, Spring Boot 4, Spring Security, Spring Data JPA and PostgreSQL 17.

## Features

- Registration with BCrypt password hashing (cost 12)
- Stateless authentication with signed JWT access tokens (HS256, 15 minute lifetime)
- Logout that revokes the token immediately
- Notes CRUD with strict per-user isolation
- PDF export of all of a user's notes
- Audit log of logins, failures, token problems, note changes, exports and cross-user access attempts
- Append-only audit table, enforced by a database trigger
- Least-privilege database roles: the application cannot change the schema
- Strict input validation with consistent JSON errors (RFC 9457 Problem Details)
- Interactive API documentation with Swagger UI

## Quick start with Docker

### Prerequisites

- Docker Desktop, or Docker Engine with the Compose plugin
- `openssl`, to generate secrets (preinstalled on macOS and Linux)

Java, Maven and PostgreSQL all run inside containers.

### 1. Clone the repository

```bash
git clone https://github.com/aliou27/secure_platforme_note.git
cd secure_platforme_note
```

### 2. Create the `.env` file with fresh secrets

```bash
cp .env.example .env
```

Replace each placeholder in `.env` with a generated value:

| Variable | Generate with |
|---|---|
| `POSTGRES_PASSWORD` | `openssl rand -hex 24` |
| `DB_PASSWORD` | `openssl rand -hex 24` |
| `JWT_SECRET` | `openssl rand -base64 32` |

`.env` is ignored by Git and Docker and must never be committed.

### 3. Build and start

```bash
docker compose up --build
```

The first build takes a few minutes while Maven downloads dependencies. The application is ready when the logs show `Started SecurePlatformeApplication`. On first start, PostgreSQL creates the roles, schema and audit protections from the `db/` folder.

### 4. Open the API documentation

Open http://localhost:8080/swagger-ui.html, call `/api/auth/login`, click **Authorize** and paste the `accessToken`.

### Stopping and resetting

```bash
docker compose down
```

To delete all data and recreate the database on the next start:

```bash
docker compose down -v
```

The database initialization scripts only run when the data volume is empty.

## Using the API

### Register

```bash
curl -i -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"username":"alice","password":"correct-horse-battery"}'
```

### Log in and keep the token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"alice","password":"correct-horse-battery"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
```

Tokens expire after 15 minutes.

### Notes

```bash
curl -i -X POST http://localhost:8080/api/notes -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"title":"First note","content":"Hello"}'
```

```bash
curl -i "http://localhost:8080/api/notes?page=0&size=20" -H "Authorization: Bearer $TOKEN"
```

```bash
curl -i http://localhost:8080/api/notes/1 -H "Authorization: Bearer $TOKEN"
```

```bash
curl -i -X PUT http://localhost:8080/api/notes/1 -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"title":"First note, edited","content":"Hello again"}'
```

```bash
curl -i -X DELETE http://localhost:8080/api/notes/1 -H "Authorization: Bearer $TOKEN"
```

### Export all notes as a PDF

```bash
curl -OJ http://localhost:8080/api/notes/export -H "Authorization: Bearer $TOKEN"
```

The file is saved under the server's name, for example `notes-alice-2026-10-31.pdf`.

### Log out

```bash
curl -i -X POST http://localhost:8080/api/auth/logout -H "Authorization: Bearer $TOKEN"
```

## API reference

| Method | Endpoint | Auth | Description | Success |
|---|---|---|---|---|
| POST | `/api/auth/register` | Public | Create an account | `201` |
| POST | `/api/auth/login` | Public | Get a JWT access token | `200` |
| POST | `/api/auth/logout` | JWT | Revoke the current token | `204` |
| GET | `/api/auth/me` | JWT | Current user | `200` |
| POST | `/api/notes` | JWT | Create a note | `201` |
| GET | `/api/notes?page=0&size=20` | JWT | List your notes, newest first | `200` |
| GET | `/api/notes/{id}` | JWT | Read one of your notes | `200` |
| PUT | `/api/notes/{id}` | JWT | Replace title and content | `200` |
| DELETE | `/api/notes/{id}` | JWT | Delete one of your notes | `204` |
| GET | `/api/notes/export` | JWT | Download your notes as a PDF | `200` |

### Validation rules

| Field | Rule |
|---|---|
| `username` | 3 to 50 characters: letters, digits, `.`, `_`, `-`. Stored in lowercase. |
| `password` | 12 to 72 characters (72 is the BCrypt input limit) |
| `title` | 1 to 255 characters |
| `content` | 1 to 20,000 characters |
| `page` / `size` | 0 to 10,000 / 1 to 100 |
| Unknown JSON fields | Rejected |

### Status codes

| Status | Meaning |
|---|---|
| `400` | Invalid input, malformed JSON or unknown field |
| `401` | Missing, invalid, expired or revoked token, or wrong credentials |
| `404` | Note not found, or it belongs to another user |
| `409` | Username already taken |
| `500` | Unexpected error, logged on the server and never detailed to the client |

## Security design

**Authentication.** Passwords are hashed with BCrypt and never stored or logged in plaintext. Login returns a JWT signed with HMAC-SHA256; the key comes from `JWT_SECRET`, must be at least 256 bits, and the app refuses to start without it. The token holds only the user ID, issuer, timestamps and a unique ID (`jti`). Spring Security verifies signature, expiry and issuer on every request. Logout revokes the token's `jti`.

**No user enumeration.** Unknown username and wrong password return the same `401`, and both run a BCrypt check so they take the same time. Token errors return a generic `invalid_token`.

**User isolation.** Every endpoint except register and login requires a JWT, and new endpoints are protected by default. The note owner always comes from the verified token, never from the request. Every note query filters by note ID and owner ID. Another user's note returns `404`, not `403`, and the attempt is audited. Ownership can never change, and sorting is fixed by the server.

**Input handling.** Every body is a validated DTO; entities are never exposed. Errors never echo submitted values. DTOs hide passwords, tokens and note content from logs. JPA parameter binding prevents SQL injection.

**PDF export.** Generated with Apache PDFBox from plain text, with no HTML rendering, which rules out the SSRF issues of HTML-to-PDF converters. Built in memory, never written to disk, capped at the 1,000 most recent notes, and audited.

**Audit log.** Events: `USER_REGISTERED`, `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `LOGOUT`, `TOKEN_EXPIRED`, `TOKEN_INVALID`, `NOTE_CREATED`, `NOTE_UPDATED`, `NOTE_DELETED`, `NOTES_EXPORTED`, `UNAUTHORIZED_ACCESS`. Each entry stores type, username, timestamp, client IP and JSON details, never passwords, tokens or note content. Entries are written in their own transaction so failed operations are still recorded. A database trigger rejects any `UPDATE`, `DELETE` or `TRUNCATE`.

**Database privileges.** `notes_owner` (cannot log in) owns all objects. The app connects as `notes_app`, which can only read and write data and cannot create, alter or drop anything, so even a SQL injection could not change the schema or disable the audit trigger.

**Secrets and containers.** Secrets come from environment variables or `.env`, ignored by Git and Docker. The Docker database port is not published to the host, and the app container runs as a non-root user.

## Project structure

```text
.
├── Dockerfile                  Two-stage build: Maven build, then JRE-only runtime
├── docker-compose.yml          PostgreSQL and the application
├── .env.example                Required environment variables
├── db/
│   ├── schema.sql              Tables, indexes and the updated_at trigger
│   ├── audit_append_only.sql   Makes audit_logs append-only
│   ├── roles.sql               Applies the role split to an existing local database
│   └── docker/init.sh          First-start initialization for the Docker database
└── src/main/java/com/project/secure_platforme/
    ├── auth/                   Register, login, logout, current user
    ├── note/                   Notes CRUD and PDF export
    ├── user/                   Users
    ├── audit/                  Audit log
    ├── security/               Security config, JWT, token revocation
    ├── exception/              Global error handling
    └── config/                 OpenAPI configuration
```

## Running without Docker

Requires Java 21 and PostgreSQL 17. As the PostgreSQL superuser:

```bash
psql -d postgres -c "CREATE ROLE notes_app LOGIN PASSWORD 'choose-a-password';"
psql -d postgres -c "CREATE DATABASE secure_notes_db OWNER notes_app;"
psql -d secure_notes_db -c "SET ROLE notes_app;" -f db/schema.sql -f db/audit_append_only.sql
psql -d secure_notes_db -f db/roles.sql
```

Create `.env` in the project root from `.env.example` (only `DB_PASSWORD`, `JWT_SECRET` and `API_DOCS_ENABLED` are needed locally), then:

```bash
./mvnw spring-boot:run
```

## Configuration

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_PASSWORD` | Yes | | Password of the `notes_app` role |
| `JWT_SECRET` | Yes | | Base64 key, at least 32 bytes |
| `POSTGRES_PASSWORD` | Docker only | | PostgreSQL superuser password in the container |
| `DB_URL` | No | `jdbc:postgresql://127.0.0.1:5432/secure_notes_db` | Set automatically in Docker |
| `DB_USERNAME` | No | `notes_app` | Database user |
| `API_DOCS_ENABLED` | No | `false` | Enables Swagger UI. Keep it off in production. |

Hibernate runs with `ddl-auto: validate`: the app never changes the schema and refuses to start if entities and tables differ.

## Inspecting the Docker database

```bash
docker compose exec db psql -U postgres -d secure_notes_db
```

## Known limitations

- Token revocation is held in memory: lost on restart and not shared between instances. Tokens expire after 15 minutes anyway.
- No refresh tokens.
- The PDF font covers Latin text including French accents; other characters such as emoji appear as `?`.
- Behind Docker's port mapping, audit entries may show the Docker gateway IP instead of the real client IP.
- HTTPS is expected to be terminated by a reverse proxy in production.
- No rate limiting: login attempts are audited but not throttled.
