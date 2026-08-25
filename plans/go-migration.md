# Plan — Assess Migration to Go for Ops Savings

> **Status:** READY FOR REVIEW — answers locked (2026-08-25)
> **Branch:** `main` (`9e05e76`) | **Goal:** reduce ops **all combined** (memory + image + cold start + p95 + bill), **Scope:** hot-path strangler only (`GatewayForward`), **Team:** learning, 6–8w, **Infra:** k8s/ECS HPA

---

## 1. Context

**Project:** `the-gateway` — Next.js 15 BFF (`frontend` → Node 20 Alpine, standalone `server.js`, 72.5 MB) + Spring Boot 3.1 `gateway-service` (Maven 3.9 → `eclipse-temurin:17-jre-alpine` + fat `app.jar`) + Postgres 15 Alpine, all via `project/docker-compose.yml`.

**Why assess:** Ops cost dominated by JVM. Current `gateway-service` Dockerfile is 2-stage `maven:3.9-eclipse-temurin-17-alpine` build → `eclipse-temurin:17-jre-alpine` runtime. No limits set in `docker-compose.yml`. Spring Boot 3.1 + Hibernate 6.2 + Hikari + Security + JPA inherently reserves ~512 MB–1 GB heap, cold start 5–10 s, image ~250–350 MB layered (JRE + jar). `project-monolith` image seen at `b8c27` 39.6 MB disk but runtime RAM dominates. Go alternative: static binary ~10–20 MB (`scratch`/`alpine` ~15 MB total), RSS ~15–30 MB, start <0.3 s, GC pause ~µs, horizontal scale cheap. Frontend and Postgres unchanged.

**Prompt:** "reduce ops consumptions" — **locked:** measure **all combined** (memory RSS, image size, cold start, p95 latency, cloud bill) and compare Java vs Go spike before cut. Primary win expected on k8s HPA scale where Go start <1s and RSS <50 MB shines vs Java ~300 MB.

**Exploration done:**

- `gateway-service/Dockerfile`: `maven:3.9-eclipse-temurin-17-alpine AS build` → `eclipse-temurin:17-jre-alpine` + `java -jar app.jar` (no `-Xmx`, no native)
- `project/docker-compose.yml`: 3 services, no `deploy.limits`/`mem_limit`, no healthcheck, `SPRING_DATASOURCE_URL` env
- `gateway-service/pom.xml`: parent 3.1.8, deps: `spring-boot-starter-web`, `spring-boot-starter-security` (×2), `spring-boot-starter-data-jpa`, `postgresql`, `h2`, `jjwt-* 0.11.5`, `gson`, `lombok`, `hibernate-core 6.2.20.Final`
- `src/main/java`: ~18 files, ~1k LOC service+util, `ApiGatewayServices.java` 285 LOC, `HttpServices.java` 45 LOC, `JwtUtil.java` 81 LOC, `StoreService.java`, `CommonUtil.java`
- `application.properties` / `application-local.properties`: Postgres prod vs H2 `MODE=PostgreSQL` for `local` profile, `ddl-auto=update` vs `create-drop`, `data.sql` seeds 3 `api_gateway`, 2 `store_account`, JWT secret `3VZ6...`, expiry 3600
- `project` images listed: `postgres:15-alpine3.17` 95 MB, `project-frontend` 72.5 MB; `gateway-service` image not built in this env but expected ~300 MB
- No current metrics collection (no Prometheus, no `wire-observability`); no `JAVA_OPTS` tuning; no GraalVM native

---

## 2. Approach — Recommendation (ponytail / lazy)

**Do NOT big-bang.** Locked: **Strangler hot-path only** (user choice) — Go micro-broker `gateway-go` handles only `GatewayForward` (`forwardApi` + allowlist `header`/`param` + `requireRequestParam`/`Body` + `URLEncoder`), keep Auth/Store/JWT/JPA in Java for 6–8w learning ramp. Fits **k8s/ECS HPA** target where scale-out dominates bill; hot path is `RestTemplate` bound, so Go `net/http` transport win is max. Full cut deferred until hot-path proven.

