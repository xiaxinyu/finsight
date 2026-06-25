import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchReportDataQuality } from '../api/finance'
import { formatDataQualityStrip } from '../utils/dataQualityStrip'

/** Shared data-quality context for all /reports/* pages (shown once per route). */
export function ReportsDataQualityBar() {
  const { data, isLoading } = useQuery({
    queryKey: ['report-data-quality', 'shared'],
    queryFn: () => fetchReportDataQuality('fin_metric_monthly'),
    staleTime: 60_000,
  })

  if (isLoading || !data) return null

  const tone = data.confidence === 'low' ? 'warn' : data.confidence === 'high' ? 'ok' : 'default'
  const unclassified = data.unclassifiedCount ?? 0

  return (
    <div className={`fs-reports-quality-bar fs-reports-quality-bar--${tone}`} role="status">
      <span className="fs-reports-quality-bar__label">Data quality</span>
      <span className="fs-reports-quality-bar__text">{formatDataQualityStrip(data)}</span>
      {unclassified > 0 && (
        <Link to="/transactions?unclassified=1" className="fs-reports-quality-bar__action">
          Review unclassified
        </Link>
      )}
    </div>
  )
}
