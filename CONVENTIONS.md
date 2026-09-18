# CONVENTIONS.md — The Gateway

## Git workflow

- `main` is protected.
- Branch from `main` using `feat/`, `fix/`, `refactor/`, or `chore/`.
- Merge through a reviewed pull request with green CI.
- Prefer separate, reversible PRs for independent refactors.

## Commits

Use `<type>(<scope>): <imperative subject>` with a 72-character subject limit. Valid types include `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, and `ci`.

## Code style

- TypeScript: strict mode, existing Next.js/React patterns, no unnecessary abstractions.
- Go: `gofmt`, `go vet`, small adapters, service logic behind typed seams.
- Reuse before a new dependency; stdlib/platform first.
- Do not store JWTs in browser storage.
- Keep response codes and route contracts centralized.

## Required gates

```bash
cd frontend && npm run lint && npm run typecheck && npm test && npm run build
cd gateway-go && go build ./... && go vet ./... && go test ./...
cd frontend && npm run test:e2e
```

## Domain terms

See `specs/tech-architecture/tech-stack.md` for BffGateway, GatewayForward, ForwardRequest, ApiGateway, and StoreAccount.
