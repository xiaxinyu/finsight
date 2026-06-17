import type { EChartsOption } from 'echarts'
import type { ReportPoint } from '../api/report'
import { formatMoney } from './format'
import { formatPeriodPreview, type PeriodRange } from './periodPresets'

export type SpendingDriftRow = {
  key: string
  periodA: number
  periodB: number
  delta: number
  deltaPct: number
  monthlyA: number
  monthlyB: number
  monthlyDeltaPct: number
  shareOfShift: number
}

export type SpendingDriftKpi = {
  key: string
  label: string
  value: string
  hint?: string
  tone?: 'income' | 'expense' | 'neutral' | 'warn'
}

export type SpendingDriftInsight = {
  text: string
  warn?: boolean
}

export function periodDaySpan(range: PeriodRange): number {
  return range[1].diff(range[0], 'day') + 1
}

export function periodSpanLabel(days: number): string {
  if (days >= 360 && days <= 370) return '12 mo'
  if (days >= 28 && days <= 31) return '1 mo'
  const months = Math.max(1, Math.round(days / 30))
  return `${months} mo · ${days}d`
}

export function periodsComparable(spanA: number, spanB: number): boolean {
  if (spanA <= 0 || spanB <= 0) return false
  const ratio = Math.max(spanA, spanB) / Math.min(spanA, spanB)
  return ratio <= 1.12
}

export function alignedPriorYearPeriod(period: PeriodRange): PeriodRange {
  return [period[0].subtract(1, 'year'), period[1].subtract(1, 'year')]
}

function sumReportPoints(points: ReportPoint[]): number {
  return points.reduce((s, p) => s + (p.value ?? 0), 0)
}

function monthlyRate(total: number, days: number): number {
  if (days <= 0) return 0
  return (total / days) * 30.4375
}

export function buildSpendingDriftRows(ptsA: ReportPoint[], ptsB: ReportPoint[]): SpendingDriftRow[] {
  const mapA = new Map(ptsA.map((p) => [p.key, p.value]))
  const mapB = new Map(ptsB.map((p) => [p.key, p.value]))
  const keys = [...new Set([...mapA.keys(), ...mapB.keys()])]
  const totalShift = keys.reduce((s, key) => s + ((mapB.get(key) ?? 0) - (mapA.get(key) ?? 0)), 0)
  return keys.map((key) => {
    const periodA = mapA.get(key) ?? 0
    const periodB = mapB.get(key) ?? 0
    const delta = periodB - periodA
    return {
      key,
      periodA,
      periodB,
      delta,
      deltaPct: periodA > 0 ? (delta / periodA) * 100 : (periodB > 0 ? 100 : 0),
      monthlyA: periodA,
      monthlyB: periodB,
      monthlyDeltaPct: periodA > 0 ? (delta / periodA) * 100 : (periodB > 0 ? 100 : 0),
      shareOfShift: totalShift !== 0 ? (delta / totalShift) * 100 : 0,
    }
  }).sort((a, b) => Math.abs(b.delta) - Math.abs(a.delta))
}

export function normalizeSpendingDriftRows(
  rows: SpendingDriftRow[],
  spanA: number,
  spanB: number,
): SpendingDriftRow[] {
  return rows.map((row) => {
    const monthlyA = monthlyRate(row.periodA, spanA)
    const monthlyB = monthlyRate(row.periodB, spanB)
    const monthlyDelta = monthlyB - monthlyA
    return {
      ...row,
      monthlyA,
      monthlyB,
      monthlyDeltaPct: monthlyA > 0 ? (monthlyDelta / monthlyA) * 100 : (monthlyB > 0 ? 100 : 0),
    }
  })
}

export function buildSpendingDriftKpis(
  ptsA: ReportPoint[],
  ptsB: ReportPoint[],
  period: PeriodRange,
  comparePeriod: PeriodRange,
): SpendingDriftKpi[] {
  const labelA = formatPeriodPreview(period[0], period[1])
  const labelB = formatPeriodPreview(comparePeriod[0], comparePeriod[1])
  const totalA = sumReportPoints(ptsA)
  const totalB = sumReportPoints(ptsB)
  const spanA = periodDaySpan(period)
  const spanB = periodDaySpan(comparePeriod)
  const comparable = periodsComparable(spanA, spanB)
  const delta = totalB - totalA
  const deltaPct = totalA > 0 ? (delta / totalA) * 100 : 0
  const monthlyA = monthlyRate(totalA, spanA)
  const monthlyB = monthlyRate(totalB, spanB)
  const monthlyDeltaPct = monthlyA > 0 ? ((monthlyB - monthlyA) / monthlyA) * 100 : 0
  const keys = new Set([...ptsA.map((p) => p.key), ...ptsB.map((p) => p.key)])

  return [
    {
      key: 'current',
      label: 'Current period',
      value: formatMoney(totalA),
      hint: `${labelA} · ${periodSpanLabel(spanA)}`,
    },
    {
      key: 'compare',
      label: 'Compare period',
      value: formatMoney(totalB),
      hint: `${labelB} · ${periodSpanLabel(spanB)}`,
    },
    {
      key: 'delta',
      label: comparable ? 'Total change' : 'Total change · uneven periods',
      value: `${delta >= 0 ? '+' : ''}${formatMoney(delta)}`,
      hint: comparable ? `${deltaPct >= 0 ? '+' : ''}${deltaPct.toFixed(1)}%` : 'Use monthly pace below',
      tone: delta > 0 ? 'expense' : delta < 0 ? 'income' : 'neutral',
    },
    {
      key: 'pace',
      label: 'Monthly pace Δ',
      value: `${monthlyDeltaPct >= 0 ? '+' : ''}${monthlyDeltaPct.toFixed(1)}%`,
      hint: `${formatMoney(monthlyA)}/mo → ${formatMoney(monthlyB)}/mo`,
      tone: monthlyDeltaPct > 10 ? 'expense' : monthlyDeltaPct < -10 ? 'income' : 'neutral',
    },
    {
      key: 'cats',
      label: 'Categories',
      value: String(keys.size),
      hint: comparable ? 'Comparable spans' : 'Align periods for fair %',
      tone: comparable ? 'neutral' : 'warn',
    },
  ]
}

