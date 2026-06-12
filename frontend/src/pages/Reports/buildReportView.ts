import type { ReportConfig } from '../../config/reports'
import type { ReportPoint } from '../../api/report'
import type { InsightBullet } from '../../components/InsightPanel'
import { finsightColors } from '../../styles/finsight-tokens'
import { formatMoney } from '../../utils/format'
import { formatPeriodPreview } from '../../utils/periodPresets'
import type { FsColumn } from '../../components/FsDataTable'
import {
  buildMonthlyCashflow,
  buildWeekdaySeries,
  enrichCategoryRows,
  rollupToLevel1,
  insightsBudget,
  insightsCashflow,
  insightsCategoryRows,
  insightsFixedVariable,
  insightsSpendingDrift,
  monthSeriesFromPoints,
  sumReportPoints,
} from '../../utils/reportAnalytics'
import type dayjs from 'dayjs'

export type ReportKpi = {
  key: string
  label: string
  value: string
  hint?: string
  tone?: 'income' | 'expense' | 'neutral' | 'warn'
}

export type ReportView = {
  insights: InsightBullet[]
  kpis: ReportKpi[]
  chartOption: Record<string, unknown>
  tableData: Record<string, unknown>[]
  tableCols: FsColumn<Record<string, unknown>>[]
  tableSummary?: Record<string, number | string>
  chartTitle?: string
}

type Filters = {
  period: [dayjs.Dayjs, dayjs.Dayjs]
  comparePeriod: [dayjs.Dayjs, dayjs.Dayjs]
}

type ReportData = Record<string, unknown>

const chartBase = {
  grid: { left: 48, right: 16, top: 48, bottom: 28 },
  tooltip: { trigger: 'axis' as const },
}

