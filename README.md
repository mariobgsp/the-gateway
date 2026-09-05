# The Gateway

Gateway services example — **Next.js 15 (App Router) BFF** + **Go backend (`gateway-go`, chi + pgx)** + **PostgreSQL**. Deep modules via `BffGateway` + `GatewayForward(ForwardRequest)`.

## Design Reference

[Figma Link](https://www.figma.com/design/8IKh4NrzxsXJajEt8jL32x/the-gateway?node-id=1-6344&t=RuiBqghCN5cc1GHp-1)

## Architecture

```
Browser ──▶ Next.js frontend (BFF route handlers /gw/*)
                    │  httpOnly session cookie (JWT never exposed to JS)
                    │  deep module src/lib/bffGateway.ts:bffProxy hides session→401 + envelope mapping
                    ▼
              Go backend gateway-go (JWT auth, ForwardRequest typed seam)
                    │  GatewayForward: handler.Forward → ForwardRequest{PathName,QueryParams,Headers,Body} → service.Do (allowlist, JavaURLEncode)
                    │  UpstreamPort internal (net/http client, 5s dial / 30s timeout)
                    ▼
              PostgreSQL + upstream APIs (e.g. thecatapi.com)
```

All backend calls proxied server-side. JWT in `httpOnly`, `SameSite=Lax`, `Secure` (production) cookie — never `localStorage` — guarded by `middleware.ts`.

## Ops Tuning

- `project/docker-compose.yml`: `gateway-go` 64M limit (HPA `k8s/gateway-go/{deployment,service,hpa}.yaml` 32Mi request / 64Mi limit, HPA 2–10 CPU 60%/mem 70%)
- `gateway-go/Dockerfile`: multi-stage `scratch` ~12M (ponytail ceiling)

## Running with Docker

1. Docker + Docker Compose
2. `cd project && docker compose up -d --build`

Services: Frontend `http://localhost:3000` (or `3001` when vivante occupies 3000), Backend Go `http://localhost:8080`, Postgres `5432`.

Default creds: `ario_test` / `password123` (seed `project/pg-init-scripts/gateway.sql`).

## Running locally without Docker

### Backend Go (Postgres)

```bash
cd ../project && docker compose up -d postgres   # or local Postgres
psql "$DATABASE_URL" -f project/pg-init-scripts/gateway.sql  # seed once
cd ../gateway-go
go run ./cmd/server  # DATABASE_URL=postgres://microservices:password@localhost:5432/gateway?sslmode=disable PORT=8080
```

### Frontend

```bash
cd frontend
npm install
npm run dev          # or BACKEND_API_URL=http://localhost:8080 npm run dev -- -p 3001
# prod: BACKEND_API_URL=http://localhost:8080 npm run build && npm run start -- -p 3001
```

## Testing

### Frontend (Vitest 20 tests — 15 api + 5 bffGateway)

```bash
cd frontend
npm test             # bffProxy 401/200/null→500/404 preserve/200+04→500
```

### Backend Go

```bash
cd gateway-go
go vet ./... && go test ./...
```

### E2e Playwright (full stack, Go + Postgres)

```bash
cd frontend
npx playwright install chromium
npm run test:e2e  # webServer boots Go :8080 + Next.js 3001 (3000 workaround vivante); Postgres must be seeded (see above)
# 10 passed: auth, API add/edit/delete/detail/Try-It, Store add/edit/regenerate/delete
```

### Lint / typecheck / build

```bash
cd frontend
npm run lint
npm run typecheck
npm run build
cd ../gateway-go && go vet ./... && go build -o /tmp/gateway-go ./cmd/server
```

## Verification

```bash
test -f specs/PLAN-AUDIT_LATEST.md && grep -q "Verdict.*READY" specs/PLAN-AUDIT_LATEST.md
npm run lint && npm run typecheck && npm test && npm run build   # frontend
cd ../gateway-go && go vet ./... && go test ./...                # backend
npm run test:e2e                                                  # e2e 10/10
```

## Environment variables

| Variable | Description | Default |
| ---------- | ------------- | --------- |
| `BACKEND_API_URL` | Go gateway URL | `http://localhost:8080` |
| `DATABASE_URL` (Go) | Postgres DSN for `gateway-go` | `postgres://microservices:password@postgres:5432/gateway?sslmode=disable` |
| `COOKIE_SECURE` | Set `true` on HTTPS | `false` local |

## Deep Modules

- `BffGateway` (`src/lib/bffGateway.ts:bffProxy`) — deep, hides `getSessionToken→401 {code:"98"}` + `callBackend` + `envelopeError` `httpStatus>=400?httpStatus:500`
- `GatewayForward` (`gateway-go/internal/service/forward.go`: `ParseForwardPath` tolerant `?foo→""` keep-last, no double-decode, `BuildForwardURL` canonical `JavaURLEncode`, `FilterHeaders` case-insensitive)
- `UpstreamPort` internal to `GatewayForward` (`net/http` client)

See `specs/tech-architecture/tech-stack.md`, `specs/PLAN-AUDIT_LATEST.md` (READY), `k8s/gateway-go/`.
