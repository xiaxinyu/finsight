import { test, expect } from '@playwright/test'

const VIEWPORTS = [
  { width: 360, height: 800 },
  { width: 390, height: 844 },
  { width: 768, height: 1024 },
  { width: 1024, height: 768 },
  { width: 1440, height: 900 },
] as const

async function assertNoPageOverflow(page: import('@playwright/test').Page) {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return doc.scrollWidth > doc.clientWidth + 1
  })
  expect(overflow).toBe(false)
}

for (const viewport of VIEWPORTS) {
  test(`login layout ${viewport.width}x${viewport.height}`, async ({ page }) => {
    await page.setViewportSize(viewport)
    await page.goto('/app/login')
    await page.waitForSelector('.fs-login-page, form, .ant-form', { timeout: 15_000 })
    await assertNoPageOverflow(page)
  })
}

test('profile page with mocked APIs', async ({ page }) => {
  await page.route('**/api/v1/features', async (route) => {
    await route.fulfill({
      json: {
        profile: true,
        advisor: true,
        forecast: true,
        merchantMining: true,
        advisorLocalAi: false,
        planningPersist: false,
      },
    })
  })
  await page.route('**/api/v1/analytics/profile', async (route) => {
    await route.fulfill({
      json: {
        ok: true,
        data: {
          overallScore: 72,
          userType: 'balanced',
          userTypeExplanation: 'Mock profile',
          asOf: '2026-06-23',
          dimensions: [
            { id: 'data_trust', score: 80, level: 'strong', summary: 'ok', reason: 'ok', evidence: [], actions: [] },
          ],
          metricsGate: { ok: true, gateEnabled: false },
          metricsSource: 'fin_metric_monthly',
        },
      },
    })
  })
  await page.route('**/api/v1/advisor/recommendations', async (route) => {
    await route.fulfill({ json: { ok: true, data: [] } })
  })
  await page.route('**/api/v1/cards/list', async (route) => {
    await route.fulfill({ json: { ok: true, data: [] } })
  })

  for (const viewport of VIEWPORTS) {
    await page.setViewportSize(viewport)
    await page.goto('/app/profile')
    await page.waitForSelector('.fs-data-page--profile', { timeout: 15_000 })
    await assertNoPageOverflow(page)
  }
})
