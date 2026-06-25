import { Navigate, useParams } from 'react-router-dom'
import { legacyReportRedirects } from '../config/reports'
import { ReportsDataQualityBar } from '../components/ReportsDataQualityBar'
import { ReportsPage } from '../pages/Reports'

export function ReportRoute() {
  const { reportId = '' } = useParams()
  const target = legacyReportRedirects[reportId]
  if (target) {
    return <Navigate to={`/reports/${target}`} replace />
  }
  return (
    <div className="fs-reports-route">
      <ReportsDataQualityBar />
      <ReportsPage />
    </div>
  )
}
