import { test, expect } from '@playwright/test';

test.describe('The Gateway App E2E Tests', () => {
  test('should login successfully and navigate to Home', async ({ page }) => {
    // Navigate to root
    await page.goto('/');

    // Check title is visible
    await expect(page.locator('h1').filter({ hasText: 'The Gateway' })).toBeVisible();

    // Fill login form
    await page.fill('input#username', 'testuser');
    await page.fill('input#password', 'password123');

    // Click login
    await page.click('button:has-text("login")');

    // Should navigate to home
    await expect(page).toHaveURL(/.*\/home/);

    // Check "API List" tab is active by checking the presence of the table header
    await expect(page.locator('div').filter({ hasText: /^API Name$/ })).toBeVisible();
    
    // Verify one of the APIs is rendered
    await expect(page.locator('div').filter({ hasText: 'Gateway-Cat-Api' }).first()).toBeVisible();
  });

  test('should toggle between API List and Store Account tabs', async ({ page }) => {
    // Mock login by navigating directly to home (since no auth guard is implemented yet)
    await page.goto('/home');

    // Verify API List is active by default
    await expect(page.locator('div').filter({ hasText: /^API Name$/ })).toBeVisible();

    // Click on Store Account tab
    await page.click('button:has-text("Store Account")');

    // Verify Store Account tab is active
    await expect(page.locator('div').filter({ hasText: /^Store Name$/ })).toBeVisible();
    
    // Verify store content
    await expect(page.locator('div').filter({ hasText: 'Store-Example-Satu' }).first()).toBeVisible();
  });

  test('should navigate to API Detail Page', async ({ page }) => {
    await page.goto('/home');

    // Click on Gateway-Cat-Api row
    await page.getByText('Gateway-Cat-Api').click();

    // Should navigate to detail page
    await expect(page).toHaveURL(/.*\/api\/4/);

    // Verify details are rendered
    await expect(page.getByText('https://api.thecatapi.com')).toBeVisible();
    await expect(page.getByText('/v1/images/search')).toBeVisible();
    
    // Verify "Try it Now!" button is visible
    await expect(page.getByRole('button', { name: 'Try it Now!' })).toBeVisible();
  });
});
