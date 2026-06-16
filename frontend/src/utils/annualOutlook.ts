import type { EChartsOption } from 'echarts'
import type { ForecastCategory, ForecastData, ForecastMonth } from '../api/analytics'
import { formatMoney } from './format'

export const FORECAST_SCENARIOS = [
  { value: 'base', label: 'Base' },
  { value: 'conservative', label: 'Conservative' },
  { value: 'optimistic', label: 'Optimistic' },
  { value: 'stress', label: 'Stress' },
] as const

export type ForecastScenario = (typeof FORECAST_SCENARIOS)[number]['value']

export function scenarioLabel(scenario: string): string {
  return FORECAST_SCENARIOS.find((s) => s.value === scenario)?.label ?? scenario
}

export function isDeficitMonth(forecast: Pick<ForecastData, 'deficitMonths'>, yearMonth: string): boolean {
  return (forecast.deficitMonths || []).includes(yearMonth)
}

export function buildAnnualOutlookInsights(forecast: ForecastData): { text: string; warn?: boolean }[] {
  const scenario = scenarioLabel(forecast.scenario)
  const deficits = forecast.deficitMonths || []
  const bullets: { text: string; warn?: boolean }[] = []
  for (const line of forecast.explanation || []) {
    bullets.push({ text: line })
  }
  bullets.push({
    text: deficits.length
      ? `${scenario} scenario projects deficit in ${deficits.length} month(s): ${deficits.join(', ')}.`
      : `No projected deficit months under the ${scenario} scenario.`,
    warn: deficits.length > 0,
  })
  if (forecast.budgetTarget?.monthlyCap) {
    bullets.push({
      text: `Budget target: ${formatMoney(forecast.budgetTarget.monthlyCap)}/month (${forecast.budgetTarget.source}).`,
    })
  }
  if (forecast.budgetSuggestion) {
    bullets.push({
      text: `Budget suggestion: ${formatMoney(forecast.budgetSuggestion.monthlyCap)}/month (${forecast.budgetSuggestion.note})`,
    })
  }
  return bullets
}

export function buildAnnualOutlookKpis(forecast: ForecastData) {
  const net = Number(forecast.yearNet ?? forecast.yearIncome - forecast.yearExpense)
  const deficits = forecast.deficitMonths || []
  const halfWidth = forecast.confidence?.halfWidthPct ?? 10
  return [
    { key: 'year', label: 'Year', value: String(forecast.year) },
    { key: 'scenario', label: 'Scenario', value: scenarioLabel(forecast.scenario) },
    { key: 'inc', label: 'Forecast income', value: formatMoney(forecast.yearIncome), tone: 'income' as const },
    { key: 'exp', label: 'Forecast expense', value: formatMoney(forecast.yearExpense), tone: 'expense' as const },
    {
      key: 'net',
      label: 'Forecast net',
      value: forecast.yearNetLower != null && forecast.yearNetUpper != null
        ? `${formatMoney(net)} (${formatMoney(forecast.yearNetLower)} – ${formatMoney(forecast.yearNetUpper)})`
        : formatMoney(net),
      tone: net >= 0 ? 'income' as const : 'expense' as const,
    },
    {
      key: 'conf',
      label: 'Confidence band',
      value: `±${halfWidth}%`,
      tone: 'neutral' as const,
    },
    {
      key: 'def',
      label: 'Deficit months',
      value: String(deficits.length),
      tone: deficits.length ? 'warn' as const : 'neutral' as const,
    },
  ]
}

