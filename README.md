# Forum

A Spring Boot + React forum application with topics, replies, JWT authentication, role-based editing, admin user management, and maintenance mode.

## Project Structure

```text
Backend/          Spring Boot backend API
Frontend/          React frontend served by Vite locally or nginx in Docker
docker-compose.yaml
README.md
```

Keep backend-specific Maven commands inside `Backend`. Keep frontend npm commands inside `Frontend`. Run Docker Compose from the repository root.

## What Runs In Docker

`docker-compose.yaml` starts the full application stack:

- `postgres` - PostgreSQL 16 database with a persistent Docker volume.
- `backend` - Spring Boot API built from `Backend/Dockerfile`.
- `ui` - React UI built from `Frontend/Dockerfile` and served by nginx.
- `adminer` - optional database browser.

The UI container proxies API requests like `/post`, `/auth`, `/user`, and `/maintenance` to the backend container, so the browser uses one origin and no CORS setup is needed.

## Prerequisites

- Docker Desktop or Docker Engine with Docker Compose v2.
- Free local ports, unless overridden:
  - `2233` for the UI
  - `9000` for the backend API
  - `5432` for PostgreSQL
  - `8090` for Adminer

## Quick Start

From the repository root:

```bash
docker compose up --build
```

Open:

- UI: http://localhost:2233
- Backend health: http://localhost:9000/healthz
- OpenAPI YAML: http://localhost:9000/openapi.yaml
- Scalar docs: http://localhost:9000/scalar.html
- Adminer: http://localhost:8090

Default seeded admin account:

```text
username: admin
password: admin123
```

## Background Mode

```bash
docker compose up --build -d
docker compose logs -f backend ui
```

Stop containers:

```bash
docker compose down
```

Stop containers and delete the database volume:

```bash
docker compose down -v
```

Use `down -v` only when you want a clean database and fresh Flyway migrations.

## Compose Configuration

Compose works without a `.env` file because development defaults are included. For custom ports or secrets, create a root `.env` file next to `docker-compose.yaml`:

```properties
FORUM_UI_PORT=2233
FORUM_BACKEND_PORT=9000
FORUM_POSTGRES_PORT=5432
FORUM_ADMINER_PORT=8090

FORUM_DEV_DB_NAME=forum
FORUM_DEV_DB_USERNAME=admin
FORUM_DEV_DB_PASSWORD=admin
FORUM_JWT_TOKEN_TTL=PT1H
FORUM_DEV_JWT_SECRET=replace-with-base64-32-byte-secret
```

Generate a JWT secret:

```bash
openssl rand -base64 32
```

PowerShell alternative:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

The default Compose JWT secret is for local development only.

## Adminer Login

Open http://localhost:8090 and use:

```text
System: PostgreSQL
Server: postgres
Username: admin
Password: admin
Database: forum
```

If you changed the database values in root `.env`, use those values instead.

## Main API Features

- `POST /auth/register` - register a standard user.
- `POST /auth/login` - receive a JWT token.
- `GET /post` - list topics.
- `POST /post` - create a topic as a logged-in user.
- `GET /post/{id}` - open a topic and increment view count.
- `PUT /post/{id}` - edit own topic, or any topic as moderator/admin.
- `GET /post/{id}/replies?page=0&size=10` - read paginated replies.
- `POST /post/{id}/replies` - create a reply inside a topic.
- `PUT /reply/{id}` - edit own reply, or any reply as moderator/admin.
- `GET /user`, `POST /user`, `PUT /user/{id}`, `DELETE /user/{id}` - admin user management.
- `POST /maintenance/backup/start`, `POST /maintenance/restore/start`, `POST /maintenance/finish` - admin maintenance controls.

Roles:

- `admin` can do everything and can create/update moderators.
- `moderator` can create content, edit own content, and edit other users' topics/replies.
- `user` can create topics/replies and edit only their own content.

## Local Development Without Full Compose

Start only PostgreSQL from the repository root:

```bash
docker compose up -d postgres
```

Run the backend:

```bash
cd Backend
cp .env.dev.example .env.dev
```

Edit `Backend/.env.dev` and set a real `FORUM_DEV_JWT_SECRET`. The default database URL in `.env.dev.example` points to `localhost:5432`, which matches the Compose PostgreSQL port.

Then start Spring Boot:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Run the UI in a second terminal:

```bash
cd Frontend
npm install
npm run dev
```

The Vite dev server proxies API calls to `http://localhost:9000` by default. Override it with:

```bash
VITE_API_TARGET=http://localhost:9000 npm run dev
```

## Tests And Builds

Backend compile/package:

```bash
cd Backend
./mvnw -DskipTests package
```

Backend tests:

```bash
cd Backend
./mvnw test
```

UI production build:

```bash
cd Frontend
npm ci
npm run build
```

Validate Compose from the repository root:

```bash
docker compose config
```

