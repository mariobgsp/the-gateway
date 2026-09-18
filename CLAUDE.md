# CLAUDE.md — The Gateway

> **Project:** API Gateway management UI — Next.js 15 BFF + Go backend (`gateway-go`, chi + pgx)
> **Workflow:** PR-based, protected `main`, Conventional Commits
> **Domain:** BffGateway, GatewayForward, ForwardRequest, ApiGateway, StoreAccount

## Verification

| Gate | Command |
| --- | --- |
| frontend lint | `cd frontend && npm run lint` |
| frontend typecheck | `cd frontend && npm run typecheck` |
| frontend test | `cd frontend && npm test` |
| frontend build | `cd frontend && npm run build` |
| backend build | `cd gateway-go && go build ./...` |
| backend vet | `cd gateway-go && go vet ./...` |
| backend test | `cd gateway-go && go test ./...` |
| e2e | `cd frontend && npm run test:e2e` |

## Architecture

- **BffGateway:** `frontend/src/lib/bffGateway.ts` owns session → `401`, backend fetch + 20s abort, private sanitization, execute mapping, and envelope → `NextResponse` translation.
- **BFF adapters:** `frontend/src/app/gw/[...path]/route.ts` dispatches the declarative table in `frontend/src/lib/gwRoutes.ts`.
- **GatewayForward:** `gateway-go/internal/service/forward.go` accepts typed `ForwardRequest`, applies persisted allowlists, encodes query parameters once, and calls the upstream client.
- **Response contract:** `gateway-go/internal/api` owns envelope codes and error-to-response mapping.
- **Persistence:** `gateway-go/internal/store` is split by entity and uses positional pgx collection helpers for repeated row mapping.

## Hard gates

- Unauthenticated BFF calls return `401` with `{code:"98"}`.
- Envelope `code "00"` is success; all other codes are errors.
- JWT stays in an `httpOnly`/`SameSite=Lax`/secure-in-production cookie, never `localStorage`.
- Forward query parsing remains tolerant: valueless keys become empty values, duplicates keep the last value, and values are not decoded twice.
- `/health` remains `{"status":"ok"}` for Kubernetes probes.

## Conventions

- Reuse before new dependency; stdlib/platform first.
- Keep handlers as adapters and domain behavior in services.
- Keep PostgreSQL runtime data in ignored `project/db-data/`; seed fresh databases from `project/pg-init-scripts/gateway.sql`.
- Branches use `refactor/`, `feat/`, `fix/`, `chore/`; commits use Conventional Commits.