function netBandSeries(months: ForecastMonth[]) {
  const upper = months.map((m) => (m.forecast ? Number(m.netUpper ?? m.net * 1.1) : null))
  const lower = months.map((m) => (m.forecast ? Number(m.netLower ?? m.net * 0.9) : null))
  return [
    {
      name: 'Net upper',
      type: 'line' as const,
      data: upper,
      lineStyle: { opacity: 0 },
      stack: 'net-confidence',
      symbol: 'none',
      silent: true,
    },
    {
      name: 'Net band',
      type: 'line' as const,
      data: lower.map((l, i) => (l == null || upper[i] == null ? null : upper[i]! - l)),
      lineStyle: { opacity: 0 },
      areaStyle: { color: 'rgba(37, 99, 235, 0.14)' },
      stack: 'net-confidence',
      symbol: 'none',
      silent: true,
      tooltip: { show: false },
    },
  ]
}

function splitActualForecast(months: ForecastMonth[], pick: (m: ForecastMonth) => number) {
  return {
    actual: months.map((m) => (m.actual ? pick(m) : null)),
    forecast: months.map((m) => (m.forecast ? pick(m) : null)),
  }
}

export function buildAnnualOutlookChartOption(forecast: ForecastData): EChartsOption {
  const months = forecast.months || []
  const labels = months.map((m) => m.yearMonth)
  const deficitSet = new Set(forecast.deficitMonths || [])
  const halfWidth = forecast.confidence?.halfWidthPct ?? 10
  const bandLabel = `Net ±${halfWidth}%`
  const incomeSeries = splitActualForecast(months, (m) => m.income)
  const expenseSeries = splitActualForecast(months, (m) => m.expense)
  const netSeries = splitActualForecast(months, (m) => m.net)
  const budgetTarget = forecast.budgetTarget?.monthlyCap ?? months[0]?.budgetTarget ?? null

  return {
    grid: { left: 48, right: 16, top: 48, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const items = Array.isArray(params) ? params : [params]
        const ym = String(items[0]?.name ?? '')
        const month = months.find((m) => m.yearMonth === ym)
        if (!month) return ym
        const band = month.netLower != null && month.netUpper != null
          ? `<br/>Net range: ${formatMoney(month.netLower)} – ${formatMoney(month.netUpper)}`
          : ''
        const deficit = deficitSet.has(ym) ? '<br/><span style="color:#dc2626">Deficit month</span>' : ''
        const kind = month.actual ? '<br/><span style="color:#64748b">Actual</span>' : '<br/><span style="color:#64748b">Forecast</span>'
        return [
          ym + deficit + kind,
          `Income: ${formatMoney(month.income)}`,
          `Expense: ${formatMoney(month.expense)}`,
          `Net: ${formatMoney(month.net)}`,
          month.budgetTarget != null ? `Budget target: ${formatMoney(month.budgetTarget)}` : '',
          band,
        ].filter(Boolean).join('<br/>')
      },
    },
    legend: {
      data: [
        'Income (actual)',
        'Income (forecast)',
        'Expense (actual)',
        'Expense (forecast)',
        'Net (actual)',
        'Net (forecast)',
        'Budget target',
        bandLabel,
      ],
      top: 4,
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: {
        fontSize: 10,
        formatter: (value: string) => (deficitSet.has(value) ? `{def|${value}}` : value),
        rich: { def: { color: '#dc2626', fontWeight: 'bold' } },
      },
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: 'Income (actual)',
        type: 'line',
        smooth: true,
        data: incomeSeries.actual,
        itemStyle: { color: '#16a34a' },
      },
      {
        name: 'Income (forecast)',
        type: 'line',
        smooth: true,
        data: incomeSeries.forecast,
        lineStyle: { type: 'dashed' },
        itemStyle: { color: '#16a34a' },
      },
      {
        name: 'Expense (actual)',
        type: 'line',
        smooth: true,
        data: expenseSeries.actual,
        itemStyle: { color: '#ea580c' },
      },
      {
        name: 'Expense (forecast)',
        type: 'line',
        smooth: true,
        data: expenseSeries.forecast,
        lineStyle: { type: 'dashed' },
        itemStyle: { color: '#ea580c' },
      },
      ...netBandSeries(months),
      {
        name: 'Net (actual)',
        type: 'line',
        smooth: true,
        data: netSeries.actual,
        lineStyle: { width: 2 },
        itemStyle: { color: '#2563eb' },
      },
      {
        name: 'Net (forecast)',
        type: 'line',
        smooth: true,
        data: netSeries.forecast,
        lineStyle: { type: 'dashed', width: 2 },
        itemStyle: { color: '#2563eb' },
        markPoint: {
          symbol: 'pin',
          symbolSize: 36,
          data: (forecast.deficitMonths || []).map((ym) => ({
            name: 'Deficit',
            coord: [ym, months.find((m) => m.yearMonth === ym)?.net ?? 0],
            itemStyle: { color: '#dc2626' },
          })),
        },
      },
      ...(budgetTarget != null ? [{
        name: 'Budget target',
        type: 'line' as const,
        data: months.map(() => budgetTarget),
        lineStyle: { type: 'dotted' as const, color: '#94a3b8' },
        itemStyle: { color: '#94a3b8' },
        symbol: 'none' as const,
      }] : []),
      {
        name: bandLabel,
        type: 'line',
        data: [],
        lineStyle: { type: 'dotted', color: '#93c5fd' },
        itemStyle: { color: '#93c5fd' },
      },
    ],
  }
}

