import { test, expect } from '@playwright/test'

/**
 * 登录页冒烟测试
 * 验证前端应用能正常渲染登录页，文档与实现保持一致。
 */
test.describe('Login Page', () => {
  test('should display login form', async ({ page }) => {
    await page.goto('/login')

    await expect(page.locator('input[placeholder*="用户名"], input[name="username"]')).toBeVisible()
    await expect(page.locator('input[type="password"]')).toBeVisible()
    await expect(page.locator('button:has-text("登录")')).toBeVisible()
  })

  test('should redirect to login when accessing protected route without token', async ({ page }) => {
    await page.goto('/')

    await expect(page).toHaveURL(/\/login$/)
  })
})
