export type DeltaTone = 'adverse' | 'favorable' | 'neutral'

export type ForecastKind = 'actual' | 'forecast' | 'budget' | 'band'

export function deltaTone(value: number, expenseContext = true): DeltaTone {
  const n = Number(value) || 0
  if (n === 0) return 'neutral'
  if (expenseContext) return n > 0 ? 'adverse' : 'favorable'
  return n > 0 ? 'favorable' : 'adverse'
}

export function deltaToneClass(tone: DeltaTone): string {
  if (tone === 'adverse') return 'fs-table-cell-delta--adverse'
  if (tone === 'favorable') return 'fs-table-cell-delta--favorable'
  return 'fs-table-cell-delta--neutral'
}

export function formatDeltaPercent(value: number, amount?: number | null): string {
  const pct = Number(value) || 0
  const pctText = `${pct >= 0 ? '+' : ''}${pct.toFixed(1)}%`
  if (amount == null || Number.isNaN(Number(amount))) return pctText
  const amt = Number(amount)
  const amtPrefix = amt >= 0 ? '+' : ''
  const amtText = `${amtPrefix}¥${Math.abs(amt).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`
  return `${pctText} (${amtText})`
}

export function resolveForecastKind(row: Record<string, unknown>): ForecastKind | null {
  if (row.actual === true) return 'actual'
  if (row.forecast === true) return 'forecast'
  if (row.budget === true) return 'budget'
  return null
}

export function budgetGap(expense: number, budgetTarget?: number | null): number | null {
  if (budgetTarget == null || Number.isNaN(Number(budgetTarget))) return null
  return Number(budgetTarget) - Number(expense || 0)
}
