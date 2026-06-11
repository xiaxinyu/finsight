import { type Dayjs } from 'dayjs'
import type { ReportPoint } from '../api/report'
import { MONTH_NAMES, formatMoney } from './format'
import type { InsightBullet } from '../components/InsightPanel'

const MONTH_INDEX: Record<string, number> = {
  january: 0, jan: 0,
  february: 1, feb: 1,
  march: 2, mar: 2,
  april: 3, apr: 3,
  may: 4,
  june: 5, jun: 5,
  july: 6, jul: 6,
  august: 7, aug: 7,
  september: 8, sep: 8,
  october: 9, oct: 9,
  november: 10, nov: 10,
  december: 11, dec: 11,
}

export function monthIndexFromKey(key: string): number | null {
  const raw = String(key ?? '').trim().toLowerCase()
  if (!raw) return null
  if (/^\d+$/.test(raw)) {
    const n = parseInt(raw, 10)
    if (n >= 1 && n <= 12) return n - 1
    if (n >= 0 && n <= 11) return n
    return null
  }
  return MONTH_INDEX[raw] ?? null
}

/** Months (0–11) touched by an inclusive period. */
export function monthsInPeriod(period: [Dayjs, Dayjs]): number[] {
  const start = period[0].startOf('month')
  const end = period[1].endOf('month')
  const out: number[] = []
  let cur = start
  while (cur.isBefore(end) || cur.isSame(end, 'month')) {
    out.push(cur.month())
    cur = cur.add(1, 'month')
  }
  return out
}

export function sumReportPoints(points: ReportPoint[]): number {
  return points.reduce((s, p) => s + (Number(p.value) || 0), 0)
}

export function mapReportByMonth(points: ReportPoint[]): Map<number, number> {
  const m = new Map<number, number>()
  for (const p of points) {
    const idx = monthIndexFromKey(p.key)
    if (idx == null) continue
    m.set(idx, (m.get(idx) ?? 0) + (Number(p.value) || 0))
  }
  return m
}

export type MonthlyCashflowRow = {
  month: string
  monthIndex: number
  income: number
  expense: number
  surplus: number
}

export function buildMonthlyCashflow(
  incomePts: ReportPoint[],
  expensePts: ReportPoint[],
  period: [Dayjs, Dayjs],
): MonthlyCashflowRow[] {
  const incMap = mapReportByMonth(incomePts)
  const expMap = mapReportByMonth(expensePts)
  return monthsInPeriod(period).map((mi) => {
    const income = incMap.get(mi) ?? 0
    const expense = expMap.get(mi) ?? 0
    return {
      month: MONTH_NAMES[mi],
      monthIndex: mi,
      income,
      expense,
      surplus: income - expense,
    }
  })
}

export type CategoryRow = { key: string; value: number; share: number; code?: string; level1Code?: string; level1Name?: string }

export function enrichCategoryRows(rows: ReportPoint[]): CategoryRow[] {
  const filtered = rows.filter((r) => r.key && Number.isFinite(r.value) && r.value > 0)
  const total = filtered.reduce((s, r) => s + r.value, 0)
  return filtered
    .map((r) => ({
      key: r.key,
      value: r.value,
      share: total > 0 ? (r.value / total) * 100 : 0,
      code: r.code,
      level1Code: r.level1Code,
      level1Name: r.level1Name,
    }))
    .sort((a, b) => b.value - a.value)
}

/** Roll up leaf categories to level-1 parents for clearer charts. */
export function rollupToLevel1(rows: ReportPoint[]): ReportPoint[] {
  const map = new Map<string, number>()
  for (const r of rows) {
    if (!Number.isFinite(r.value) || r.value <= 0) continue
    const label = r.level1Name || r.level1Code || r.key
    if (!label) continue
    map.set(label, (map.get(label) ?? 0) + r.value)
  }
  return [...map.entries()]
    .map(([key, value]) => ({ key, value }))
    .sort((a, b) => b.value - a.value)
}

const WEEKDAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

export function buildWeekdaySeries(points: ReportPoint[]): { label: string; value: number }[] {
  const vals = [0, 0, 0, 0, 0, 0, 0]
  for (const p of points) {
    const i = parseInt(p.key, 10)
    if (i >= 0 && i < 7) vals[i] = p.value
  }
  return WEEKDAY_LABELS.map((label, i) => ({ label, value: vals[i] }))
}

function pct(n: number) {
  return `${(Number(n) || 0).toFixed(1)}%`
}

export function insightsCashflow(rows: MonthlyCashflowRow[], periodLabel: string): InsightBullet[] {
  if (!rows.length) {
    return [{ text: 'No cash flow in this period. Widen the date range or clear card/category filters.', warn: true }]
  }
  const income = rows.reduce((s, r) => s + r.income, 0)
  const expense = rows.reduce((s, r) => s + r.expense, 0)
  const net = income - expense
  const savings = income > 0 ? (net / income) * 100 : 0
  const deficitMonths = rows.filter((r) => r.surplus < 0).length
  const best = [...rows].sort((a, b) => b.surplus - a.surplus)[0]
  const worst = [...rows].sort((a, b) => a.surplus - b.surplus)[0]
  const bullets: InsightBullet[] = [
    {
      text: `${periodLabel}: Income ${formatMoney(income)}, expense ${formatMoney(expense)}, net ${formatMoney(net)} (${pct(savings)} savings rate).`,
    },
  ]
  if (deficitMonths > 0) {
    bullets.push({
      text: `${deficitMonths} of ${rows.length} month(s) ran a deficit. Worst: ${worst.month} (${formatMoney(worst.surplus)}).`,
      warn: true,
    })
  }
  if (best.surplus > 0 && best.month !== worst.month) {
    bullets.push({ text: `Strongest month: ${best.month} with ${formatMoney(best.surplus)} surplus.` })
  }
  if (net < 0) bullets.push({ text: 'Period net is negative — review recurring bills and top categories in Budget vs Actual.', warn: true })
  else if (savings >= 20) bullets.push({ text: 'Savings rate is healthy for this period.' })
  return bullets
}

export function insightsBudget(
  lines: Record<string, unknown>[],
  totalActual: number,
  totalLimit: number,
): InsightBullet[] {
  if (totalLimit <= 0) {
    return [{ text: 'No monthly budget configured. Set limits in Planning to unlock utilization analysis.', warn: true }]
  }
  const util = (totalActual / totalLimit) * 100
  const over = lines.filter((l) => Number(l.actual || 0) > Number(l.limit || 0))
  const bullets: InsightBullet[] = [
    { text: `MTD utilization ${pct(util)} — ${formatMoney(totalActual)} of ${formatMoney(totalLimit)} budget.` },
  ]
  if (util >= 100) bullets.push({ text: 'Budget exhausted for the month. Pause discretionary spend or raise limits.', warn: true })
  else if (util >= 80) bullets.push({ text: 'Above 80% of budget — only essential purchases until month-end.', warn: true })
  if (over.length > 0) {
    bullets.push({
      text: `Over budget: ${over.map((l) => String(l.bucketKey || l.categoryCode)).join(', ')}.`,
      warn: true,
    })
  }
  return bullets
}

export function insightsFixedVariable(
  buckets: Record<string, number>,
  weekday: { label: string; value: number }[],
  periodLabel: string,
): InsightBullet[] {
  const fixed = buckets.fixed ?? 0
  const variable = buckets.variable ?? buckets.life ?? 0
  const bullets: InsightBullet[] = [
    { text: `${periodLabel} structure: fixed ${fixed}% · variable ${variable}% of classified spend.` },
  ]
  if (fixed >= 45) bullets.push({ text: 'Fixed burden is high (>45%). Consider renegotiating rent, utilities, or subscriptions.', warn: true })
  const weekend = (weekday[5]?.value ?? 0) + (weekday[6]?.value ?? 0)
  const weekdayTotal = weekday.reduce((s, d) => s + d.value, 0)
  if (weekdayTotal > 0 && weekend / weekdayTotal >= 0.35) {
    bullets.push({ text: `Weekend spend is ${pct((weekend / weekdayTotal) * 100)} of the week — discretionary pattern detected.` })
  }
  return bullets
}

