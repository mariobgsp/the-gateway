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
      command: `export JAVA_HOME="\${JAVA_HOME:-\$HOME/.local/jdks/jdk17}" && cd ../gateway-service && ./mvnw -q spring-boot:run -Dspring-boot.run.profiles=local`,
      url: `${BACKEND_URL}/api/gateway/getApiList`,
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
