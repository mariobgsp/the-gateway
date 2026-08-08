# The Gateway

Gateway services example with a **Next.js (App Router) React TypeScript frontend** and a **Spring Boot backend**.

## Design Reference
[Figma Link](https://www.figma.com/design/8IKh4NrzxsXJajEt8jL32x/the-gateway?node-id=1-6344&t=RuiBqghCN5cc1GHp-1)

## Architecture

```
Browser ──▶ Next.js frontend (BFF route handlers /gw/*)
                    │  httpOnly session cookie (JWT never exposed to JS)
                    ▼
              Spring Boot gateway-service (JWT auth)
                    │
                    ▼
              PostgreSQL + upstream APIs (e.g. thecatapi.com)
```

All backend calls are proxied server-side through Next.js route handlers. The JWT is stored in an
`httpOnly`, `SameSite=Lax`, `Secure` (production) cookie — never in `localStorage` — and protected
routes are guarded by an auth middleware.

## Running with Docker

1. Ensure you have Docker and Docker Compose installed.
2. Navigate to the `project` directory:
   ```bash
   cd project
   ```
3. Build and start the containers:
   ```bash
   docker compose up -d --build
   ```

### Services
- **Frontend**: `http://localhost:3000`
- **Backend (Gateway Service)**: `http://localhost:8080`
- **Database (PostgreSQL)**: port `5432`

### Default credentials
- Username: `ario_test`
- Password: `password123`

## Running locally without Docker

### 1. Backend (uses in-memory H2 with seeded data — no PostgreSQL needed)
```bash
cd gateway-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
The `local` profile seeds users, API gateway configs, and store accounts from `data.sql`.

### 2. Frontend
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:3000`.

## Testing

### Backend unit tests (36 tests)
```bash
cd gateway-service
./mvnw test
```

### Frontend unit tests (Vitest)
```bash
cd frontend
npm test
```

### End-to-end tests (Playwright — full stack, real backend)
```bash
cd frontend
npx playwright install chromium
npm run test:e2e
```
The e2e suite starts both the backend (`local` profile, H2) and the Next.js dev server automatically.

### Lint / typecheck / build
```bash
cd frontend
npm run lint
npm run typecheck
npm run build
```

## Environment variables

| Variable           | Description                                        | Default               |
|--------------------|----------------------------------------------------|-----------------------|
| `BACKEND_API_URL`  | URL of the Spring Boot gateway-service            | `http://localhost:8080` |
| `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | PostgreSQL connection for the backend | overrides for docker |
