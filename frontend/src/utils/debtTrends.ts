import type { EChartsOption } from 'echarts'
import type { CategoryYearMatrix, CategoryYearMatrixRow } from './trendChanges'
import { buildCategoryYearTrendChart, downloadCategoryYearMatrixCsv, yearColumnLabel } from './trendChanges'
import { formatMoney } from './format'
import { SEMANTIC_TAG_LABELS } from './categorySemantics'

export type TrendDeltaMetric = {
  from: number
  to: number
  deltaAmount: number
  deltaPercent: number
}

export type DebtTrendMover = {
  categoryCode?: string
  categoryName?: string
  label?: string
  fromAmount?: number
  toAmount?: number
  deltaAmount: number
  deltaPercent?: number
  pctChange?: number
  contributionPct: number
  drillDown?: Record<string, string>
}

export type DebtYearPoint = {
  year: number
  borrowing: number
  repayment: number
  net: number
  cumulativeNet?: number
  estimatedBalance?: number
  debtDirection?: 'increase' | 'decrease' | 'flat'
  yoyNetDelta?: number
  partial: boolean
  throughDate?: string
}

export type DebtBalanceSummary = {
  currentLiabilities: number
  loanOutstanding?: number
  creditCardLiabilities?: number
  asOfDate?: string
  historyFromYear?: number
  periodStartBalance?: number
  periodBalanceChange?: number
  periodTrend?: 'increase' | 'decrease' | 'flat'
  source?: 'loan_ledger' | 'bank_card_balances'
  note?: string
}

export type LoanLedgerLender = {
  loanId?: string
  lenderName?: string
  outstandingBalance?: number
  monthlyPayment?: number
  interestRatePct?: number
  linkCount?: number
  maturityDate?: string
}

export type LoanLedgerSummary = {
  activeLoanCount?: number
  totalOutstanding?: number
  totalMonthlyPayment?: number
  weightedAvgRatePct?: number
  annualizedRepaymentEstimate?: number
  lenders?: LoanLedgerLender[]
}

export type DebtTrendsReport = {
  fromYear: number
  toYear: number
  historyFromYear?: number
  compareMode?: 'ytd_aligned' | 'full_year'
  debtBalance?: DebtBalanceSummary
  loanLedger?: LoanLedgerSummary
  summary: {
    borrowing: TrendDeltaMetric
    repayment: TrendDeltaMetric
    netFlow: TrendDeltaMetric
    headline?: string
  }
  debtYearSeries: DebtYearPoint[]
  repaymentTypeMatrix: CategoryYearMatrix
  borrowingTypeMatrix: CategoryYearMatrix
  loanRepaymentMatrix?: CategoryYearMatrix
  loanBorrowingMatrix?: CategoryYearMatrix
  topRepaymentGrowth: DebtTrendMover[]
  debtPressure: {
    detected: boolean
    borrowingPctChange: number
    repaymentPctChange: number
    gapPct: number
    note: string
  }
}

export type DebtYoYCard = {
  key: string
  label: string
  from: number
  to: number
  deltaAmount: number
  deltaPercent: number
  tone: 'warn' | 'expense' | 'income' | 'neutral'
  hint?: string
}

const CARD_ORDER = ['repayment', 'borrowing', 'netFlow'] as const

export function debtTypeLabel(tagId: string, fallback?: string): string {
  return SEMANTIC_TAG_LABELS[tagId as keyof typeof SEMANTIC_TAG_LABELS] || fallback || tagId
}

export function buildDebtYoYCards(report: DebtTrendsReport): DebtYoYCard[] {
  const rep = report.summary.repayment
  const bor = report.summary.borrowing
  const net = report.summary.netFlow
  const fromLedger = (report.loanLedger?.activeLoanCount ?? 0) > 0
  return [
    {
      key: 'repayment',
      label: fromLedger ? '还款' : 'Repayments',
      from: rep.from,
      to: rep.to,
      deltaAmount: rep.deltaAmount,
      deltaPercent: rep.deltaPercent,
      tone: rep.deltaAmount > 0 ? 'warn' : 'income',
      hint: fromLedger ? '贷款月供与关联流水（含信用卡还款）' : 'Money paid toward loans & credit',
    },
    {
      key: 'borrowing',
      label: fromLedger ? '新增借款' : 'New borrowing',
      from: bor.from,
      to: bor.to,
      deltaAmount: bor.deltaAmount,
      deltaPercent: bor.deltaPercent,
      tone: bor.deltaAmount > 0 ? 'expense' : 'neutral',
      hint: fromLedger ? '放款与信用类借款流水' : 'Loans received & credit drawn',
    },
    {
      key: 'netFlow',
      label: fromLedger ? '净现金流' : 'Net flow',
      from: net.from,
      to: net.to,
      deltaAmount: net.deltaAmount,
      deltaPercent: net.deltaPercent,
      tone: net.to < 0 ? 'warn' : net.to > 0 ? 'income' : 'neutral',
      hint: fromLedger ? '借款减还款（负值 = 负债上升）' : 'Borrowing minus repayments (negative = debt load up)',
    },
  ]
}