export function insightsSpendingDrift(
  periodA: ReportPoint[],
  periodB: ReportPoint[],
  labelA: string,
  labelB: string,
): InsightBullet[] {
  const totalA = sumReportPoints(periodA)
  const totalB = sumReportPoints(periodB)
  if (totalA <= 0 && totalB <= 0) {
    return [{ text: 'No expense data in either comparison period.', warn: true }]
  }
  const delta = totalB - totalA
  const deltaPct = totalA > 0 ? (delta / totalA) * 100 : 0
  const mapA = new Map(periodA.map((p) => [p.key, p.value]))
  const mapB = new Map(periodB.map((p) => [p.key, p.value]))
  const keys = new Set([...mapA.keys(), ...mapB.keys()])
  const movers = [...keys].map((key) => ({
    key,
    delta: (mapB.get(key) ?? 0) - (mapA.get(key) ?? 0),
  })).sort((a, b) => Math.abs(b.delta) - Math.abs(a.delta))
  const topUp = movers.filter((m) => m.delta > 0)[0]
  const topDown = movers.filter((m) => m.delta < 0)[0]
  const bullets: InsightBullet[] = [
    {
      text: `Total expense ${formatMoney(totalA)} (${labelA}) → ${formatMoney(totalB)} (${labelB}): ${delta >= 0 ? '+' : ''}${formatMoney(delta)} (${pct(deltaPct)}).`,
    },
  ]
  if (Math.abs(deltaPct) >= 15) bullets.push({ text: 'Material shift between periods — inspect category movers below.', warn: true })
  if (topUp) bullets.push({ text: `Largest increase: "${topUp.key}" (+${formatMoney(topUp.delta)}).`, warn: topUp.delta > totalA * 0.1 })
  if (topDown) bullets.push({ text: `Largest decrease: "${topDown.key}" (${formatMoney(topDown.delta)}).` })
  return bullets
}

export function insightsCategoryRows(rows: CategoryRow[], periodLabel: string): InsightBullet[] {
  if (!rows.length) {
    return [{ text: 'No category spend for filters. Broaden the period or clear category filter.', warn: true }]
  }
  const total = rows.reduce((s, r) => s + r.value, 0)
  const top = rows[0]
  const top3Share = rows.slice(0, 3).reduce((s, r) => s + r.share, 0)
  const bullets: InsightBullet[] = [
    { text: `"${top.key}" leads at ${pct(top.share)} (${formatMoney(top.value)}) of ${formatMoney(total)}.` },
  ]
  if (top3Share >= 60) {
    bullets.push({ text: `Top 3 categories = ${pct(top3Share)} of spend. High concentration risk.`, warn: true })
  }
  if (rows.length >= 5) {
    const tail = rows.slice(4).reduce((s, r) => s + r.value, 0)
    bullets.push({ text: `${rows.length - 4} smaller categories combined: ${formatMoney(tail)} (${pct((tail / total) * 100)}).` })
  }
  bullets.push({ text: `Period: ${periodLabel}. Click a category row to drill into transactions.` })
  return bullets
}

export function monthSeriesFromPoints(points: ReportPoint[], period: [Dayjs, Dayjs]) {
  const map = mapReportByMonth(points)
  return monthsInPeriod(period).map((mi) => ({
    label: MONTH_NAMES[mi],
    value: map.get(mi) ?? 0,
  }))
}
