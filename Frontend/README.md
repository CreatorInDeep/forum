# Forum UI

React UI for the Spring Forum API.

## API endpoints used

- `POST /auth/login`
- `POST /auth/register`
- `GET /post`
- `POST /post`
- `GET /post/{id}`
- `PUT /post/{id}`
- `GET /post/{id}/replies?page=0&size=10`
- `POST /post/{id}/replies`
- `GET /reply`
- `POST /reply`
- `GET /reply/{id}`
- `PUT /reply/{id}`
- `GET /user`
- `POST /user`
- `PUT /user/{id}`
- `DELETE /user/{id}`
- `GET /maintenance/status`
- `POST /maintenance/backup/start`
- `POST /maintenance/restore/start`
- `POST /maintenance/finish`
- `GET /healthz`

## Run locally

Start the Spring backend from `../Forum-BE` first. By default it listens on port `9000`.

```bash
cd ../Forum-BE
./mvnw spring-boot:run
```

Then start the UI from `Forum-UI`:

```bash
npm install
npm run dev
```

The Vite dev server proxies API calls to `http://localhost:9000`. Override it with:

```bash
VITE_API_TARGET=http://localhost:9000 npm run dev
```

Seeded local admin credentials from the backend tests and migration are:

```text
admin / admin123
```
