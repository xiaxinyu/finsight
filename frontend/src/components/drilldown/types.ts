export type DrillDownLayer = 'insight' | 'breakdown' | 'transactions' | 'actions'

export type DrillDownAction = {
  label: string
  type: string
  path: string
}

export type DrillDownContext = {
  title: string
  metricLabel: string
  explanation: string[]
  params: Record<string, string>
  actions?: DrillDownAction[]
  source?: 'dashboard' | 'report' | 'profile' | 'cash-risk' | 'annual-outlook'
}
