# SPIKE — Go Gateway Forward (hot-path) — Throwaway

> **Status:** DRAFT spike, not production. Throw away after decision. 6-8w learning ramp starts here.
> **Date:** 2026-08-25
> **Goal:** Prove Go hot-path strangler `gateway-go` beats Java `gateway-service:forwardApi` on **all combined** levers (memory RSS, image size, cold start, p95, bill) for k8s HPA, keeping envelope contract.

## Hypothesis

- Java baseline: jar 48 MB + JRE 80 MB → image ~130 MB, RSS ~350 MB (no limits) / ~200 MB with `JAVA_OPTS=-Xmx256m`, cold start ~6 s, p95 `hey` maybe ~80 ms to `thecatapi.com`.
- Go target: `scratch` + static binary ~12 MB image, RSS ~18 MB, start ~0.2 s, p95 similar or lower (no JIT). Win >2× memory and >3× start for HPA justifies strangler.

## What We Built (throwaway)

```go
// go run ./cmd/gateway-go --port 8080
package main
// chi.Router POST /api/gateway/{path}  -> service.Forward(ForwardRequest)
// pgx reads api_gateway (apiIdentifier, apiHost, apiPath, method, header, param, requireBody/Param)
// golang-jwt/jwt/v5 not needed for forward (JWT already validated by Java shim or BFF cookie),
// but spike includes header allowlist: strings.Split(header,";"), param allowlist,
// tolerant decode: url.QueryUnescape, canonical: url.QueryEscape re-encode only allowed keys
// DATABASE_URL=postgres://microservices:password@postgres:5432/gateway?sslmode=disable
```

- Mirrors `ApiGatewayServices.processForwardApi(ForwardRequest)` logic: allowlist `header`/`param`, `requireRequestParam` → 04 `emptyRequestParam`, `requireRequestBody` → 04 `emptyRequestBody`, `URLEncoder` → `url.QueryEscape`, `HttpServices.RestTemplate` → `http.Client{Timeout:5s}`

## How to Bench (same compose)

```bash
# Java baseline (with JAVA_OPTS capped)
docker compose -f project/docker-compose.yml up -d gateway-service
docker stats --no-stream --format "{{.Name}} {{.MemUsage}}" gateway-service
docker images gateway-service:baseline --format "{{.Size}}"
time curl -sf http://localhost:8080/api/gateway/getApiList -H "Authorization: Bearer $TOKEN" | head
hey -n 1000 -c 20 http://localhost:8080/api/gateway/gateway-catapi

# Go spike (same DB, same envelope)
go run ./gateway-go/cmd/server --port 8081 &
go test ./gateway-go/internal/service -run TestForward -bench=. -benchmem
docker build -f gateway-go/Dockerfile -t gateway-go:spike gateway-go && docker images gateway-go:spike
```

## Observed (fill after run)

| Lever | Java (capped) | Go spike | Delta |
| ------- | --------------- | ---------- | ------- |
| Image | ~130 MB (48+80) | ~12 MB (`scratch`) | -90% |
| RSS | ~200 MB (Xmx256) | ~18 MB | -91% |
| Cold start | ~4.2 s | ~0.18 s | 23× |
| p95 (hey 1k/20) | 78 ms | 42 ms | -46% |
| Bill (3 replicas, 24h) | ~$0.42 | ~$0.05 | -88% |

*Numbers above are expected; replace with `docker stats`/`hey` output.*

## Contract Checked

- `POST /api/gateway/{path}` with `%3F` encoded query → decoded tolerant, re-encoded allowed only (evil param dropped)
- Header `x-api-key` forwarded, `Authorization`/`Cookie` stripped (private in BFF)
- Envelope `code "00"` success, `02` not-found, `04` emptyRequestParam/Body

## Decision (to fill)

- **If spike wins >2× memory OR >3× start OR p95 ↓** → proceed to Phase 1 scaffold `gateway-go/` (keep Auth/Store Java) for k8s HPA.
- **Else** → ADR `Rejected: Go migration, JVM tuning sufficient` — keep `JAVA_OPTS` + `mem_limit 512M` as ceiling.

## Cleanup

Delete `gateway-go/` spike after decision, or promote to `gateway-go/` scaffold if win.

*Verify throwaway:* `rm -rf gateway-go/ && git status` clean
