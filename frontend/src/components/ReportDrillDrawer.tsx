import { useMemo } from 'react'
import type { DrillDownContext } from './drilldown/types'
import { buildReportDrillContext } from './drilldown/buildDrillContext'
import { UnifiedDrillDrawer } from './drilldown/UnifiedDrillDrawer'

type Props = {
  open: boolean
  params: Record<string, string>
  title?: string
  explanation?: string[]
  onClose: () => void
}

/** @deprecated Use UnifiedDrillDrawer + useDrillDown — kept for gradual migration. */
export function ReportDrillDrawer({ open, params, title = 'Transaction drill-down', explanation, onClose }: Props) {
  const context = useMemo<DrillDownContext>(() => buildReportDrillContext({
    title,
    metricLabel: params.consumeName || title,
    params,
    explanation,
    source: 'report',
  }), [title, params, explanation])

  return <UnifiedDrillDrawer open={open} context={context} onClose={onClose} />
}

export { UnifiedDrillDrawer } from './drilldown/UnifiedDrillDrawer'
