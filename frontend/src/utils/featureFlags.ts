import type { FeatureFlags } from '../api/features'
import type { FsMenuItem } from '../config/menuConfig'

const FORECAST_REPORT_PATHS = new Set([
  '/reports/annual-outlook',
  '/reports/trend-changes',
  '/reports/cash-risk',
])

const MERCHANT_REPORT_PATHS = new Set([
  '/reports/subscriptions',
  '/reports/merchant-concentration',
  '/reports/merchant-drift',
])

function isMenuItemVisible(item: FsMenuItem, flags: FeatureFlags): boolean {
  if (!item.path) {
    return true
  }
  if (item.path === '/profile') {
    return flags.profile
  }
  if (FORECAST_REPORT_PATHS.has(item.path)) {
    return flags.forecast
  }
  if (MERCHANT_REPORT_PATHS.has(item.path)) {
    return flags.merchantMining
  }
  return true
}

export function filterMenuByFeatures(items: FsMenuItem[], flags: FeatureFlags): FsMenuItem[] {
  const out: FsMenuItem[] = []
  for (const item of items) {
    if (item.children?.length) {
      const children = filterMenuByFeatures(item.children, flags)
      if (children.length === 0) {
        continue
      }
      out.push({ ...item, children })
      continue
    }
    if (isMenuItemVisible(item, flags)) {
      out.push(item)
    }
  }
  return out
}
