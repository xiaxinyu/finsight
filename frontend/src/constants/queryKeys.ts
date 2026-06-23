/** Shared React Query keys for heavy analytics endpoints (v2.0.0). */
export const QUERY_KEYS = {
  financialProfile: ['financial-profile'] as const,
  advisorRecommendations: ['advisor-recommendations'] as const,
  annualOutlook: (year: number, scenario: string, payloadHash: string) =>
    ['annual-outlook', year, scenario, payloadHash] as const,
  cashRiskCalendar: (year: number, scenario: string) =>
    ['cash-risk-calendar', year, scenario] as const,
  forecastBacktest: ['forecast-backtest'] as const,
  profileHistory: (dimensionId: string, from: string, to: string) =>
    ['profile-history', dimensionId, from, to] as const,
} as const

/** 10 minutes — aligns with backend analytics TTL cache. */
export const ANALYTICS_STALE_MS = 600_000

/** Keep warm data in memory for 30 minutes after last use. */
export const ANALYTICS_GC_MS = 1_800_000