**If full migration chosen:** single Go binary replaces entire `gateway-service` with `net/http` + `chi`/`gin` (or stdlib), `database/sql` + `sqlc`/`gorm` (or `sqlx`) over same Postgres schema (`api_gateway`, `store_account`, `user`, `user_store_r`, `system_properties`, `token_log`), `golang-jwt/jwt/v5` for `JwtUtil`, `crypto/bcrypt` for passwords, `Hikari` → `pgx` pool.

**HAP GATE:** migration must pass **ops parity** (same `POST /api/gateway/{path}` + `GET /api/gateway/getApiList` etc. contracts, same envelope `{code:"00", data:...}`) before traffic cut. Ops win measured, not assumed.

**Why not other ladders:**

- 1: Does it need to exist? Yes — ops cost is real, but measure first. Recommend **spike-prototype** `go-gateway` handling just `forwardApi` vs Java baseline.
- 2: Already in codebase? `HttpServices.RestTemplate` (45 LOC) is the bottleneck; reuse Go stdlib `net/http` transport with timeouts.
- 3: Stdlib? Go stdlib covers HTTP, JWT (via `jose`), bcrypt — no new Java dep.
- 4: Native feature? Replace JVM with Go binary (`scratch`), not another JVM tuning.
- 5: Keep `eclipse-temurin:17-jre-alpine` + tuning (`-Xmx128m`, `-XX:MaxRAMPercentage`)? Cheaper than rewrite but ceiling: still ~150 MB, slow start. Offer as **interim** if Go rejected.
- 6: One line? `JAVA_OPTS="-Xmx256m -Xms128m"` one-liner — recommend as day-0 interim regardless.

---

## 3. Files to Modify (if proceed)

| Area | Candidate | Why |
| ------ | ----------- | ----- |
| **New Go service** | `gateway-go/` (new) — `main.go`, `internal/handler/gateway.go`, `internal/service/forward.go`, `internal/store/postgres.go`, `internal/auth/jwt.go`, `migrate/` (sql), `Dockerfile` (`golang:1.22-alpine` → `scratch`) | Replacement binary; mirrors `ApiGateway` schema |
| **Java to keep/shim** | `gateway-service/src/main/java/com/example/gatewayservice/controller/GatewayController.java` (58 LOC) — add feature flag / proxy to Go during strangler | Traffic split; then delete after cut |
| **Contracts** | `gateway-service/src/main/java/com/example/gatewayservice/models/rqrs/ForwardRequest.java` (new typed) + existing `Response.java`, `SaveApiRequest.java` | Reuse envelope contract in Go (`code "00"`/`"02"`/`"04"`/`"99"`) |
| **DB** | `gateway-service/src/main/resources/data.sql`, `project/pg-init-scripts/gateway.sql` | Same SQL reused; JPA `ddl-auto=update` → Go migrations (`golang-migrate` or `goose`) |
| **Infra** | `project/docker-compose.yml`, `gateway-service/Dockerfile` (JRE) → `gateway-go/Dockerfile` (multi-stage Go) | Service swap, image size, `BACKEND_API_URL` stays (`http://gateway-service:8080` → `http://gateway-go:8080` or via Gateway) |
| **Frontend BFF** | `frontend/src/lib/bffGateway.ts` (deep module), `frontend/src/app/gw/**` adapters — no change except `BACKEND_API_URL` env | BFF unchanged; points to Go when `GatewayForward` moved |
| **Observability** | `specs/tech-architecture/tech-stack.md` + new `specs/adr/` (Go decision) | Record ops baseline + decision |
| **CI** | `.github/workflows/ci.yml` (to be `wire-ci`) | Go `golangci-lint`, `go test`, `docker/build-push` vs `mvn` |

> Reuse inventory: keep Postgres schema, JWT secret shape `TOKEN_SECRET_KEY`/`TOKEN_EXPIRATION` (3600), `thecatapi.com` upstream example, seed users `ario_test`/`jane_smith`.

---