export function buildReportView(
  cfg: ReportConfig,
  data: ReportData | null | undefined,
  applied: Filters,
): ReportView {
  const periodLabel = formatPeriodPreview(applied.period[0], applied.period[1])
  const empty: ReportView = {
    insights: [{ text: 'Adjust filters and click Apply to load data.' }],
    kpis: [],
    chartOption: { title: { text: cfg.title, left: 0, textStyle: { fontSize: 13 } } },
    tableData: [],
    tableCols: [],
  }
  if (!data) return empty

  if ('inc' in data && data.inc && data.exp) {
    const rows = buildMonthlyCashflow(data.inc as ReportPoint[], data.exp as ReportPoint[], applied.period)
    const incomeTotal = rows.reduce((s, r) => s + r.income, 0)
    const expenseTotal = rows.reduce((s, r) => s + r.expense, 0)
    const net = incomeTotal - expenseTotal
    const savings = incomeTotal > 0 ? ((net / incomeTotal) * 100).toFixed(1) : '—'
    return {
      insights: insightsCashflow(rows, periodLabel),
      kpis: [
        { key: 'inc', label: 'Income', value: formatMoney(incomeTotal), tone: 'income' },
        { key: 'exp', label: 'Expense', value: formatMoney(expenseTotal), tone: 'expense' },
        { key: 'net', label: 'Net', value: formatMoney(net), tone: net >= 0 ? 'income' : 'expense', hint: `Savings ${savings}%` },
        { key: 'def', label: 'Deficit months', value: String(rows.filter((r) => r.surplus < 0).length), hint: `of ${rows.length} months` },
      ],
      chartTitle: 'Monthly cash flow',
      chartOption: {
        ...chartBase,
        legend: { data: ['Income', 'Expense', 'Net'], top: 4, textStyle: { fontSize: 11 } },
        xAxis: { type: 'category', data: rows.map((r) => r.month), axisLabel: { fontSize: 10 } },
        yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
        series: [
          { name: 'Income', type: 'bar', data: rows.map((r) => r.income), itemStyle: { color: '#16a34a' }, barMaxWidth: 20 },
          { name: 'Expense', type: 'bar', data: rows.map((r) => r.expense), itemStyle: { color: '#ea580c' }, barMaxWidth: 20 },
          { name: 'Net', type: 'line', smooth: true, data: rows.map((r) => r.surplus), itemStyle: { color: '#2563eb' }, lineStyle: { width: 2 } },
        ],
      },
      tableCols: [
        { title: 'Month', dataIndex: 'month', sortType: 'text' },
        { title: 'Income', dataIndex: 'income', unit: 'CNY', align: 'right', sortType: 'number' },
        { title: 'Expense', dataIndex: 'expense', unit: 'CNY', align: 'right', sortType: 'number' },
        { title: 'Net', dataIndex: 'surplus', unit: 'CNY', align: 'right', sortType: 'number' },
      ],
      tableData: rows as unknown as Record<string, unknown>[],
      tableSummary: { month: 'Total', income: incomeTotal, expense: expenseTotal, surplus: net },
    }
  }

  if ('bva' in data && data.bva) {
    const lines = (data.bva as Record<string, unknown>[]) || []
    const meta = (data.meta as Record<string, unknown>) || {}
    const totalActual = Number(meta.actualTotal || 0)
    const totalLimit = Number(meta.limitTotal || 0)
    const util = totalLimit > 0 ? ((totalActual / totalLimit) * 100).toFixed(1) : '—'
    const enriched: Record<string, unknown>[] = lines.map((l) => {
      const limit = Number(l.limit || 0)
      const actual = Number(l.actual || 0)
      const remaining = Number(l.remaining ?? limit - actual)
      const bucketUtil = limit > 0 ? (actual / limit) * 100 : 0
      return { ...l, remaining, utilization: bucketUtil }
    })
    return {
      insights: insightsBudget(lines, totalActual, totalLimit),
      kpis: [
        { key: 'actual', label: 'Spent (period)', value: formatMoney(totalActual), tone: 'expense' },
        { key: 'limit', label: 'Budget', value: formatMoney(totalLimit) },
        { key: 'rem', label: 'Remaining', value: formatMoney(Math.max(0, totalLimit - totalActual)), tone: totalLimit - totalActual < totalLimit * 0.2 ? 'warn' : 'neutral' },
        { key: 'util', label: 'Utilization', value: `${util}%`, tone: Number(util) >= 80 ? 'warn' : 'neutral' },
      ],
      chartTitle: 'Budget vs actual by bucket',
      chartOption: {
        ...chartBase,
        legend: { data: ['Limit', 'Actual'], top: 4 },
        xAxis: { type: 'category', data: enriched.map((r) => String(r.bucketKey ?? r.categoryCode ?? 'line')), axisLabel: { fontSize: 10, rotate: 20 } },
        yAxis: { type: 'value' },
        series: [
          { name: 'Limit', type: 'bar', data: enriched.map((r) => Number(r.limit ?? 0)), itemStyle: { color: '#94a3b8' }, barMaxWidth: 22 },
          { name: 'Actual', type: 'bar', data: enriched.map((r) => Number(r.actual ?? 0)), itemStyle: { color: '#2563eb' }, barMaxWidth: 22 },
        ],
      },
      tableCols: [
        { title: 'Bucket', dataIndex: 'bucketKey', sortType: 'text' },
        { title: 'Limit', dataIndex: 'limit', unit: 'CNY', align: 'right', sortType: 'number' },
        { title: 'Actual', dataIndex: 'actual', unit: 'CNY', align: 'right', sortType: 'number' },
        { title: 'Used %', dataIndex: 'utilization', align: 'right', sortType: 'percent', render: (v) => `${Number(v).toFixed(1)}%` },
        { title: 'Remaining', dataIndex: 'remaining', unit: 'CNY', align: 'right', sortType: 'number' },
      ],
      tableData: enriched,
      tableSummary: { bucketKey: 'Total', limit: totalLimit, actual: totalActual, remaining: totalLimit - totalActual },
    }
  }

  if ('summary' in data && data.summary) {
    const buckets = (data.summary as Record<string, unknown>).buckets_pct as Record<string, number> || {}
    const weekRows = ('week' in data && data.week) ? buildWeekdaySeries(data.week as ReportPoint[]) : []
    const weekTotal = weekRows.reduce((s, d) => s + d.value, 0)
    return {
      insights: insightsFixedVariable(buckets, weekRows, periodLabel),
      kpis: [
        { key: 'fixed', label: 'Fixed %', value: `${buckets.fixed ?? 0}%` },
        { key: 'var', label: 'Variable %', value: `${buckets.variable ?? buckets.life ?? 0}%` },
        { key: 'week', label: 'Weekday spend', value: formatMoney(weekTotal), hint: periodLabel },
      ],
      chartTitle: 'Fixed vs variable structure',
      chartOption: {
        ...chartBase,
        xAxis: { type: 'category', data: Object.keys(buckets), axisLabel: { fontSize: 10 } },
        yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
        series: [{ type: 'bar', data: Object.values(buckets), itemStyle: { color: '#2563eb' }, barMaxWidth: 28 }],
      },
      tableCols: weekRows.length ? [
        { title: 'Weekday', dataIndex: 'label', sortType: 'text' },
        { title: 'Amount', dataIndex: 'value', unit: 'CNY', align: 'right', sortType: 'number' },
      ] : [],
      tableData: weekRows.map((r) => ({ key: r.label, label: r.label, value: r.value })),
      tableSummary: weekRows.length ? { label: 'Total', value: weekTotal } : undefined,
    }
  }

  if ('a' in data && data.a) {
    const ptsA = data.a as ReportPoint[]
    const ptsB = (data.b as ReportPoint[]) || []
    const labelA = periodLabel
    const labelB = formatPeriodPreview(applied.comparePeriod[0], applied.comparePeriod[1])
    const totalA = sumReportPoints(ptsA)
    const totalB = sumReportPoints(ptsB)
    const deltaPct = totalA > 0 ? ((totalB - totalA) / totalA) * 100 : 0
    const mapA = new Map(ptsA.map((p) => [p.key, p.value]))
    const mapB = new Map(ptsB.map((p) => [p.key, p.value]))
    const keys = [...new Set([...mapA.keys(), ...mapB.keys()])]
    const compareRows = keys.map((key) => {
      const a = mapA.get(key) ?? 0
      const b = mapB.get(key) ?? 0
      return { key, periodA: a, periodB: b, delta: b - a, deltaPct: a > 0 ? ((b - a) / a) * 100 : 0 }
    }).sort((x, y) => Math.abs(y.delta) - Math.abs(x.delta))
    const topMovers = compareRows.slice(0, 10)
    return {
      insights: insightsSpendingDrift(ptsA, ptsB, labelA, labelB),
      kpis: [
        { key: 'y1', label: labelA, value: formatMoney(totalA) },
        { key: 'y2', label: labelB, value: formatMoney(totalB) },
        { key: 'delta', label: 'Change', value: `${deltaPct >= 0 ? '+' : ''}${deltaPct.toFixed(1)}%`, tone: deltaPct > 0 ? 'expense' : 'income' },
        { key: 'cats', label: 'Categories', value: String(keys.length) },
      ],
      chartTitle: `Spending drift · ${labelA} vs ${labelB}`,
      chartOption: {
        tooltip: { trigger: 'axis' },
        legend: { data: [labelA, labelB], top: 4 },
        grid: chartBase.grid,
        xAxis: { type: 'category', data: topMovers.map((r) => r.key), axisLabel: { fontSize: 10, rotate: 25 } },
        yAxis: { type: 'value' },
        series: [
          { name: labelA, type: 'bar', data: topMovers.map((r) => r.periodA), itemStyle: { color: '#94a3b8' }, barMaxWidth: 16 },
          { name: labelB, type: 'bar', data: topMovers.map((r) => r.periodB), itemStyle: { color: '#2563eb' }, barMaxWidth: 16 },
        ],
      },
      tableCols: [
        { title: 'Category', dataIndex: 'key', sortType: 'text', ellipsis: true },
        { title: labelA, dataIndex: 'periodA', unit: 'CNY', align: 'right', sortType: 'number' },
        { title: labelB, dataIndex: 'periodB', unit: 'CNY', align: 'right', sortType: 'number' },
        { title: 'Δ', dataIndex: 'delta', unit: 'CNY', align: 'right', sortType: 'number', isDelta: true },
        { title: 'Δ%', dataIndex: 'deltaPct', align: 'right', sortType: 'percent', render: (v) => `${Number(v).toFixed(1)}%` },
      ],
      tableData: compareRows as unknown as Record<string, unknown>[],
      tableSummary: { key: 'Total', periodA: totalA, periodB: totalB, delta: totalB - totalA },
    }
  }

  if ('rows' in data && data.rows) {
    const rawRows = data.rows as ReportPoint[]
    const catRows = enrichCategoryRows(rawRows)
    const chartRows = enrichCategoryRows(rollupToLevel1(rawRows))
    const total = catRows.reduce((s, r) => s + r.value, 0)
    const top = chartRows.slice(0, 12)

    if (cfg.type === 'weekSummary' || cfg.type === 'homeBuckets') {
      const week = buildWeekdaySeries(data.rows as ReportPoint[])
      const weekTotal = week.reduce((s, d) => s + d.value, 0)
      return {
        insights: insightsCategoryRows(catRows, periodLabel),
        kpis: [
          { key: 'total', label: 'Total', value: formatMoney(weekTotal) },
          { key: 'peak', label: 'Peak day', value: week.sort((a, b) => b.value - a.value)[0]?.label ?? '—' },
        ],
        chartTitle: 'Spend by weekday',
        chartOption: {
          ...chartBase,
          xAxis: { type: 'category', data: week.map((d) => d.label) },
          yAxis: { type: 'value' },
          series: [{ type: 'bar', data: week.map((d) => d.value), itemStyle: { color: '#2563eb' }, barMaxWidth: 28 }],
        },
        tableCols: [
          { title: 'Weekday', dataIndex: 'label', sortType: 'text' },
          { title: 'Amount', dataIndex: 'value', unit: 'CNY', align: 'right', sortType: 'number' },
        ],
        tableData: week.map((d) => ({ key: d.label, label: d.label, value: d.value })),
        tableSummary: { label: 'Total', value: weekTotal },
      }
    }

    if (cfg.type === 'monthlyCompare' || cfg.type === 'timeCurve') {
      const series = monthSeriesFromPoints(data.rows as ReportPoint[], applied.period)
      const yearTotal = series.reduce((s, d) => s + d.value, 0)
      const peak = [...series].sort((a, b) => b.value - a.value)[0]
      return {
        insights: [
          { text: `${cfg.title} total ${formatMoney(yearTotal)} for ${periodLabel}.` },
          peak ? { text: `Peak month: ${peak.label} (${formatMoney(peak.value)}).` } : { text: 'No monthly values in range.' },
        ],
        kpis: [
          { key: 'yt', label: 'Period total', value: formatMoney(yearTotal) },
          { key: 'avg', label: 'Monthly avg', value: formatMoney(series.length ? yearTotal / series.length : 0) },
        ],
        chartTitle: cfg.title,
        chartOption: {
          ...chartBase,
          xAxis: { type: 'category', data: series.map((d) => d.label) },
          yAxis: { type: 'value' },
          series: [{
            name: cfg.title,
            type: 'line',
            smooth: true,
            areaStyle: { opacity: 0.08 },
            data: series.map((d) => d.value),
            itemStyle: { color: cfg.txnType === 'income' ? finsightColors.income : '#2563eb' },
          }],
        },
        tableCols: [
          { title: 'Month', dataIndex: 'label', sortType: 'text' },
          { title: 'Amount', dataIndex: 'value', unit: 'CNY', align: 'right', sortType: 'number' },
        ],
        tableData: series.map((d) => ({ key: d.label, label: d.label, value: d.value })),
        tableSummary: { label: 'Total', value: yearTotal },
      }
    }

    const donut = cfg.chartKind === 'donut'
    return {
      insights: insightsCategoryRows(catRows, periodLabel),
      kpis: [
        { key: 'total', label: 'Total spend', value: formatMoney(total), tone: 'expense' },
        { key: 'cats', label: 'Categories', value: String(catRows.length) },
        { key: 'top', label: 'Top share', value: `${catRows[0]?.share.toFixed(1) ?? 0}%`, hint: catRows[0]?.key },
      ],
      chartTitle: cfg.title,
      chartOption: donut ? {
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie',
          radius: ['44%', '70%'],
          center: ['50%', '52%'],
          data: top.map((r) => ({ name: r.key, value: r.value })),
          label: { fontSize: 11, formatter: '{b}\n{d}%' },
        }],
      } : {
        ...chartBase,
        xAxis: { type: 'category', data: top.map((r) => r.key), axisLabel: { fontSize: 10, rotate: 25 } },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: top.map((r) => r.value), itemStyle: { color: '#2563eb' }, barMaxWidth: 22 }],
      },
      tableCols: [
        { title: 'Category', dataIndex: 'key', sortType: 'text', ellipsis: true },
        { title: 'Amount', dataIndex: 'value', unit: 'CNY', align: 'right', sortType: 'number' },
        { title: 'Share', dataIndex: 'share', align: 'right', sortType: 'percent', render: (v) => `${Number(v).toFixed(1)}%` },
      ],
      tableData: top.map((r) => ({ key: r.key, value: r.value, share: r.share })),
      tableSummary: { key: 'Total', value: total, share: 100 },
    }
  }

  if ('forecast' in data && data.forecast) {
    const forecast = data.forecast as Record<string, unknown>
    const months = (forecast.months as Record<string, unknown>[]) || []
    const incomeTotal = Number(forecast.yearIncome || 0)
    const expenseTotal = Number(forecast.yearExpense || 0)
    const net = Number(forecast.yearNet || incomeTotal - expenseTotal)
    const deficits = (forecast.deficitMonths as string[]) || []
    return {
      insights: [{ text: deficits.length ? `Projected deficit in ${deficits.length} month(s).` : 'No projected deficit months in base scenario.' }],
      kpis: [
        { key: 'inc', label: 'Forecast income', value: formatMoney(incomeTotal), tone: 'income' },
        { key: 'exp', label: 'Forecast expense', value: formatMoney(expenseTotal), tone: 'expense' },
        { key: 'net', label: 'Forecast net', value: formatMoney(net), tone: net >= 0 ? 'income' : 'expense' },
        { key: 'def', label: 'Deficit months', value: String(deficits.length), tone: deficits.length ? 'warn' : 'neutral' },
      ],
      chartTitle: 'Forecast cash flow (dashed = projected)',
      chartOption: {
        ...chartBase,
        legend: { data: ['Income', 'Expense', 'Net'], top: 4 },
        xAxis: { type: 'category', data: months.map((m) => String(m.yearMonth)), axisLabel: { fontSize: 10 } },
        yAxis: { type: 'value' },
        series: [
          { name: 'Income', type: 'line', smooth: true, data: months.map((m) => Number(m.income || 0)), lineStyle: { type: 'dashed' }, itemStyle: { color: '#16a34a' } },
          { name: 'Expense', type: 'line', smooth: true, data: months.map((m) => Number(m.expense || 0)), lineStyle: { type: 'dashed' }, itemStyle: { color: '#ea580c' } },
          { name: 'Net', type: 'line', smooth: true, data: months.map((m) => Number(m.net || 0)), lineStyle: { type: 'dashed', width: 2 }, itemStyle: { color: '#2563eb' } },
        ],
      },
      tableCols: [
        { title: 'Month', dataIndex: 'yearMonth', sortType: 'text' },
        { title: 'Income', dataIndex: 'income', unit: 'CNY', align: 'right', sortType: 'number' },
        { title: 'Expense', dataIndex: 'expense', unit: 'CNY', align: 'right', sortType: 'number' },
        { title: 'Net', dataIndex: 'net', unit: 'CNY', align: 'right', sortType: 'number' },
      ],
      tableData: months,
      tableSummary: { yearMonth: 'Year total', income: incomeTotal, expense: expenseTotal, net },
    }
  }

  if ('trends' in data && data.trends) {
    const trends = data.trends as Record<string, unknown>
    const growth = (trends.topCategoryGrowth as Record<string, unknown>[]) || []
    return {
      insights: growth.slice(0, 3).map((g) => ({
        text: `${g.categoryCode}: ${g.pctChange}% YoY (${formatMoney(Number(g.deltaAmount || 0))})`,
      })),
      kpis: [
        { key: 'from', label: 'From year', value: String(trends.fromYear || '') },
        { key: 'to', label: 'To year', value: String(trends.toYear || '') },
        { key: 'n', label: 'Significant shifts', value: String(growth.length) },
      ],
      chartTitle: 'Top category growth',
      chartOption: {
        ...chartBase,
        xAxis: { type: 'category', data: growth.map((g) => String(g.categoryCode)), axisLabel: { fontSize: 10, rotate: 20 } },
        yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
        series: [{ type: 'bar', data: growth.map((g) => Number(g.pctChange || 0)), itemStyle: { color: '#7c3aed' }, barMaxWidth: 22 }],
      },
      tableCols: [
        { title: 'Category', dataIndex: 'categoryCode', sortType: 'text' },
        { title: 'Change %', dataIndex: 'pctChange', align: 'right', sortType: 'number' },
        { title: 'Delta', dataIndex: 'deltaAmount', unit: 'CNY', align: 'right', sortType: 'number' },
      ],
      tableData: growth,
    }
  }

  return empty
}
