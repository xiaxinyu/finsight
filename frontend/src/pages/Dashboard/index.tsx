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
import { homeSummary, fetchReport } from '../../api/report'
import { cashflowMetrics, decisionCards, financialPulse } from '../../api/finance'
import { advisorFeedback, advisorRecommendations } from '../../api/analytics'
import type { AdvisorCard } from '../../api/analytics'
import { FsChart } from '../../components/FsChart'
import { UnifiedDrillDrawer } from '../../components/ReportDrillDrawer'
import { buildDashboardDrillContext, drillParamsForCategory, drillParamsForMonth } from '../../components/drilldown/buildDrillContext'
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
import { PeriodRangePicker, periodToStrings } from '../../components/PeriodRangePicker'
import { formatMoney, MONTH_NAMES } from '../../utils/format'
import { defaultPeriodRange, formatPeriodPreview, type PeriodRange } from '../../utils/periodPresets'
import { rollupToLevel1 } from '../../utils/reportAnalytics'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { useFeatureFlags } from '../../hooks/useFeatureFlags'

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

  const { data: periodReport, isFetching: totalsLoading } = useQuery({
    queryKey: ['dash-period', periodKey],
    queryFn: async () => {
      const base: Record<string, string> = {}
      if (periodKey.start) base.transactionDateStartStr = periodKey.start
      if (periodKey.end) base.transactionDateEndStr = periodKey.end
      const [inc, exp, cats] = await Promise.all([
        fetchReport('/transaction-report/month-income', { ...base, txnTypes: 'income' }),
        fetchReport('/transaction-report/month-expense', { ...base, txnTypes: 'expense' }),
        fetchReport('/transaction-report/consume', { ...base, txnTypes: 'expense' }),
      ])
      const income = inc.reduce((s, r) => s + r.value, 0)
      const expense = exp.reduce((s, r) => s + r.value, 0)
      const months = MONTH_NAMES.map((name, i) => ({
        month: name,
        income: Number(inc[i]?.value || 0),
        expense: Number(exp[i]?.value || 0),
      }))
      const rolled = rollupToLevel1(cats)
      const topCats = rolled.filter((r) => r.value > 0).sort((a, b) => b.value - a.value)
      const expenseTotal = topCats.reduce((s, r) => s + r.value, 0)
      const top3 = topCats.slice(0, 3)
      const top3Share = expenseTotal > 0 ? (top3.reduce((s, r) => s + r.value, 0) / expenseTotal) * 100 : 0
      return { income, expense, months, topCats: top3, top3Share, expenseTotal, txnCount: topCats.length }
    },
  })

  const { data: pulse, isError: pulseError, error: pulseErr } = useQuery({
    queryKey: ['financial-pulse'],
    queryFn: financialPulse,
  })
  const { data: cashflow } = useQuery({
    queryKey: ['cashflow'],
    queryFn: cashflowMetrics,
  })
  const { data: advisorCards, isError: cardsError, error: cardsErr, refetch: refetchAdvisor } = useQuery({
    queryKey: ['advisor-recommendations'],
    queryFn: advisorRecommendations,
    enabled: flags.advisor,
  })
  const { data: legacyCards } = useQuery({
    queryKey: ['decision-cards'],
    queryFn: decisionCards,
    enabled: !flags.advisor || !advisorCards?.length,
  })

  const income = Number(periodReport?.income ?? 0)
  const expense = Number(periodReport?.expense ?? 0)
  const net = income - expense
  const savingsLabel = savingsRateLabel(income, net)
  const healthScore = (summary?.health_score || summary?.healthScore) as Record<string, number> | undefined

  const dq = pulse?.dataQuality
  const unclassified = dq?.unclassifiedCount ?? 0
  const trustPct = dataTrustScore(unclassified)

  const pieData = useMemo(
    () => (periodReport?.topCats || []).map((r) => ({ name: r.key, value: r.value })),
    [periodReport?.topCats],
  )

  const cashflowOption = useMemo(() => {
    const months = periodReport?.months || []
    return {
      title: { text: `Cash flow · ${formatPeriodPreview(period[0], period[1])}`, left: 0, textStyle: { fontSize: 13, fontWeight: 600 } },
      legend: { data: ['Income', 'Expense', 'Net'], top: 4, right: 0, textStyle: { fontSize: 11 } },
      grid: { left: 48, right: 16, top: 48, bottom: 28 },
      tooltip: { trigger: 'axis' as const },
      xAxis: { type: 'category' as const, data: months.map((m) => m.month), axisLabel: { fontSize: 10 } },
      yAxis: { type: 'value' as const, axisLabel: { fontSize: 10 } },
      series: [
        { name: 'Income', type: 'bar' as const, data: months.map((m) => m.income), itemStyle: { color: '#16a34a' }, barMaxWidth: 18 },
        { name: 'Expense', type: 'bar' as const, data: months.map((m) => m.expense), itemStyle: { color: '#ea580c' }, barMaxWidth: 18 },
        {
          name: 'Net',
          type: 'line' as const,
          smooth: true,
          data: months.map((m) => m.income - m.expense),
          itemStyle: { color: '#2563eb' },
          lineStyle: { width: 2 },
        },
      ],
    }
  }, [period, periodReport?.months])

  const loadError = isError ? error : pulseError ? pulseErr : cardsError ? cardsErr : null
  const loading = isLoading || totalsLoading
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

  const openCategoryDrill = (categoryName: string) => {
    if (!periodKey.start || !periodKey.end) return
    openDrill(buildDashboardDrillContext({
      title: `${categoryName} · ${periodLabel}`,
      metricLabel: categoryName,
      params: drillParamsForCategory(categoryName, periodKey.start, periodKey.end),
      explanation: [
        `${categoryName} is among your top expense categories this period.`,
        (periodReport?.top3Share ?? 0) >= 50
          ? `Top-3 categories account for ${(periodReport?.top3Share ?? 0).toFixed(1)}% of spend — high concentration.`
          : 'Spending is relatively diversified across categories.',
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
              <span className="fs-dash-kpi-label">Income</span>
              <span className="fs-dash-kpi-value fs-dash-kpi-value--income">{formatMoney(income)}</span>
              <span className="fs-dash-kpi-hint">{periodLabel}</span>
            </div>
            <div className="fs-dash-kpi-card">
              <span className="fs-dash-kpi-label">Expense</span>
              <span className="fs-dash-kpi-value fs-dash-kpi-value--expense">{formatMoney(expense)}</span>
              <span className="fs-dash-kpi-hint">{periodLabel}</span>
            </div>
            <div className="fs-dash-kpi-card">
              <span className="fs-dash-kpi-label">Net</span>
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
              <span className="fs-dash-kpi-value">{(periodReport?.top3Share ?? 0).toFixed(1)}%</span>
              <span className="fs-dash-kpi-hint">
                {(periodReport?.topCats || []).map((c) => c.key).join(' · ') || '—'}
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
              <ContentCard title="Expense concentration" size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile="donut"
                  height={chartHeight}
                  loading={loading}
                  empty={<EmptyState compact title="No categories" description="Classify expenses to unlock category insights." />}
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
                      const name = (p as { name?: string }).name
                      if (name) openCategoryDrill(name)
                    },
                  }}
                />
                {periodReport && periodReport.expenseTotal > 0 && (
                  <Typography.Paragraph type="secondary" className="fs-dash-analysis-note">
                    {(periodReport.top3Share ?? 0) >= 50
                      ? `Spending is concentrated: top 3 categories account for ${periodReport.top3Share.toFixed(1)}% of ${formatMoney(periodReport.expenseTotal)}. Review caps in Planning.`
                      : `Expense spread across ${periodReport.txnCount} categories — top category is ${periodReport.topCats[0]?.key || 'n/a'} (${formatMoney(periodReport.topCats[0]?.value || 0)}).`}
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