export function orderedDebtCards(cards: DebtYoYCard[]): DebtYoYCard[] {
  return CARD_ORDER
    .map((key) => cards.find((c) => c.key === key))
    .filter((c): c is DebtYoYCard => c != null)
}

export function debtDirectionLabel(direction?: DebtYearPoint['debtDirection']): string {
  if (direction === 'increase') return 'Debt up'
  if (direction === 'decrease') return 'Debt down'
  return 'Stable'
}

export function debtDirectionTone(direction?: DebtYearPoint['debtDirection']): 'danger' | 'success' | 'default' {
  if (direction === 'increase') return 'danger'
  if (direction === 'decrease') return 'success'
  return 'default'
}

export function buildDebtInsights(report: DebtTrendsReport) {
  const bullets: { text: string; warn?: boolean }[] = []
  const balance = report.debtBalance
  const ledger = report.loanLedger
  if (ledger?.activeLoanCount && ledger.activeLoanCount > 0) {
    bullets.push({
      text: `Loan ledger: ${ledger.activeLoanCount} active facilities, ${formatMoney(ledger.totalOutstanding ?? 0)} outstanding, ${formatMoney(ledger.totalMonthlyPayment ?? 0)}/mo (${(ledger.weightedAvgRatePct ?? 0).toFixed(2)}% avg rate).`,
      warn: (ledger.totalOutstanding ?? 0) > 0,
    })
  }
  if (balance?.currentLiabilities != null) {
    const change = balance.periodBalanceChange ?? 0
    const from = balance.historyFromYear ?? report.historyFromYear ?? report.fromYear
    const sourceLabel = balance.source === 'loan_ledger' ? 'Loan outstanding' : 'Outstanding debt'
    if (Math.abs(change) >= 500) {
      const dir = change > 0 ? 'increased' : 'reduced'
      bullets.push({
        text: `${sourceLabel} ${dir} by ${formatMoney(Math.abs(change))} since ${from} (now ${formatMoney(balance.currentLiabilities)}).`,
        warn: change > 0,
      })
    } else {
      bullets.push({
        text: `${sourceLabel} is ${formatMoney(balance.currentLiabilities)} as of today${balance.creditCardLiabilities ? ` (incl. ${formatMoney(balance.creditCardLiabilities)} credit cards)` : ''}.`,
        warn: balance.currentLiabilities > 0,
      })
    }
  }
  bullets.push({ text: report.summary.headline || 'Year-over-year debt cash flow.' })
  const pressure = report.debtPressure
  bullets.push({
    text: `Borrowing ${pressure.borrowingPctChange >= 0 ? '+' : ''}${pressure.borrowingPctChange.toFixed(1)}% vs repayments ${pressure.repaymentPctChange >= 0 ? '+' : ''}${pressure.repaymentPctChange.toFixed(1)}% (gap ${pressure.gapPct >= 0 ? '+' : ''}${pressure.gapPct.toFixed(1)} pts).`,
    warn: pressure.detected,
  })
  if (pressure.detected) {
    bullets.push({ text: pressure.note, warn: true })
  }
  const top = report.topRepaymentGrowth[0]
  if (top) {
    bullets.push({
      text: `Largest repayment shift: ${top.categoryName || top.label} (${formatMoney(top.deltaAmount)}).`,
      warn: Number(top.deltaAmount) > 0,
    })
  }
  return bullets
}

export function buildDebtYearChart(series: DebtYearPoint[]): EChartsOption {
  const labels = series.map((p) => yearColumnLabel(p.year, series.filter((x) => x.partial).map((x) => String(x.year))))
  return {
    grid: { left: 48, right: 16, top: 40, bottom: 28 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['Repayments', 'New borrowing'], top: 4, textStyle: { fontSize: 11 } },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    series: [
      {
        name: 'Repayments',
        type: 'bar',
        data: series.map((p) => ({
          value: p.repayment,
          itemStyle: { color: p.partial ? '#fdba74' : '#ea580c', borderRadius: [4, 4, 0, 0] },
        })),
        barMaxWidth: 36,
      },
      {
        name: 'New borrowing',
        type: 'bar',
        data: series.map((p) => ({
          value: p.borrowing,
          itemStyle: { color: p.partial ? '#93c5fd' : '#2563eb', borderRadius: [4, 4, 0, 0] },
        })),
        barMaxWidth: 36,
      },
    ],
  }
}