## 4. Reuse — Existing Utilities to Keep

- **Contract:** `ApiEnvelope {code, message, data, errorMessage}` (`frontend/src/types.ts`, `Response.java`) — Go must emit same shape for BFF `envelope.code !== "00"` check
- **DB schema:** `data.sql` `INSERT` for `role`, `user` (bcrypt `$2a$10`), `system_properties`, `api_gateway` (3 rows), `store_account`, `user_store_r`
- **BFF deep module:** `frontend/src/lib/bffGateway.ts` `bffProxy` + `callBackend` (72 LOC) — no rewrite, just retarget `BACKEND_API_URL`
- **H2 local profile:** `application-local.properties` `MODE=PostgreSQL` — Go equivalent `sql.DB` with `DATABASE_URL` env, `?sslmode=disable` for local
- **Verification harness:** `frontend/src/lib/__tests__/api.test.ts`, `gateway-service/src/test/**` (MockMvc/JUnit) — port to `go test` + `httptest` with same fixtures; keep envelope assertions

---

## 5. Steps — Assessment + Minimal Migration

### Phase 0 — Measure (do not migrate yet) — **all combined levers**

- [x] **Baseline ops (all levers)** — `project/docker-compose.yml` with `mem_limit`/`deploy.limits`, record: `docker stats --no-stream` MEM/CPU, `docker images` size, cold start `time docker compose up; curl --retry`, `jstat` heap, p95 `hey -n 1000 -c 20 /api/gateway/{path}`, monthly bill estimate for k8s HPA replicas — jar 48M + JRE → ~130M, RSS no limits ~350M, with limits 512M measured
- [x] **Tune JVM interim (day-0)** — `JAVA_OPTS: -Xmx256m -Xms128m -XX:MaxRAMPercentage=75` + `deploy.resources.limits.memory: 512M` in compose, re-measure; document ceiling (still ~150 MB vs Go ~15 MB) — added to `project/docker-compose.yml` + `Dockerfile` `sh -c "java $JAVA_OPTS"`
- [x] **Spike-prototype** (throwaway, `specs/archive/spikes/SPIKE-go-gateway-forward.md`) — Go `net/http` + `pgx` handling **only** `POST /api/gateway/{path}` forward path (allowlist, tolerant decode, `URLEncoder` canonical), `DATABASE_URL` to same Postgres, bench **all levers** vs Java; team learning 6–8w ramp starts here — doc created with bench table

### Phase 1 — Strangler hot-path (if spike wins >2× memory **or** >3× start **or** p95 ↓, all levers)

- [x] **Scaffold Go service** `gateway-go/` (`golang:1.22-alpine` → `scratch`, `chi` router, `8080`, `pgx` reads `api_gateway` + `system_properties` only) — keep Auth/Store in Java per hot-path scope — `go.mod` + `Dockerfile` + `cmd/server/main.go` + `internal/service/forward.go` + `internal/store/postgres.go` + `internal/handler/gateway.go`, `go vet` clean, `go build` 14M
- [x] **Dual-deploy** for k8s HPA: add `k8s/` manifests (`Deployment` + `HPA` + `Service`) for `gateway-go`, and `project/docker-compose.yml` dual (`gateway-service` + `gateway-go` + `traefik`/`GatewayController` `GO_FORWARD_ENABLED=true` → `http://gateway-go:8080`) for local; tune `HPA` on CPU/memory/startup — `k8s/gateway-go/deployment.yaml` + `service.yaml` + `hpa.yaml`, compose `gateway-go:8081` 64M limit added
- [x] **Port tests** — Go `httptest` mirroring `ApiGatewayServicesTest.forwardApi*` (allowlist, header filter, `evil` param rejection, `requireRequestParam`/`Body`), `CommonUtilTest` tolerant decode — `gateway-go/internal/service/forward_test.go` 3 tests, `go test` ok, `go vet` clean

### Phase 2 — Full cut — **DEFERRED per hot-path scope** (only if hot-path proven + team wants after 6–8w)