export function buildSpendingDriftInsights(
  ptsA: ReportPoint[],
  ptsB: ReportPoint[],
  period: PeriodRange,
  comparePeriod: PeriodRange,
): SpendingDriftInsight[] {
  const labelA = formatPeriodPreview(period[0], period[1])
  const labelB = formatPeriodPreview(comparePeriod[0], comparePeriod[1])
  const spanA = periodDaySpan(period)
  const spanB = periodDaySpan(comparePeriod)
  const comparable = periodsComparable(spanA, spanB)
  const rows = normalizeSpendingDriftRows(buildSpendingDriftRows(ptsA, ptsB), spanA, spanB)
  const totalA = sumReportPoints(ptsA)
  const totalB = sumReportPoints(ptsB)
  if (totalA <= 0 && totalB <= 0) {
    return [{ text: 'No expense data in either period. Broaden the date range or clear filters.', warn: true }]
  }

  const bullets: SpendingDriftInsight[] = []
  if (!comparable) {
    bullets.push({
      text: `Period lengths differ (${periodSpanLabel(spanA)} vs ${periodSpanLabel(spanB)}). Raw totals are misleading — use monthly pace and category deltas below, or align compare to prior-year dates.`,
      warn: true,
    })
  }

  const monthlyA = monthlyRate(totalA, spanA)
  const monthlyB = monthlyRate(totalB, spanB)
  const pacePct = monthlyA > 0 ? ((monthlyB - monthlyA) / monthlyA) * 100 : 0
  bullets.push({
    text: comparable
      ? `Spend ${formatMoney(totalA)} (${labelA}) vs ${formatMoney(totalB)} (${labelB}).`
      : `Monthly pace ${formatMoney(monthlyA)}/mo (${labelA}) vs ${formatMoney(monthlyB)}/mo (${labelB}) — ${pacePct >= 0 ? '+' : ''}${pacePct.toFixed(1)}%.`,
  })

  const topUp = rows.find((r) => r.delta > 0)
  const topDown = rows.find((r) => r.delta < 0)
  if (topUp) {
    bullets.push({
      text: `Largest increase: “${topUp.key}” (+${formatMoney(topUp.delta)}${comparable ? `, +${topUp.deltaPct.toFixed(0)}%` : ''}).`,
      warn: topUp.delta > totalA * 0.08,
    })
  }
  if (topDown) {
    bullets.push({ text: `Largest decrease: “${topDown.key}” (${formatMoney(topDown.delta)}).` })
  }
  bullets.push({ text: 'Click a category row or chart bar to drill into transactions.' })
  return bullets
}

export function buildSpendingDriftChart(
  rows: SpendingDriftRow[],
  labelA: string,
  labelB: string,
  topN = 10,
): EChartsOption {
  const top = rows.slice(0, topN).reverse()
  const labelWidth = Math.min(200, Math.max(96, ...top.map((r) => r.key.length * 7)))
  return {
    grid: { left: 8, right: 20, top: 32, bottom: 12, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v) => formatMoney(Number(v)),
    },
    legend: { data: [labelA, labelB], top: 0, textStyle: { fontSize: 11 } },
    yAxis: {
      type: 'category',
      data: top.map((r) => r.key),
      axisLabel: {
        fontSize: 11,
        width: labelWidth,
        overflow: 'truncate',
        interval: 0,
      },
    },
    xAxis: {
      type: 'value',
      axisLabel: { fontSize: 10, formatter: (v: number) => (v >= 1000 ? `${(v / 1000).toFixed(0)}k` : String(v)) },
    },
    series: [
      {
        name: labelA,
        type: 'bar',
        data: top.map((r) => r.periodA),
        itemStyle: { color: '#94a3b8', borderRadius: [0, 2, 2, 0] },
        barMaxWidth: 12,
        barGap: '20%',
      },
      {
        name: labelB,
        type: 'bar',
        data: top.map((r) => r.periodB),
        itemStyle: { color: '#2563eb', borderRadius: [0, 2, 2, 0] },
        barMaxWidth: 12,
      },
    ],
  }
}

export function spendingDriftChartHeight(rowCount: number): number {
  const n = Math.min(Math.max(rowCount, 4), 12)
  return Math.min(420, Math.max(220, 56 + n * 32))
}

export function shiftCompareToPriorYear(period: PeriodRange): PeriodRange {
  return alignedPriorYearPeriod(period)
}