export function buildNetDebtLineChart(series: DebtYearPoint[]): EChartsOption {
  const labels = series.map((p) => yearColumnLabel(p.year, series.filter((x) => x.partial).map((x) => String(x.year))))
  return {
    grid: { left: 48, right: 16, top: 24, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const row = Array.isArray(params) ? params[0] : params
        const idx = Number((row as { dataIndex?: number }).dataIndex ?? 0)
        const pt = series[idx]
        if (!pt) return ''
        const dir = debtDirectionLabel(pt.debtDirection)
        return [
          `<b>${labels[idx]}</b>`,
          `Net ${formatMoney(pt.net)} (${dir})`,
          `Borrowed ${formatMoney(pt.borrowing)} · Repaid ${formatMoney(pt.repayment)}`,
          pt.estimatedBalance != null ? `Est. balance ${formatMoney(pt.estimatedBalance)}` : '',
        ].filter(Boolean).join('<br/>')
      },
    },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    series: [{
      name: 'Net flow',
      type: 'line',
      smooth: true,
      data: series.map((p) => ({
        value: p.net,
        itemStyle: {
          color: p.debtDirection === 'increase' ? '#dc2626'
            : p.debtDirection === 'decrease' ? '#16a34a' : '#64748b',
        },
      })),
      lineStyle: { color: '#94a3b8', width: 2 },
      areaStyle: { opacity: 0.06, color: '#64748b' },
      markLine: {
        silent: true,
        symbol: 'none',
        lineStyle: { type: 'dashed', color: '#cbd5e1' },
        data: [{ yAxis: 0 }],
      },
    }],
  }
}

export function buildDebtBalanceChart(series: DebtYearPoint[]): EChartsOption {
  const partialYears = series.filter((x) => x.partial).map((x) => String(x.year))
  const labels = series.map((p) => yearColumnLabel(p.year, partialYears))
  const hasBalance = series.some((p) => p.estimatedBalance != null)
  if (!hasBalance) return {}
  return {
    grid: { left: 52, right: 16, top: 28, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const row = Array.isArray(params) ? params[0] : params
        const idx = Number((row as { dataIndex?: number }).dataIndex ?? 0)
        const pt = series[idx]
        if (!pt) return ''
        return [
          `<b>${labels[idx]}</b>`,
          `Est. outstanding ${formatMoney(pt.estimatedBalance ?? 0)}`,
          `Net ${formatMoney(pt.net)} (${debtDirectionLabel(pt.debtDirection)})`,
        ].join('<br/>')
      },
    },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 }, min: 0 },
    series: [{
      name: 'Outstanding debt',
      type: 'line',
      smooth: true,
      data: series.map((p) => ({
        value: p.estimatedBalance ?? 0,
        itemStyle: { color: p.partial ? '#f97316' : '#7c2d12' },
      })),
      lineStyle: { color: '#9a3412', width: 2.5 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(124, 45, 18, 0.18)' },
            { offset: 1, color: 'rgba(124, 45, 18, 0.02)' },
          ],
        },
      },
    }],
  }
}

export { buildCategoryYearTrendChart, downloadCategoryYearMatrixCsv, yearColumnLabel }

export function debtMatrixTotals(matrix: CategoryYearMatrix): Record<string, number> {
  const totals: Record<string, number> = {}
  for (const y of matrix.years) {
    totals[String(y)] = matrix.rows.reduce((s, r) => s + Number(r.amountsByYear[String(y)] ?? 0), 0)
  }
  return totals
}

export function officialDebtTotals(series: DebtYearPoint[], kind: 'repayment' | 'borrowing'): Record<string, number> {
  const totals: Record<string, number> = {}
  for (const p of series) {
    totals[String(p.year)] = kind === 'repayment' ? p.repayment : p.borrowing
  }
  return totals
}

export function buildRepaymentMoverChart(movers: DebtTrendMover[]): EChartsOption {
  const sorted = [...movers]
    .sort((a, b) => Math.abs(Number(b.deltaAmount || 0)) - Math.abs(Number(a.deltaAmount || 0)))
    .slice(0, 8)
  const chartRows = [...sorted].reverse()
  const labels = chartRows.map((m) => m.categoryName || m.label || '—')
  return {
    grid: { left: 8, right: 20, top: 8, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    yAxis: { type: 'category', data: labels, axisLabel: { interval: 0, fontSize: 11 } },
    series: [{
      name: 'Repayment Δ',
      type: 'bar',
      data: chartRows.map((m) => {
        const delta = Number(m.deltaAmount || 0)
        return {
          value: delta,
          itemStyle: { color: delta >= 0 ? '#ea580c' : '#16a34a', borderRadius: delta >= 0 ? [0, 4, 4, 0] : [4, 0, 0, 4] },
        }
      }),
      barMaxWidth: 22,
    }],
  }
}

export function moverLabel(row: DebtTrendMover): string {
  return String(row.label || row.categoryName || row.categoryCode || '—')
}

export function moverKey(row: DebtTrendMover): string {
  return String(row.categoryCode || row.categoryName || row.label)
}

export function trendChartHeight(count: number): number {
  const rows = Math.max(count, 3)
  return Math.min(420, Math.max(220, rows * 36 + 48))
}

export type { CategoryYearMatrix, CategoryYearMatrixRow }
