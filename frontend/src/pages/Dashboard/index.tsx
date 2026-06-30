import { useMemo, useState } from 'react'
import dayjs from 'dayjs'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Alert, Col, Progress, Row, Typography } from 'antd'
import {
  DashboardOutlined,
  FallOutlined,
  FundOutlined,
  LineChartOutlined,
  RiseOutlined,
} from '@ant-design/icons'
import { homeSummary } from '../../api/report'
import { cashflowMetrics, decisionCards, financialPulse } from '../../api/finance'
import { advisorFeedback, advisorRecommendations, fetchMetricPeriodSummary, fetchSemanticBreakdown } from '../../api/analytics'
import type { AdvisorCard } from '../../api/analytics'
import { FsChart } from '../../components/FsChart'
import { UnifiedDrillDrawer } from '../../components/ReportDrillDrawer'
import { buildDashboardDrillContext, drillParamsForMonth, drillParamsForSemanticTag } from '../../components/drilldown/buildDrillContext'
import { useDrillDown } from '../../hooks/useDrillDown'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { PageSkeleton } from '../../components/PageSkeleton'
import { AdvisorStrip } from '../../components/AdvisorStrip'
import { AdvisorEvidenceDrawer } from '../../components/AdvisorEvidenceDrawer'
import { DashboardInsightStrip } from '../../components/DashboardInsightStrip'
import { DashboardQualityStrip } from '../../components/DashboardQualityStrip'
import { AccountBalancePanel } from '../../components/AccountBalancePanel'
import { finsightColors } from '../../styles/finsight-tokens'
import { PeriodRangePicker } from '../../components/PeriodRangePicker'
import { periodToStrings } from '../../utils/periodStrings'
import { formatMoney } from '../../utils/format'
import { defaultPeriodRange, formatPeriodPreview, type PeriodRange } from '../../utils/periodPresets'
import { isDrillableSemanticTag, topSemanticRows, type SemanticBreakdownRow } from '../../utils/semanticBreakdownReport'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { useFeatureFlags } from '../../hooks/useFeatureFlags'
import { ANALYTICS_STALE_MS, QUERY_KEYS } from '../../constants/queryKeys'
import { DASHBOARD_METRIC_HINTS, MetricExplanation } from '../../components/MetricExplanation'
import { mapDashboardPeriodTotals } from '../../utils/dashboardMetrics'
import { REPORT_METRICS_SOURCE } from '../../utils/reportTaxonomy'

function savingsRateLabel(income: number, net: number): string {
  if (income <= 0) return '—'
  return `${((net / income) * 100).toFixed(1)}%`
}

function dataTrustScore(unclassified: number): number {
  const unclsPenalty = unclassified > 0 ? Math.min(90, Math.round(unclassified / 5)) : 0
  return Math.max(0, 100 - unclsPenalty)
}

