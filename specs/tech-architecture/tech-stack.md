# Tech Stack — The Gateway

## Contexts

| Context | Purpose | Stack |
| --- | --- | --- |
| **BFF Gateway** | Next.js server routes own the session cookie and proxy browser calls | Next.js 15, React 19, TypeScript, `fetch`, `AbortController` |
| **Gateway Service** | Authenticated API/store management and upstream forwarding | Go, chi, pgx, PostgreSQL, JWT, bcrypt |
| **Upstream** | Configured third-party APIs | Go `net/http` with 5s dial / 30s client timeout |
| **Infra** | Local and deployed runtime | Docker Compose, Kubernetes, PostgreSQL 15 |

## Module map

```text
frontend/src/
  app/gw/[...path]/route.ts       catch-all HTTP adapter
  lib/gwRoutes.ts                 declarative BFF route table
  lib/bffGateway.ts               auth, timeout, backend call, envelope mapping
  lib/api.ts                      browser API client
  components/ui.tsx               shared page primitives
  components/EntityModal.tsx      API/store creation forms
  components/TryItModal.tsx       upstream execution form
  hooks/                          shared async and toast state

gateway-go/internal/
  api/                            response envelope, codes, service errors
  handler/                        router, HTTP adapters, auth middleware
  service/                        API, account, auth, forward workflows
  store/                          PostgreSQL connection and entity queries
```

## Domain language

- **BffGateway** — the deep server-side proxy that hides session lookup, unauthorized responses, backend fetches, timeout handling, and envelope translation.
- **GatewayForward** — the typed Go forward pipeline: resolve configuration, allowlist headers and parameters, encode the URL once, enforce request gates, and invoke upstream.
- **ForwardRequest** — the typed handler-to-forwarder seam: `PathName`, `QueryParams`, `Headers`, and `Body`.
- **ApiGateway** — persisted upstream configuration: identifier, host, path, method, header/parameter allowlists, and request requirements.
- **StoreAccount** — a user-owned client id and secret key.

## Invariants

- JWTs live only in an `httpOnly`, `SameSite=Lax`, secure-in-production cookie; never in `localStorage`.
- Envelope code `"00"` means success. Codes `"01"` through `"06"`, `"98"`, and `"99"` identify service faults.
- BFF sanitization is private to `BffGateway`; backend allowlists are enforced again from persisted configuration.
- Query parsing is tolerant (`?foo` becomes `foo=""`, duplicate keys are last-wins); canonical Java-compatible encoding happens once in `service.BuildForwardURL`.
- PostgreSQL runtime data under `project/db-data/` is local-only and ignored by git.
- Reuse stdlib/platform facilities before adding dependencies.

## Verification

```bash
cd gateway-go && go build ./... && go vet ./... && go test ./...
cd frontend && npm run lint && npm run typecheck && npm test && npm run build
cd frontend && npm run test:e2e
```