const CATEGORY_COLORS = ['#ea580c', '#7c3aed', '#0891b2', '#ca8a04', '#db2777']

export function buildCategoryForecastChartOption(
  categories: ForecastCategory[],
  yearMonths: string[],
): EChartsOption {
  const top = categories.slice(0, 5)
  return {
    grid: { left: 48, right: 16, top: 48, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const items = Array.isArray(params) ? params : [params]
        const ym = String(items[0]?.name ?? '')
        const lines = [ym]
        for (const item of items) {
          const name = String((item as { seriesName?: string }).seriesName ?? '')
          if (!name || name.endsWith(' band')) continue
          const val = Number((item as { value?: number }).value ?? 0)
          lines.push(`${name}: ${formatMoney(val)}`)
        }
        return lines.join('<br/>')
      },
    },
    legend: { data: top.map((c) => c.categoryName), top: 4 },
    xAxis: { type: 'category', data: yearMonths, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value' },
    series: top.flatMap((cat, idx) => {
      const color = CATEGORY_COLORS[idx % CATEGORY_COLORS.length]
      const amounts = yearMonths.map((ym) => {
        const month = cat.months.find((m) => m.yearMonth === ym)
        return month?.amount ?? 0
      })
      const lowers = yearMonths.map((ym) => {
        const month = cat.months.find((m) => m.yearMonth === ym)
        return month?.amountLower ?? (month?.amount ?? 0) * 0.9
      })
      const uppers = yearMonths.map((ym) => {
        const month = cat.months.find((m) => m.yearMonth === ym)
        return month?.amountUpper ?? (month?.amount ?? 0) * 1.1
      })
      const stack = `cat-${cat.categoryCode}`
      return [
        {
          name: `${cat.categoryName} band`,
          type: 'line' as const,
          data: uppers,
          lineStyle: { opacity: 0 },
          stack,
          symbol: 'none',
          silent: true,
        },
        {
          name: `${cat.categoryName} band`,
          type: 'line' as const,
          data: lowers.map((l, i) => uppers[i] - l),
          lineStyle: { opacity: 0 },
          areaStyle: { color: `${color}22` },
          stack,
          symbol: 'none',
          silent: true,
          tooltip: { show: false },
        },
        {
          name: cat.categoryName,
          type: 'line' as const,
          smooth: true,
          data: amounts,
          lineStyle: { width: 2 },
          itemStyle: { color },
        },
      ]
    }),
  }
}

export function deficitRowClassName(record: Record<string, unknown>, deficitMonths: string[]): string {
  const ym = String(record.yearMonth ?? '')
  return isDeficitMonth({ deficitMonths }, ym) ? 'fs-table-row fs-annual-outlook-row--deficit' : 'fs-table-row'
}
