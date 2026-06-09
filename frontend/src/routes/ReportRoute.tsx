import { Navigate, useParams } from 'react-router-dom'
import { legacyReportRedirects } from '../config/reports'
import { ReportPageView } from '../pages/Reports/ReportPageView'

export function ReportRoute() {
  const { reportId = '' } = useParams()
  const target = legacyReportRedirects[reportId]
  if (target) {
    return <Navigate to={`/reports/${target}`} replace />
  }
  return <ReportPageView />
}
