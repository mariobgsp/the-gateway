# CLAUDE.md — The Gateway

> **Project:** API Gateway management UI — Next.js 15 BFF + Go backend (big-bang port of Spring Boot 3.1; `gateway-service/` retained until cutover)
> **Workflow:** `team-pr` (PR-based, protect `main`), Conventional Commits, GitHub Actions
> **Domain:** BffGateway, GatewayForward, ForwardRequest, UpstreamPort

## Commands (verify gates)

| Gate | Command |
| ------ | --------- |
| lint | `cd frontend && npm run lint` |
| typecheck | `cd frontend && npm run typecheck` (`tsc --noEmit`) |
| test (frontend) | `cd frontend && npm test` (Vitest 15 tests) |
| test (backend) | `cd gateway-go && go test ./...` |
| build (frontend) | `cd frontend && npm run build` |
| build (backend) | `cd gateway-go && go build ./...` |
| e2e | `cd frontend && npm run test:e2e` (Playwright, auto-starts H2 + dev servers) |
| import-boundaries | `test -f specs/import-boundaries.json && bash scripts/check-import-boundaries.sh` |

## Architecture

- **BFF Gateway** (`frontend/src/lib/bffGateway.ts` deep Module): hides `getSessionToken→401`, `callBackend`+20s Abort, `envelopeError` mapping; adapters in `frontend/src/app/gw/**`
- **GatewayForward** (`gateway-go/internal/service/forward.go`): typed `ForwardRequest{PathName, QueryParams, Headers, Body}`; `ParseForwardPath` tolerant decode (`?foo→""`, last-wins, no double-decode), canonical `JavaURLEncode` only in `BuildForwardURL`
- **UpstreamPort**: internal to `GatewayForward` (`net/http` client, 5s dial / 30s timeout), not exposed until 2 adapters justified

## Conventions

- JWT `httpOnly`/`SameSite=Lax`/`Secure` (`COOKIE_SECURE`/`NODE_ENV`), never `localStorage`; envelope `code "00"` success
- `H2` `local` profile + `data.sql` for tests (Local-substitutable)
- Reuse before new dep; stdlib/platform first; `ponytail:` ceiling comments for deliberate limits
- Commit: Conventional Commits (`feat:`, `fix:`) — see `CONVENTIONS.md`

## Specs Layout

- `specs/tech-architecture/tech-stack.md` (ubiquitous terms) ✓
- `specs/PLAN-AUDIT_LATEST.md` (audit verdict) ✓
- `specs/product/SCOPE_LATEST.yaml` (next: `scope-work`)
- `specs/epics/` + `specs/adr/` + `state.yaml` (to be bootstrapped via `seed-conventions`)

## Hard Gates (from audit)

- **BffGateway**: unauthenticated → `401` `{code:"98"}`; envelope `code "00"` mapping; `sanitizeHeaders`/`sanitizeParams` private in BFF
- **GatewayForward**: `ForwardRequest` contract, tolerant query, single `URLEncoder` place

## Workflow

- `team-pr`: branch from `main` → PR → review → merge; protected `main`
- CI: GitHub Actions (to be wired via `wire-ci`)
- Backend lint: `cd gateway-go && go vet ./...`
