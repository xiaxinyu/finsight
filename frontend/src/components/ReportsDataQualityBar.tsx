import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Alert } from 'antd'
import { fetchReportDataQuality } from '../api/finance'
import { useFeatureFlags } from '../hooks/useFeatureFlags'
import { formatDataQualityStrip } from '../utils/dataQualityStrip'

/** Shared data-quality context for all /reports/* pages (shown once per route). */
export function ReportsDataQualityBar() {
  const { flags } = useFeatureFlags()
  const { data, isLoading } = useQuery({
    queryKey: ['report-data-quality', 'shared'],
    queryFn: () => fetchReportDataQuality('v_transaction_finance_semantics'),
    staleTime: 60_000,
  })

  if (isLoading || !data) return null

  const tone = data.confidence === 'low' ? 'warn' : data.confidence === 'high' ? 'ok' : 'default'
  const unclassified = data.unclassifiedCount ?? 0
  const metricsGate = data.metricsGate as { ok?: boolean; mismatches?: string[]; warning?: string } | undefined
  const gateFailed = flags.metricsReconcileGate && metricsGate && metricsGate.ok === false

  return (
    <>
      {gateFailed && (
        <Alert
          type="warning"
          showIcon
          banner
          className="fs-reports-reconcile-banner"
          message="Stored metrics differ from live ledger totals"
          description={
            metricsGate?.warning
            || (metricsGate?.mismatches?.length
              ? metricsGate.mismatches.join(' · ')
              : 'Forecast and Profile may show degraded numbers until metrics refresh.')
          }
        />
      )}
      <div className={`fs-reports-quality-bar fs-reports-quality-bar--${tone}`} role="status">
        <span className="fs-reports-quality-bar__label">Data quality</span>
        <span className="fs-reports-quality-bar__text">{formatDataQualityStrip(data)}</span>
        {unclassified > 0 && (
          <Link to="/transactions?emptyConsume=1" className="fs-reports-quality-bar__action">
            Review unclassified
          </Link>
        )}
      </div>
    </>
  )
}
