# The Gateway

Next.js 15 API management UI with a Go gateway backend and PostgreSQL.

## Architecture

```text
Browser
  │ httpOnly gw_session cookie
  ▼
Next.js BFF /gw/[...path]
  │ bffGateway: auth, timeout, envelope mapping
  ▼
Go gateway-go
  ├── JWT auth + rate limiting
  ├── API and store services
  └── GatewayForward → configured upstream APIs
```

All backend calls are server-side. JWTs use an `httpOnly`, `SameSite=Lax`, secure-in-production cookie; they are never exposed to browser JavaScript or stored in `localStorage`.

## Run locally

Start PostgreSQL and seed it:

```bash
cd project && docker compose up -d postgres
psql "$DATABASE_URL" -f project/pg-init-scripts/gateway.sql
```

Start the backend:

```bash
cd gateway-go
DATABASE_URL=postgres://microservices:password@localhost:5432/gateway?sslmode=disable go run ./cmd/server
```

Start the frontend:

```bash
cd frontend
BACKEND_API_URL=http://localhost:8080 npm run dev
```

The frontend runs on `http://localhost:3000`; the Playwright configuration uses `3001` to avoid a local port conflict.

Seed credentials: `ario_test` / `password123`.

## Tests and builds

```bash
cd gateway-go && go build ./... && go vet ./... && go test ./...
cd frontend && npm run lint && npm run typecheck && npm test && npm run build
cd frontend && npm run test:e2e
```

## Key modules

- `frontend/src/lib/bffGateway.ts` — session authentication, backend transport, timeout, sanitization, and envelope mapping.
- `frontend/src/lib/gwRoutes.ts` — declarative `/gw/*` route table.
- `gateway-go/internal/api` — response envelope, error codes, and error mapping.
- `gateway-go/internal/service/forward.go` — typed `ForwardRequest` pipeline.
- `gateway-go/internal/store` — entity-specific PostgreSQL queries.

Local PostgreSQL runtime files under `project/db-data/` are ignored and are not part of the repository.
