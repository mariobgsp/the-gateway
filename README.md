# The Gateway

[![CI](https://github.com/mariobgsp/the-gateway/actions/workflows/ci.yml/badge.svg)](https://github.com/mariobgsp/the-gateway/actions/workflows/ci.yml)

Gateway services example — **Next.js 15 (App Router) BFF** + **Go backend (`gateway-go`, chi + pgx)** + **PostgreSQL**. Deep modules via `BffGateway` + `GatewayForward(ForwardRequest)`.

## Design Reference

[Figma Link](https://www.figma.com/design/8IKh4NrzxsXJajEt8jL32x/the-gateway?node-id=1-6344&t=RuiBqghCN5cc1GHp-1)

## Architecture

### Main: end-to-end request flow

```mermaid
flowchart LR
    Browser -->|"/gw/* + httpOnly session cookie<br/>(JWT never exposed to JS)"| BFF["Next.js BFF<br/>route handlers /gw/*"]
    BFF -->|"bffProxy: session→401 + envelope mapping<br/>src/lib/bffGateway.ts"| Go["Go backend gateway-go<br/>JWT auth + ForwardRequest seam"]
    Go -->|"service.Do: allowlist + JavaURLEncode<br/>UpstreamPort internal (5s dial / 30s timeout)"| DB[("PostgreSQL + upstream APIs<br/>(e.g. thecatapi.com)")]
```

### Supporting 1: BffGateway (bffProxy decision flow)

```mermaid
flowchart TD
    A["/gw/* adapter<br/>{backendPath, method, validate}"] --> B["getSessionToken()"]
    B -->|"no token"| C["401 {code: 98}"]
    B -->|"token"| D["callBackend + sanitizeHeaders/Params (private)"]
    D -->|"envelope.code != 00"| E["error passthrough<br/>httpStatus>=400 ? httpStatus : 500"]
    D -->|"envelope.code == 00"| F["200 + envelope"]
```

### Supporting 2: GatewayForward (Forward.Do pipeline)

```mermaid
flowchart TD
    A["handler.Forward<br/>ParseForwardPath (?foo→'', last-wins, no double-decode)"] --> B["ForwardRequest<br/>{PathName, QueryParams, Headers, Body}"]
    B --> C["Lookup config by identifier<br/>miss → 404 code 02"]
    C --> D["BuildForwardURL<br/>allowlist + JavaURLEncode"]
    D --> E["FilterHeaders<br/>case-insensitive allowlist"]
    E --> F["requireRequestBody gate<br/>empty → 400 code 04"]
    F --> G["invokeUpstream (net/http)<br/>5s dial / 30s timeout"]
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
