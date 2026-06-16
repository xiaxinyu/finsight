import type { DrillDownLayer } from './types'

export const DRILL_LAYER_ORDER: DrillDownLayer[] = ['insight', 'breakdown', 'transactions', 'actions']

export type DrillCrumb = {
  key: string
  title: string
  layer: DrillDownLayer
  merchant?: string | null
}

export function drillBreadcrumbs(layer: DrillDownLayer, merchant: string | null): DrillCrumb[] {
  const crumbs: DrillCrumb[] = [
    { key: 'insight', title: 'Insight', layer: 'insight' },
    { key: 'breakdown', title: 'Breakdown', layer: 'breakdown' },
  ]
  if (merchant) {
    crumbs.push({ key: 'merchant', title: merchant, layer: 'transactions', merchant })
  } else if (layer === 'transactions') {
    crumbs.push({ key: 'transactions', title: 'Transactions', layer: 'transactions' })
  }
  if (layer === 'actions') {
    crumbs.push({ key: 'actions', title: 'Actions', layer: 'actions' })
  }
  return crumbs
}

export function previousLayer(layer: DrillDownLayer, merchant: string | null): DrillDownLayer | null {
  if (layer === 'actions') return merchant ? 'transactions' : 'breakdown'
  if (layer === 'transactions') return 'breakdown'
  if (layer === 'breakdown') return 'insight'
  return null
}

export function canAdvanceFromInsight(layer: DrillDownLayer): boolean {
  return layer === 'insight'
}
