import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright E2E 配置
 * 默认对本地开发环境 http://localhost:5173 执行冒烟测试。
 * 运行前请确保前端 dev server 已启动（pnpm dev）。
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
})
