# The Gateway — Frontend

Next.js (App Router) frontend for the Gateway API management platform.

## Stack
- Next.js 15 (App Router), React 19, TypeScript
- BFF route handlers under `/gw/*` proxy all calls to the Spring Boot backend
- JWT stored in an httpOnly cookie; protected pages guarded by `src/middleware.ts`
- Vitest (unit) + Playwright (full-stack e2e)

## Scripts
```bash
npm run dev        # dev server on :3000
npm run build      # production build (standalone output)
npm run start      # serve production build
npm run lint       # eslint
npm run typecheck  # tsc --noEmit
npm test           # vitest unit tests
npm run test:e2e   # playwright full-stack tests (starts backend + frontend)
```

## Configuration
`BACKEND_API_URL` (default `http://localhost:8080`) — see `.env.example`.
