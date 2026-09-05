import { test, expect, type Page } from "@playwright/test";

const USERNAME = "ario_test";
const PASSWORD = "password123";

async function login(page: Page): Promise<void> {
  await page.goto("/");
  await page.fill("input#username", USERNAME);
  await page.fill("input#password", PASSWORD);
  await page.click('button:has-text("Sign In")');
  await expect(page).toHaveURL(/\/home/);
  await expect(page.locator("div.table-row").first()).toBeVisible();
}

test.describe("Authentication", () => {
  test("should login successfully and land on home", async ({ page }) => {
    await login(page);
    await expect(page.locator("div.table-header")).toBeVisible();
    await expect(page.getByText("Gateway-Cat-Api").first()).toBeVisible();
  });

  test("should show error for invalid credentials and stay on login", async ({
    page,
  }) => {
    await page.goto("/");
    await page.fill("input#username", "nobody_" + Date.now());
    await page.fill("input#password", "wrong-password");
    await page.click('button:has-text("Sign In")');

    await expect(page.getByRole("alert")).toBeVisible();
    await expect(page).toHaveURL("/");
  });

  test("should redirect unauthenticated users from protected pages to login", async ({
    page,
  }) => {
    await page.goto("/home");
    await expect(page).toHaveURL("/");

    await page.goto("/api/gateway-catapi");
    await expect(page).toHaveURL("/");
  });

  test("should redirect authenticated users from login to home", async ({
    page,
  }) => {
    await login(page);
    await page.goto("/");
    await expect(page).toHaveURL(/\/home/);
  });

  test("should sign out and require login again", async ({ page }) => {
    await login(page);
    await page.click('button:has-text("Sign Out")');
    await expect(page).toHaveURL("/");
    await expect(page.locator("input#username")).toBeVisible();

    await page.goto("/home");
    await expect(page).toHaveURL("/");
  });
});

test.describe("API List", () => {
  test("should toggle between API List and Store Account tabs", async ({
    page,
  }) => {
    await login(page);

    await expect(
      page.locator("div.table-cell").filter({ hasText: "API Name" }),
    ).toBeVisible();

    await page.click('button:has-text("Store Account")');
    await expect(
      page.locator("div.table-cell").filter({ hasText: "Store Name" }),
    ).toBeVisible();
    await expect(page.getByText("Store-Example-Satu").first()).toBeVisible();

    await page.click('button:has-text("API List")');
    await expect(
      page.locator("div.table-cell").filter({ hasText: "API Name" }),
    ).toBeVisible();
  });

  test("should add, edit, and delete an API", async ({ page }) => {
    await login(page);

    const apiName = `E2E-API-${Date.now()}`;
    const apiPath = `e2e-api-${Date.now()}`;

    await page.click('button:has-text("+ Add API")');
    await page.fill(
      '.modal-box input[placeholder="e.g. My-Awesome-API"]',
      apiName,
    );
    await page.fill(
      '.modal-box input[placeholder="e.g. my-awesome-api"]',
      apiPath,
    );
    await page.click('.modal-box button:has-text("Add API")');

    await expect(page.getByText(apiName).first()).toBeVisible();

    await page.getByText(apiName).first().click();
    await expect(page).toHaveURL(new RegExp(`/api/${apiPath}`));
    await expect(
      page.locator('input[placeholder="https://api.example.com"]'),
    ).toHaveValue("https://api.thecatapi.com");

    const renamed = `${apiName}-renamed`;
    page.on("dialog", (dialog) => dialog.accept());
    await page.locator('input[placeholder="API name"]').fill(renamed);
    await page.click('button:has-text("Save Changes")');
    await expect(page.locator("div.toast-success")).toContainText(
      "saved successfully",
    );
    await expect(page.locator(".breadcrumb-current")).toHaveText(renamed);

    await page.click('button:has-text("Delete API")');
    await expect(page).toHaveURL(/\/home/);
    await expect(page.getByText(renamed)).toHaveCount(0);
  });

  test("should show detail page for seeded API", async ({ page }) => {
    await login(page);
    await page.getByText("Gateway-Cat-Api").first().click();

    await expect(page).toHaveURL(/\/api\/gateway-catapi/);
    await expect(
      page.locator('input[placeholder="https://api.example.com"]'),
    ).toHaveValue("https://api.thecatapi.com");
    await expect(page.locator('input[placeholder="/v1/endpoint"]')).toHaveValue(
      "/v1/images/search",
    );
    await expect(
      page.getByRole("button", { name: "Try it Now!" }),
    ).toBeVisible();
  });

  test("should execute API via Try It modal", async ({ page }) => {
    await login(page);
    await page.getByText("Gateway-Cat-Api").first().click();
    await page.click('button:has-text("Try it Now!")');

    await page.fill('.modal-box textarea[placeholder*="limit"]', "limit: 2");
    await page.click('button:has-text("Send Request")');

    await expect(page.locator(".modal-box pre")).not.toHaveText("");
    await expect(page.locator(".modal-box pre")).toContainText(
      /cdn2\.thecatapi\.com|Request failed|❌/,
    );
  });
});

test.describe("Store Account", () => {
  test("should add, edit, regenerate key, and delete a store", async ({
    page,
  }) => {
    page.on("dialog", (dialog) => dialog.accept());
    await login(page);
    await page.click('button:has-text("Store Account")');

    const storeName = `E2E-Store-${Date.now()}`;

    await page.click('button:has-text("+ Add Store")');
    await page.fill('.modal-box input[placeholder="e.g. My-Store"]', storeName);
    await page.click('.modal-box button:has-text("Add Store")');
    await expect(page.getByText(storeName).first()).toBeVisible();

    await page.getByText(storeName).first().click();
    await expect(page.locator(".breadcrumb-current")).toHaveText(storeName);

    const renamed = `${storeName}-renamed`;
    await page.locator('input[placeholder="My Store Name"]').fill(renamed);
    await page.click('button:has-text("Save Changes")');
    await expect(page.locator("div.toast-success")).toBeVisible();
    await expect(page.locator(".breadcrumb-current")).toHaveText(renamed);

    await page.click('button:has-text("API Keys")');
    const maskedKey = await page.locator("code").first().textContent();
    await page.click('button:has-text("+ Regenerate Key")');
    await expect(
      page.getByRole("button", { name: "+ Regenerate Key" }),
    ).toBeEnabled({ timeout: 20000 });
    await expect(page.locator("div.toast-success")).toContainText(
      "New API key generated!",
    );
    const newMaskedKey = await page.locator("code").first().textContent();
    expect(newMaskedKey).not.toBe(maskedKey);

    await page.click('button:has-text("Danger Zone")');
    await page.click('button:has-text("Delete Store")');
    await expect(page).toHaveURL(/\/home/);
    await page.click('button:has-text("Store Account")');
    await expect(page.getByText(renamed)).toHaveCount(0);
  });
});
