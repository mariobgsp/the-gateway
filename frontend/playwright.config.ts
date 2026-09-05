import { defineConfig, devices } from "@playwright/test";

const BACKEND_URL = "http://localhost:8080";
const FRONTEND_URL = "http://localhost:3001";

export default defineConfig({
  testDir: "./tests/e2e",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: "html",
  timeout: 60000,
  use: {
    baseURL: FRONTEND_URL,
    trace: "on-first-retry",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: [
    {
      // Go backend. Requires Postgres with the gateway db seeded once:
      //   docker compose -f ../project/docker-compose.yml up -d postgres
      //   psql $DATABASE_URL -f ../project/pg-init-scripts/gateway.sql
      command: `cd ../gateway-go && DATABASE_URL="\${DATABASE_URL:-postgres://microservices:password@localhost:5432/gateway?sslmode=disable}" PORT=8080 go run ./cmd/server`,
      url: `${BACKEND_URL}/health`,
      reuseExistingServer: !process.env.CI,
      timeout: 300000,
    },
    {
      command: `BACKEND_API_URL=${BACKEND_URL} COOKIE_SECURE=false npm run build && BACKEND_API_URL=${BACKEND_URL} COOKIE_SECURE=false npm run start -- -p 3001`,
      url: FRONTEND_URL,
      reuseExistingServer: !process.env.CI,
      timeout: 300000,
    },
  ],
});
