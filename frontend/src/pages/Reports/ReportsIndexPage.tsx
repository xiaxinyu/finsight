import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { BarChartOutlined } from '@ant-design/icons'
import { reportConfigs } from '../../config/reports'
import { REPORT_NAV_GROUPS, REPORT_MENU_LABELS } from '../../config/reportNavigation'
import { DataPageLayout } from '../../components/DataPageLayout'
import { ContentCard } from '../../components/ContentCard'
import { ReportsDataQualityBar } from '../../components/ReportsDataQualityBar'
import { useFeatureFlags } from '../../hooks/useFeatureFlags'
import { fetchReportDataQuality } from '../../api/finance'

export function ReportsIndexPage() {
  const { flags } = useFeatureFlags()
  const { data: dq } = useQuery({
    queryKey: ['report-data-quality', 'index'],
    queryFn: () => fetchReportDataQuality('v_transaction_finance_semantics'),
    staleTime: 60_000,
  })

  const hiddenForecast = !flags.forecast
  const hiddenMerchants = !flags.merchantMining

  return (
    <div className="fs-reports-route">
      <ReportsDataQualityBar />
      <DataPageLayout
        title="Reports"
        subtitle="Decision-oriented analytics — pick a report by topic"
        icon={<BarChartOutlined />}
        className="fs-data-page--reports-index"
      >
        {dq && (dq.unclassifiedCount ?? 0) > 0 && (
          <ContentCard className="fs-reports-index-hint" size="small">
            <p>
              <strong>{dq.unclassifiedCount}</strong> unclassified transactions may skew spending and income reports.{' '}
              <Link to="/transactions?emptyConsume=1">Review now</Link>
              {' · '}
              <Link to="/statements/upload">Import more data</Link>
            </p>
          </ContentCard>
        )}

        <div className="fs-reports-index-grid">
          {REPORT_NAV_GROUPS.map((group) => {
            const ids = group.reportIds.filter((id) => {
              if (hiddenForecast && group.key === 'reports-forecast') return false
              if (hiddenMerchants && group.key === 'reports-merchants') return false
              return !!reportConfigs[id]
            })
            if (ids.length === 0) return null
            return (
              <section key={group.key} className="fs-reports-index-group">
                <header className="fs-reports-index-group-head">
                  <h3>{group.label}</h3>
                  <p>{group.description}</p>
                </header>
                <div className="fs-reports-index-cards">
                  {ids.map((id) => {
                    const cfg = reportConfigs[id]
                    const label = REPORT_MENU_LABELS[id] ?? cfg.title
                    return (
                      <Link key={id} to={`/reports/${id}`} className="fs-reports-index-card">
                        <span className="fs-reports-index-card-title">{label}</span>
                        {cfg.subtitle && (
                          <span className="fs-reports-index-card-sub">{cfg.subtitle}</span>
                        )}
                      </Link>
                    )
                  })}
                </div>
              </section>
            )
          })}
        </div>
      </DataPageLayout>
    </div>
  )
}