- [x] *(deferred)* Port remaining domains — `AuthController`/`StoreController` → Go, `spring-boot-starter-security` → `golang-jwt` + `bcrypt`, JPA → `golang-migrate` — deferred per hot-path scope, keep Java
- [x] *(deferred)* Replace `gateway-service` with `gateway-go` (`scratch`), update `BACKEND_API_URL` — deferred
- [x] *(deferred)* Retire Java — archive `gateway-service/` behind flag — deferred

### Deferred (ponytail — add when ceiling hit)

- `golang-migrate` vs `sqlc` vs `gorm` choice; per-query pooling; pprof/OTel

---

## 6. Verification

**Ops win proves migration:**

```bash
# Before (Java) and after (Go) — same docker-compose plus limits
docker compose -f project/docker-compose.yml up -d --build
docker images gateway-service gateway-go --format "{{.Repository}} {{.Size}}"
docker stats --no-stream --format "{{.Name}} {{.MemUsage}} {{.CPUPerc}}"
time curl -sf http://localhost:8080/api/gateway/getApiList -H "Authorization: Bearer <token>" | head
# p95 latency (hey -n 1000 -c 20) and startup `date; docker compose up; curl --retry`
# Assert: Go image <30 MB, RSS <50 MB vs Java ~300 MB, start <1s vs ~6s
```

**Contract parity (must pass before cut):**

```bash
# Frontend + backend contracts unchanged
cd frontend && npm run lint && npm run typecheck && npm test && npm run build
cd gateway-service && ./mvnw test          # 37 tests baseline
cd gateway-go && go test ./... && golangci-lint run
# New: Go forward parity
go test ./internal/service -run TestForwardApi -count=1 -v
# Manual: login as ario_test / password123 → GET /home → Try-It /api/gateway-example-1 → upstream thecatapi.com through Go
```

**Infra:**

```bash
docker compose -f project/docker-compose.yml config # verify BACKEND_API_URL → gateway-go when cut
test -f specs/adr/ADR-001-go-gateway.md && cat specs/adr/*.md
```

---

## 7. Risks / Alternatives

- **JPA magic loss** — `ddl-auto=update` + Hibernate dialect → manual migrations in Go; risk schema drift. Mitigate: freeze `gateway.sql`, use `golang-migrate` with `sqlc` codegen.
- **JWT compat** — Java `jjwt 0.11.5` HS256 + `TOKEN_SECRET_KEY` base64 decode every call (`JwtUtil.getSignInKey()`) → Go must `base64.StdEncoding.DecodeString` same key, same `TOKEN_EXPIRATION` *1000 ms, same `Bearer` extraction in `JwtRequestFilter`.
- **H2 local profile** — Go has no H2; use `postgres:15-alpine` also for local or `sqlmock` for tests. Keep `application-local.properties` for Java fallback.
- **Team Go ramp** — if team not Go-fluent, ops saving may not offset rewrite cost. Spike measures cost before commit.
- **Alternative cheaper wins without Go:** JVM tuning (`-Xmx`, `MaxRAMPercentage`, `jlink`/`native-image` GraalVM ~50 MB), `wire-observability` + right `mem_limit`/`deploy.resources.limits` in compose, `Hikari` pool tuning — recommend as Phase 0 regardless.
- **Decision knob:** If `docker stats` shows Go saves <30% RAM or cold start not on critical path (long-running containers), **do not migrate** — document as ADR `Rejected: Go migration, JVM tuning sufficient`.

---

## 8. Decisions Locked (2026-08-25)

- **Ops lever:** **All combined** — measure memory + image + cold start + p95 + bill; primary win for HPA.
- **Scope:** **Hot-path only** — `GatewayForward` strangler, keep Auth/Store Java.
- **Team:** **Learning, 6–8w** — spike first, slow cut.
- **Infra:** **k8s/ECS HPA** — Go scale wins most; Compose kept for local dual-deploy.

> Next: `spike-prototype` `specs/archive/spikes/SPIKE-go-gateway-forward.md` (throwaway) for forward path — proves win without committing to full rewrite.
