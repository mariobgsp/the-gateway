# CONVENTIONS.md — The Gateway

## Git Workflow: team-pr

- **Main branch:** `main` protected
- **Feature branch:** `feat/<slug>` or `fix/<slug>` from `main`
- **PR required:** 1 review, CI green (`lint`, `typecheck`, `test`, `build`), no direct push to `main`
- **Merge:** squash or merge commit; `Merge pull request` retained for audit
- **Branch naming:** `feat/`, `fix/`, `chore/`, `refactor/` prefixes

## Commit: Conventional Commits

- Format: `<type>(<scope>): <subject>` where `<type>` = `feat` | `fix` | `docs` | `refactor` | `test` | `chore` | `perf` | `ci`
- Subject imperative, no period, ≤72 chars
- Body optional, wrap 72
- Examples: `feat(bff): add BffGateway proxy`, `fix(gateway): tolerant decode for ?foo`
- Tooling: enforced via commit-msg hook (to be added) and PR title check

## Code Style & Gates

| Gate | Command | When |
| ------ | --------- | ------ |
| lint (frontend) | `cd frontend && npm run lint` | `audit-code` lint gate |
| typecheck (frontend) | `cd frontend && npm run typecheck` | `verify-work` mechanical |
| lint (backend) | `checkstyle` (to be wired) | `audit-code` lint gate |
| build (frontend) | `cd frontend && npm run build` | `verify-work` mechanical |
| build (backend) | `cd gateway-service && ./mvnw package` | `verify-work` |
| test (frontend) | `cd frontend && npm test` | `develop-tdd` red-green |
| test (backend) | `cd gateway-service && ./mvnw test` | `develop-tdd` |
| e2e | `cd frontend && npm run test:e2e` | `verify-work` full-stack |
| import-boundaries | `test -f specs/import-boundaries.json && bash scripts/check-import-boundaries.sh` | on module split/merge |

## Release

- Versioning: SemVer via tags; Conventional Commits drive changelog
- Integration: `release-branch` skill creates PR, verifies coverage gates, cleans worktree

## CI: GitHub Actions

- Platform: **GitHub Actions** (answer recorded 2026-08-25)
- Workflows: `.github/workflows/ci.yml` (lint→typecheck→test→build) to be wired via `wire-ci`
- Secrets: `BACKEND_API_URL`, `SPRING_DATASOURCE_*` for docker

## Specs Layout (bigpowers)

- `specs/product/SCOPE_LATEST.yaml` (scope-work)
- `specs/tech-architecture/tech-stack.md` ✓
- `specs/tech-architecture/adr/` (ADRs)
- `specs/epics/` (slice-tasks → plan-work)
- `specs/PLAN-AUDIT_LATEST.md` ✓
- `state.yaml` / `planning-status.yaml` (seed-conventions)

## Domain Language

See `specs/tech-architecture/tech-stack.md`: BffGateway, GatewayForward, ForwardRequest, UpstreamPort, ApiGateway, StoreAccount, SystemProperties

## E2E Port Note

- `frontend/playwright.config.ts` uses `3001` not `3000` to avoid clash with `vivante` dev server on `3000` (uid 100, cannot `fuser -k` without sudo). Original `3000` is canonical; `3001` is intentional workaround until reboot. `package-lock.json` noise reverted.

## Ponytail / Caveman

- Ponytail `full` ladder: YAGNI → reuse → stdlib → native → installed dep → one-liner → minimum code; `ponytail:` ceiling comments
- Caveman terse responses (drop articles, fragments OK), code unchanged