export function DashboardPage() {
  const { flags } = useFeatureFlags()
  const { open: drillOpen, context: drillContext, openDrill, closeDrill } = useDrillDown()
  const [period, setPeriod] = useState<PeriodRange>(() => defaultPeriodRange())
  const [evidenceCard, setEvidenceCard] = useState<AdvisorCard | null>(null)
  const periodKey = periodToStrings(period)
  const chartHeight = Math.min(useViewportTableHeight(280), 360)

  const { data: summary, isLoading, isError, error } = useQuery({
    queryKey: ['home-summary', periodKey.start, periodKey.end],
    queryFn: () => homeSummary(dayjs().year(), periodKey.start ? periodKey : undefined),
  })

  const { data: semanticPeriod, isFetching: semanticLoading } = useQuery({
    queryKey: ['dash-semantic', periodKey.start, periodKey.end],
    queryFn: () => fetchMetricPeriodSummary(periodKey.start || undefined, periodKey.end || undefined),
    staleTime: ANALYTICS_STALE_MS,
  })

  const { data: semanticBreakdown, isFetching: breakdownLoading } = useQuery({
    queryKey: ['dash-breakdown', periodKey.start, periodKey.end],
    queryFn: () => fetchSemanticBreakdown(periodKey.start || undefined, periodKey.end || undefined, { scope: 'expense' }),
    staleTime: ANALYTICS_STALE_MS,
  })

  const periodTotals = useMemo(
    () => mapDashboardPeriodTotals(semanticPeriod),
    [semanticPeriod],
  )

  const expenseConcentration = useMemo(() => {
    const rows = [...(semanticBreakdown?.rows ?? [])].sort((a, b) => b.amount - a.amount)
    const total = semanticBreakdown?.expenseTotal ?? 0
    const top3 = rows.slice(0, 3)
    const top3Total = top3.reduce((s, r) => s + r.amount, 0)
    return {
      top3,
      top3Share: total > 0 ? (top3Total / total) * 100 : 0,
      expenseTotal: total,
      count: rows.length,
    }
  }, [semanticBreakdown])

  const { data: pulse, isError: pulseError, error: pulseErr } = useQuery({
    queryKey: ['financial-pulse'],
    queryFn: financialPulse,
  })
  const { data: cashflow } = useQuery({
    queryKey: ['cashflow'],
    queryFn: cashflowMetrics,
  })
  const { data: advisorCards, isError: cardsError, error: cardsErr, refetch: refetchAdvisor } = useQuery({
    queryKey: QUERY_KEYS.advisorRecommendations,
    queryFn: advisorRecommendations,
    enabled: flags.advisor,
    staleTime: ANALYTICS_STALE_MS,
  })
  const { data: legacyCards } = useQuery({
    queryKey: ['decision-cards'],
    queryFn: decisionCards,
    enabled: !flags.advisor || !advisorCards?.length,
  })

  const income = periodTotals.realIncome
  const expense = periodTotals.consumptionExpense
  const net = periodTotals.net
  const savingsLabel = savingsRateLabel(income, net)
  const healthScore = (summary?.health_score || summary?.healthScore) as Record<string, number> | undefined

  const dq = pulse?.dataQuality
  const unclassified = dq?.unclassifiedCount ?? 0
  const trustPct = dataTrustScore(unclassified)

  const pieData = useMemo(
    () => topSemanticRows((semanticBreakdown?.rows ?? []) as SemanticBreakdownRow[], 8).map((r) => ({
      name: r.classification || r.label,
      value: r.amount,
      tagId: r.tagId,
      classification: r.classification || r.label,
    })),
    [semanticBreakdown?.rows],
  )

  const cashflowOption = useMemo(() => {
    const months = periodTotals.months
    return {
      title: { text: `Cash flow · ${formatPeriodPreview(period[0], period[1])}`, left: 0, textStyle: { fontSize: 13, fontWeight: 600 } },
      legend: { data: ['Real income', 'Consumption', 'Net'], top: 4, right: 0, textStyle: { fontSize: 11 } },
      grid: { left: 48, right: 16, top: 48, bottom: 28 },
      tooltip: { trigger: 'axis' as const },
      xAxis: { type: 'category' as const, data: months.map((m) => m.month), axisLabel: { fontSize: 10 } },
      yAxis: { type: 'value' as const, axisLabel: { fontSize: 10 } },
      series: [
        { name: 'Real income', type: 'bar' as const, data: months.map((m) => m.realIncome), itemStyle: { color: '#16a34a' }, barMaxWidth: 18 },
        { name: 'Consumption', type: 'bar' as const, data: months.map((m) => m.consumptionExpense), itemStyle: { color: '#ea580c' }, barMaxWidth: 18 },
        {
          name: 'Net',
          type: 'line' as const,
          smooth: true,
          data: months.map((m) => m.net),
          itemStyle: { color: '#2563eb' },
          lineStyle: { width: 2 },
        },
      ],
    }
  }, [period, periodTotals.months])

  const loadError = isError ? error : pulseError ? pulseErr : cardsError ? cardsErr : null
  const loading = isLoading || semanticLoading || breakdownLoading
  const periodLabel = formatPeriodPreview(period[0], period[1])
  const needsOnboarding = income === 0 && expense === 0 && Number(pulse?.liquidAssets || 0) === 0

  const openCashflowDrill = (monthName?: string) => {
    const year = period[1].year()
    const params = monthName
      ? drillParamsForMonth(monthName, year, 'expense')
      : {
          transactionDateStartStr: periodKey.start,
          transactionDateEndStr: periodKey.end,
          txnTypes: 'expense',
        }
    if (!params.transactionDateStartStr) return
    openDrill(buildDashboardDrillContext({
      title: monthName ? `Cash flow · ${monthName} ${year}` : `Cash flow · ${periodLabel}`,
      metricLabel: monthName ? `${monthName} net cash flow` : 'Period cash flow',
      params,
      explanation: monthName
        ? [`Net cash flow for ${monthName} ${year} within your selected period.`]
        : [
            `Income ${formatMoney(income)} vs expense ${formatMoney(expense)} (${periodLabel}).`,
            net >= 0
              ? `Net surplus ${formatMoney(net)} — savings rate ${savingsLabel}.`
              : `Net deficit ${formatMoney(Math.abs(net))} — review expense concentration.`,
          ],
    }))
  }

  const openSemanticDrill = (tagId: string, label: string) => {
    if (!periodKey.start || !periodKey.end || !isDrillableSemanticTag(tagId)) return
    openDrill(buildDashboardDrillContext({
      title: `${label} · ${periodLabel}`,
      metricLabel: label,
      params: drillParamsForSemanticTag(tagId, periodKey.start, periodKey.end, 'expense'),
      explanation: [
        `${label} is among your top expense classifications this period.`,
        expenseConcentration.top3Share >= 50
          ? `Top-3 classifications account for ${expenseConcentration.top3Share.toFixed(1)}% of spend — high concentration.`
          : 'Spending is relatively diversified across classifications.',
      ],
    }))
  }

  return (
    <DataPageLayout
      title="Financial Pulse"
      subtitle={`Cash flow, liquidity, and ledger quality · ${periodLabel}`}
      icon={<DashboardOutlined />}
      className="fs-data-page--dense fs-data-page--dashboard"
      actions={(
        <PeriodRangePicker
          size="small"
          value={period}
          onChange={(range) => setPeriod(range)}
        />
      )}
    >
      {loadError && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 8 }}
          message="Failed to load dashboard data"
          description={loadError instanceof Error ? loadError.message : 'Please sign in again.'}
        />
      )}
      {needsOnboarding && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 8 }}
          message="Get started with FinSight"
          description={(
            <ol style={{ margin: '4px 0 0', paddingLeft: 18 }}>
              <li><Link to="/admin/cards">Add bank cards</Link> in Admin</li>
              <li><Link to="/statements/upload">Import a statement</Link> to populate transactions</li>
              <li><Link to="/planning">Set a monthly budget</Link> and add recurring bills</li>
            </ol>
          )}
        />
      )}
      {loading && !summary ? (
        <PageSkeleton />
      ) : (
         <>
          {pulse?.dataQuality && (
            <DashboardQualityStrip
              unclassified={unclassified}
              transfers={dq?.transferPairCount ?? 0}
              dataTrustPct={trustPct}
            />
          )}

          <div className="fs-dash-kpi-strip">
            <div className="fs-dash-kpi-card">
              <MetricExplanation className="fs-dash-kpi-label" label="Real income" hint={DASHBOARD_METRIC_HINTS.realIncome} />
              <span className="fs-dash-kpi-value fs-dash-kpi-value--income">{formatMoney(income)}</span>
              <span className="fs-dash-kpi-hint">{periodLabel} · {periodTotals.metricsSource || REPORT_METRICS_SOURCE}</span>
            </div>
            <div className="fs-dash-kpi-card">
              <MetricExplanation className="fs-dash-kpi-label" label="Consumption" hint={DASHBOARD_METRIC_HINTS.consumptionExpense} />
              <span className="fs-dash-kpi-value fs-dash-kpi-value--expense">{formatMoney(expense)}</span>
              <span className="fs-dash-kpi-hint">{periodLabel}</span>
            </div>
            <div className="fs-dash-kpi-card">
              <MetricExplanation className="fs-dash-kpi-label" label="Net cashflow" hint={DASHBOARD_METRIC_HINTS.netCashflow} />
              <span className={`fs-dash-kpi-value${net >= 0 ? ' fs-dash-kpi-value--income' : ' fs-dash-kpi-value--expense'}`}>
                {formatMoney(net)}
              </span>
              <span className="fs-dash-kpi-hint">Savings {savingsLabel}</span>
            </div>
            <div className="fs-dash-kpi-card">
              <span className="fs-dash-kpi-label">Liquid assets</span>
              <span className="fs-dash-kpi-value">{formatMoney(Number(pulse?.liquidAssets || 0))}</span>
              <span className="fs-dash-kpi-hint">
                Runway {Number(cashflow?.runwayMonths || 0).toFixed(1)} mo
              </span>
            </div>
            <div className="fs-dash-kpi-card">
              <span className="fs-dash-kpi-label">Daily burn</span>
              <span className="fs-dash-kpi-value">{formatMoney(Number(cashflow?.burnRateDaily || 0))}</span>
              <span className="fs-dash-kpi-hint">30-day average</span>
            </div>
            <div className="fs-dash-kpi-card">
              <span className="fs-dash-kpi-label">Top-3 spend share</span>
              <span className="fs-dash-kpi-value">{expenseConcentration.top3Share.toFixed(1)}%</span>
              <span className="fs-dash-kpi-hint">
                {expenseConcentration.top3.map((c) => c.classification || c.label).join(' · ') || '—'}
              </span>
            </div>
          </div>

          {flags.advisor ? (
            <>
              <AdvisorStrip
                cards={advisorCards || []}
                onOpenEvidence={setEvidenceCard}
                onAccept={async (id) => {
                  await advisorFeedback(id, 'accept')
                  refetchAdvisor()
                }}
                onSnooze={async (id) => {
                  await advisorFeedback(id, 'snooze')
                  refetchAdvisor()
                }}
                onDismiss={async (id) => {
                  await advisorFeedback(id, 'dismiss')
                  refetchAdvisor()
                }}
              />
              {!advisorCards?.length && <DashboardInsightStrip cards={legacyCards || []} />}
            </>
          ) : (
            <DashboardInsightStrip cards={legacyCards || []} />
          )}

          <Row gutter={[12, 12]}>
            <Col xs={24} lg={14}>
              <ContentCard title="Cash flow trend" size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile="bar"
                  height={chartHeight}
                  loading={loading}
                  empty={<EmptyState compact title="No cash flow data" description={`No transactions in ${periodLabel}.`} />}
                  option={cashflowOption}
                  onEvents={{
                    click: (p) => {
                      const name = (p as { name?: string }).name
                      if (name) openCashflowDrill(name)
                    },
                  }}
                />
              </ContentCard>
            </Col>
            <Col xs={24} lg={10}>
              <ContentCard title="Expense by classification" size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile="donut"
                  height={chartHeight}
                  loading={loading}
                  empty={<EmptyState compact title="No classifications" description="Classify expenses to unlock reporting breakdown." />}
                  option={{
                    series: [{
                      type: 'pie',
                      radius: ['46%', '72%'],
                      center: ['50%', '52%'],
                      data: pieData,
                      label: { fontSize: 11, formatter: '{b}\n{d}%' },
                    }],
                  }}
                  onEvents={{
                    click: (p) => {
                      const evt = p as { name?: string; data?: { tagId?: string; classification?: string } }
                      const tagId = evt.data?.tagId
                      if (tagId && evt.name) {
                        openSemanticDrill(tagId, evt.data?.classification ?? evt.name)
                      }
                    },
                  }}
                />
                {expenseConcentration.expenseTotal > 0 && (
                  <Typography.Paragraph type="secondary" className="fs-dash-analysis-note">
                    {expenseConcentration.top3Share >= 50
                      ? `Spending is concentrated: top 3 classifications account for ${expenseConcentration.top3Share.toFixed(1)}% of ${formatMoney(expenseConcentration.expenseTotal)}. Review caps in Planning.`
                      : `Expense spread across ${expenseConcentration.count} classifications — largest is ${expenseConcentration.top3[0]?.classification || 'n/a'} (${formatMoney(expenseConcentration.top3[0]?.amount || 0)}).`}
                  </Typography.Paragraph>
                )}
              </ContentCard>
            </Col>
          </Row>

          <Row gutter={[12, 12]}>
            <Col xs={24} lg={pulse?.accounts?.length ? 10 : 0}>
              {pulse?.accounts && pulse.accounts.length > 0 && (
                <ContentCard title="Liquidity" size="small" styles={{ body: { padding: '10px 12px' } }}>
                  <AccountBalancePanel accounts={pulse.accounts} />
                  <div className="fs-dash-liquidity-meta">
                    <span>Safe to spend <strong>{formatMoney(Number(cashflow?.safeToSpend || 0))}</strong></span>
                    <span>Bills reserved <strong>{formatMoney(Number(cashflow?.billsReserved || 0))}</strong></span>
                  </div>
                </ContentCard>
              )}
            </Col>
            {healthScore && (
              <Col xs={24} lg={6}>
                <ContentCard title={`Health score (${period[0].year()})`} size="small" styles={{ body: { padding: '10px 12px' } }}>
                  <div className="fs-dash-health-score">
                    <span className="fs-dash-health-value">{Math.round(Number(healthScore.total || 0))}</span>
                    <span className="fs-dash-health-max">/ 100</span>
                  </div>
                  {Object.entries(healthScore).filter(([k]) => k !== 'total' && k !== 'emergencyMonths').map(([k, v]) => (
                    <div key={k} className="fs-dash-health-dim">
                      <div className="fs-dash-health-dim-label">{k.replace(/_/g, ' ')}</div>
                      <Progress percent={Math.min(100, Number(v))} size="small" showInfo={false} strokeColor={k === 'debtPressure' && Number(v) > 50 ? finsightColors.expense : undefined} />
                    </div>
                  ))}
                </ContentCard>
              </Col>
            )}
            <Col xs={24} lg={healthScore ? 8 : 14}>
              <ContentCard title="Analysis shortcuts" size="small" styles={{ body: { padding: '10px 12px' } }}>
                <div className="fs-dash-shortcuts">
                  <Link to="/reports/cashflow" className="fs-dash-shortcut">
                    <FundOutlined />
                    <span>Cashflow report</span>
                    <small>Monthly surplus & deficit months</small>
                  </Link>
                  <Link to="/reports/spending-drift" className="fs-dash-shortcut">
                    <RiseOutlined />
                    <span>Spending drift</span>
                    <small>Period-over-period</small>
                  </Link>
                  <Link to="/reports/budget-vs-actual" className="fs-dash-shortcut">
                    <FallOutlined />
                    <span>Budget vs actual</span>
                    <small>Utilization by bucket</small>
                  </Link>
                  <Link to="/wealth" className="fs-dash-shortcut">
                    <LineChartOutlined />
                    <span>Wealth snapshot</span>
                    <small>Net worth & YTD savings</small>
                  </Link>
                </div>
              </ContentCard>
            </Col>
          </Row>
        </>
      )}
      <UnifiedDrillDrawer open={drillOpen} context={drillContext} onClose={closeDrill} />
      <AdvisorEvidenceDrawer
        open={!!evidenceCard}
        card={evidenceCard}
        onClose={() => setEvidenceCard(null)}
      />
    </DataPageLayout>
  )
}
