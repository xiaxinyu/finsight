import dayjs from 'dayjs'
import { MONTH_NAMES } from '../../utils/format'
import type { DrillDownAction, DrillDownContext } from './types'

export const DEFAULT_DRILL_ACTIONS: DrillDownAction[] = [
  { label: 'Adjust budget', type: 'planning', path: '/planning' },
  { label: 'Create classification rule', type: 'rules', path: '/admin/rules' },
  { label: 'Review transactions', type: 'transactions', path: '/transactions' },
  { label: 'Mark internal transfers', type: 'transfers', path: '/transactions' },
]

export function mergeDrillActions(custom?: DrillDownAction[]): DrillDownAction[] {
  if (!custom?.length) return DEFAULT_DRILL_ACTIONS
  const seen = new Set<string>()
  const out: DrillDownAction[] = []
  for (const action of [...custom, ...DEFAULT_DRILL_ACTIONS]) {
    if (seen.has(action.path)) continue
    seen.add(action.path)
    out.push(action)
  }
  return out
}

export function buildReportDrillContext(input: {
  title: string
  metricLabel: string
  params: Record<string, string>
  explanation?: string[]
  actions?: DrillDownAction[]
  source?: DrillDownContext['source']
  provenance?: DrillDownContext['provenance']
}): DrillDownContext {
  const category = input.params.consumeName
  const semanticTag = input.params.semanticFilter
  const explanation = input.explanation?.length
    ? input.explanation
    : [
        semanticTag
          ? `Drill into "${input.metricLabel}" classification for the selected period.`
          : category
            ? `Drill into "${category}" for the selected period.`
            : 'Explore category and merchant contribution for this slice.',
        'Use breakdown to see where spending clusters, then open transactions for line-level review.',
      ]
  return {
    title: input.title,
    metricLabel: input.metricLabel,
    explanation,
    params: input.params,
    actions: mergeDrillActions(input.actions),
    source: input.source ?? 'report',
    provenance: {
      filterParams: { ...input.params },
      ...input.provenance,
    },
  }
}

export function buildDashboardDrillContext(input: {
  title: string
  metricLabel: string
  params: Record<string, string>
  explanation: string[]
  actions?: DrillDownAction[]
}): DrillDownContext {
  return {
    title: input.title,
    metricLabel: input.metricLabel,
    explanation: input.explanation,
    params: input.params,
    actions: mergeDrillActions(input.actions),
    source: 'dashboard',
  }
}

export function drillParamsForMonth(
  monthLabel: string,
  year: number,
  txnTypes: 'income' | 'expense' = 'expense',
): Record<string, string> {
  const idx = MONTH_NAMES.indexOf(monthLabel)
  if (idx < 0) return { txnTypes }
  const start = dayjs().year(year).month(idx).startOf('month').format('YYYY-MM-DD')
  const end = dayjs().year(year).month(idx).endOf('month').format('YYYY-MM-DD')
  return { transactionDateStartStr: start, transactionDateEndStr: end, txnTypes }
}

export function drillParamsForSemanticTag(
  tagId: string,
  periodStart: string,
  periodEnd: string,
  txnTypes: 'income' | 'expense' = 'expense',
): Record<string, string> {
  return {
    transactionDateStartStr: periodStart,
    transactionDateEndStr: periodEnd,
    txnTypes,
    semanticFilter: tagId,
  }
}

export function drillParamsForCategory(
  categoryName: string,
  periodStart: string,
  periodEnd: string,
  txnTypes: 'income' | 'expense' = 'expense',
): Record<string, string> {
  return {
    transactionDateStartStr: periodStart,
    transactionDateEndStr: periodEnd,
    txnTypes,
    consumeName: categoryName,
  }
}

export function drillParamsForYearMonth(
  yearMonth: string,
  txnTypes: 'income' | 'expense' = 'expense',
): Record<string, string> {
  const d = dayjs(`${yearMonth}-01`)
  if (!d.isValid()) return { txnTypes }
  return {
    transactionDateStartStr: d.startOf('month').format('YYYY-MM-DD'),
    transactionDateEndStr: d.endOf('month').format('YYYY-MM-DD'),
    txnTypes,
  }
}

export function drillParamsForMerchant(
  merchantToken: string,
  displayName: string,
  periodStart: string,
  periodEnd: string,
  txnTypes: 'income' | 'expense' = 'expense',
): Record<string, string> {
  return {
    transactionDateStartStr: periodStart,
    transactionDateEndStr: periodEnd,
    txnTypes,
    merchantToken,
    merchantLabel: displayName,
  }
}
