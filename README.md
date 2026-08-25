# The Gateway

Gateway services example — **Next.js 15 (App Router) BFF** + **Spring Boot 3.1 gateway-service** + **PostgreSQL**, with Go hot-path spike (`gateway-go`) for ops savings. Deep modules via `BffGateway` + `GatewayForward(ForwardRequest)` strangler.

## Design Reference

[Figma Link](https://www.figma.com/design/8IKh4NrzxsXJajEt8jL32x/the-gateway?node-id=1-6344&t=RuiBqghCN5cc1GHp-1)

## Architecture

```
Browser ──▶ Next.js frontend (BFF route handlers /gw/*)
                    │  httpOnly session cookie (JWT never exposed to JS)
                    │  deep module src/lib/bffGateway.ts:bffProxy hides session→401 + envelope mapping
                    ▼
              Spring Boot gateway-service (JWT auth, ForwardRequest typed seam)
                    │  GatewayForward: GatewayController → ForwardRequest(pathName,queryParams,headers,body) → ApiGatewayServices (allowlist, URLEncoder)
                    │  UpstreamPort internal (RestTemplate prod / InMemory test)
                    ▼
              PostgreSQL + upstream APIs (e.g. thecatapi.com)

Go spike (strangler, hot-path only, 14M scratch):
  gateway-go (chi + pgx, :8081) handles POST /api/gateway/{path} forward only — keep Auth/Store Java until proven
```

All backend calls proxied server-side. JWT in `httpOnly`, `SameSite=Lax`, `Secure` (production) cookie — never `localStorage` — guarded by `middleware.ts`.

## Ops Tuning

- `project/docker-compose.yml`: `JAVA_OPTS=-Xmx256m -Xms128m -XX:MaxRAMPercentage=75.0` + `deploy.resources.limits.memory 512M` for `gateway-service`; `gateway-go` 64M limit (HPA `k8s/gateway-go/{deployment,service,hpa}.yaml` 32Mi request / 64Mi limit, HPA 2–10 CPU 60%/mem 70%)
- `gateway-service/Dockerfile`: `sh -c "java $JAVA_OPTS -jar app.jar"` (ponytail ceiling)
- Images: jar 48M + JRE ~80M → ~130M Java vs Go `scratch` ~12M (spike), RSS Java ~200M capped vs Go ~18M, cold start 4s vs 0.2s (see `specs/archive/spikes/SPIKE-go-gateway-forward.md`)
- `gateway-go` hot-path only per `plans/go-migration.md` (team-pr, Conventional Commits, GitHub Actions, checkstyle) — dual-deploy `gateway-service:8080` + `gateway-go:8081` for bench

## Running with Docker

1. Docker + Docker Compose
2. `cd project && docker compose up -d --build`

Services: Frontend `http://localhost:3000` (or `3001` when vivante occupies 3000), Backend Java `http://localhost:8080`, Go spike `http://localhost:8081`, Postgres `5432`.

Default creds: `ario_test` / `password123` (seed `data.sql`).

## Running locally without Docker

### Backend Java (H2, no Postgres)

```bash
cd gateway-service
mise exec java@17.0.2 -- ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Backend Go spike (Postgres)

```bash
cd gateway-go
go run ./cmd/server  # DATABASE_URL=postgres://microservices:password@localhost:5432/gateway?sslmode=disable PORT=8081
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

### Backend Java (40 tests: 37 +3 CommonUtil tolerant)

```bash
cd gateway-service
mise exec java@17.0.2 -- ./mvnw test
```

### Go spike

```bash
cd gateway-go
go vet ./... && go test ./...   # forward_test 3 (allowlist/tolerant)
go build -o /tmp/gateway-go ./cmd/server  # 14M scratch
```

### E2e Playwright (full stack, real backend H2)

```bash
cd frontend
npx playwright install chromium
mise exec java@17.0.2 -- npm run test:e2e  # webServer H2 local + Next.js 3001 (3000 workaround vivante)
# 10 passed: auth, API add/edit/delete/detail/Try-It, Store add/edit/regenerate/delete
```

### Lint / typecheck / build

```bash
cd frontend
npm run lint
npm run typecheck
npm run build
cd ../gateway-service && mise exec java@17.0.2 -- ./mvnw package -DskipTests
cd ../gateway-go && go vet ./... && go build -o /tmp/gateway-go ./cmd/server
```

## Verification

```bash
test -f specs/PLAN-AUDIT_LATEST.md && grep -q "Verdict.*READY" specs/PLAN-AUDIT_LATEST.md
npm run lint && npm run typecheck && npm test && npm run build   # frontend
mise exec java@17.0.2 -- ./mvnw test                              # backend
go vet ./... && go test ./...                                     # go spike
mise exec java@17.0.2 -- npm run test:e2e                         # e2e 10/10
```

## Environment variables

| Variable | Description | Default |
| ---------- | ------------- | --------- |
| `BACKEND_API_URL` | Spring Boot / Go gateway URL | `http://localhost:8080` (Go `8081` when dual) |
| `DATABASE_URL` (Go) | Postgres DSN for `gateway-go` | `postgres://microservices:password@postgres:5432/gateway?sslmode=disable` |
| `SPRING_DATASOURCE_URL` etc. | Postgres for Java | docker overrides |
| `COOKIE_SECURE` | Set `true` on HTTPS | `false` local |
| `JAVA_OPTS` | Heap caps for Java | `-Xmx256m -Xms128m -XX:MaxRAMPercentage=75.0` |

## Deep Modules

- `BffGateway` (`src/lib/bffGateway.ts:bffProxy`) — deep, hides `getSessionToken→401 {code:"98"}` + `callBackend` + `envelopeError` `httpStatus>=400?httpStatus:500`
- `GatewayForward` (`ForwardRequest.java`, `CommonUtil.toForwardRequest` tolerant `?foo→""` keep-last, no second `URLDecoder`, limit 2, `ApiGatewayServices` helpers `loadApiGateway`/`filterHeaders`/`buildForwardUrl`/`requireBodyIfNeeded`, `splitConfig` shared)
- `UpstreamPort` internal to `GatewayForward` (`HttpServices` RestTemplate, `filterHeaders` case-insensitive `equalsIgnoreCase`)

See `specs/tech-architecture/tech-stack.md`, `specs/PLAN-AUDIT_LATEST.md` (READY), `plans/go-migration.md`, `k8s/gateway-go/`.
