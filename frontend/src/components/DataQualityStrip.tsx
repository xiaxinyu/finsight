import { Alert } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { fetchReportDataQuality } from '../api/finance'
import { formatDataQualityStrip } from '../utils/dataQualityStrip'

type Props = {
  metricsSource?: string
  compact?: boolean
}

export function DataQualityStrip({ metricsSource = 'fin_metric_monthly', compact = false }: Props) {
  const { data } = useQuery({
    queryKey: ['report-data-quality', metricsSource],
    queryFn: () => fetchReportDataQuality(metricsSource),
    staleTime: 60_000,
  })

  if (!data) return null

  const type = data.confidence === 'low' ? 'warning' : data.confidence === 'high' ? 'success' : 'info'
  return (
    <Alert
      type={type}
      showIcon
      className={compact ? 'fs-data-quality-strip fs-data-quality-strip--compact' : 'fs-data-quality-strip'}
      message="Data quality"
      description={formatDataQualityStrip(data)}
    />
  )
}
