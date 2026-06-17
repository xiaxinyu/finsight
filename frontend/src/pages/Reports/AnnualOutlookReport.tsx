import { useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Col, Row, Select, Tag, Typography, message } from 'antd'
import { BarChartOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { Link } from 'react-router-dom'
import { fetchForecast } from '../../api/analytics'
import { saveBudgetLine } from '../../api/finance'
import { useFeatureFlags } from '../../hooks/useFeatureFlags'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { FsChart } from '../../components/FsChart'
import { FsDataTable } from '../../components/FsDataTable'
import { InsightPanel } from '../../components/InsightPanel'
import { CombinedInsightPanel } from '../../components/CombinedInsightPanel'
import { UnifiedDrillDrawer } from '../../components/ReportDrillDrawer'
import { buildReportDrillContext, drillParamsForYearMonth } from '../../components/drilldown/buildDrillContext'
import { useDrillDown } from '../../hooks/useDrillDown'
import { ReportKpiStrip } from '../../components/ReportKpiStrip'
import { formatMoney } from '../../utils/format'
import { budgetGap } from '../../utils/fsTableCells'
import { DeltaMoneyCell, ForecastTag } from '../../components/FsTableCellViews'
import {
  FORECAST_SCENARIOS,
  buildAnnualOutlookChartOption,
  buildAnnualOutlookInsights,
  buildAnnualOutlookKpis,
  buildCategoryForecastChartOption,
  isDeficitMonth,
  scenarioLabel,
  type ForecastScenario,
} from '../../utils/annualOutlook'

type AnnualOutlookReportProps = {
  title: string
  subtitle?: string
}

export function AnnualOutlookReport({ title, subtitle }: AnnualOutlookReportProps) {
  const { flags } = useFeatureFlags()
  const qc = useQueryClient()
  const { open: drillOpen, context: drillContext, openDrill, closeDrill } = useDrillDown()
  const [year, setYear] = useState(dayjs().year())
  const [scenario, setScenario] = useState<ForecastScenario>('base')
  const [applyingBudget, setApplyingBudget] = useState(false)

  const { data, isLoading, isFetching, isError, error } = useQuery({
    queryKey: ['annual-outlook', year, scenario],
    queryFn: () => fetchForecast(year, scenario),
    enabled: flags.forecast,
  })

  const loading = isLoading || isFetching
  const insights = useMemo(() => (data ? buildAnnualOutlookInsights(data) : []), [data])
  const kpis = useMemo(() => (data ? buildAnnualOutlookKpis(data) : []), [data])
  const chartOption = useMemo(() => (data ? buildAnnualOutlookChartOption(data) : {}), [data])
  const categoryChartOption = useMemo(() => {
    if (!data?.categoryForecasts?.length) return {}
    const yearMonths = (data.months || []).map((m) => m.yearMonth)
    return buildCategoryForecastChartOption(data.categoryForecasts, yearMonths)
  }, [data])
  const confidencePct = data?.confidence?.halfWidthPct ?? 10
  const deficitMonths = data?.deficitMonths || []

  const tableCols = useMemo(() => [
    {
      title: 'Month',
      dataIndex: 'yearMonth',
      sortType: 'text' as const,
      render: (ym: string) => (
        isDeficitMonth({ deficitMonths }, ym)
          ? <><span>{ym}</span> <Tag color="red">Deficit</Tag></>
          : ym
      ),
    },
    {
      title: 'Type',
      key: 'kind',
      width: 92,
      cellType: 'forecast' as const,
    },
    { title: 'Income', dataIndex: 'income', cellType: 'money' as const, unit: 'CNY', align: 'right' as const, sortType: 'number' as const },
    { title: 'Expense', dataIndex: 'expense', cellType: 'money' as const, unit: 'CNY', align: 'right' as const, sortType: 'number' as const },
    {
      title: 'Net',
      dataIndex: 'net',
      cellType: 'moneySigned' as const,
      unit: 'CNY',
      align: 'right' as const,
      sortType: 'number' as const,
    },
    {
      title: 'Net range',
      key: 'netRange',
      align: 'right' as const,
      render: (_: unknown, row: { netLower?: number; netUpper?: number; forecast?: boolean }) => (
        row.forecast && row.netLower != null && row.netUpper != null
          ? (
            <span className="fs-table-cell-with-hint">
              <ForecastTag kind="band" />
              <span className="fs-money">{formatMoney(row.netLower)} – {formatMoney(row.netUpper)}</span>
            </span>
          )
          : '—'
      ),
    },
    {
      title: 'Budget',
      dataIndex: 'budgetTarget',
      cellType: 'money' as const,
      unit: 'CNY',
      align: 'right' as const,
      sortType: 'number' as const,
    },
    {
      title: 'Budget gap',
      key: 'budgetGap',
      unit: 'CNY',
      align: 'right' as const,
      sortType: 'number' as const,
      render: (_: unknown, row: { expense: number; budgetTarget?: number }) => {
        const gap = budgetGap(row.expense, row.budgetTarget)
        if (gap == null) return '—'
        return <DeltaMoneyCell value={gap} expenseContext={false} />
      },
    },
  ], [deficitMonths])

  const openMonthDrill = (yearMonth: string) => {
    const month = data?.months?.find((m) => m.yearMonth === yearMonth)
    const deficit = isDeficitMonth({ deficitMonths }, yearMonth)
    openDrill(buildReportDrillContext({
      title: `Annual outlook · ${yearMonth}`,
      metricLabel: `Forecast · ${scenarioLabel(scenario)}`,
      params: drillParamsForYearMonth(yearMonth),
      explanation: [
        month
          ? `Projected net ${formatMoney(month.net)} (income ${formatMoney(month.income)}, expense ${formatMoney(month.expense)}).`
          : `Forecast month ${yearMonth}.`,
        month?.netLower != null && month?.netUpper != null
          ? `Confidence band (±${confidencePct}%): ${formatMoney(month.netLower)} – ${formatMoney(month.netUpper)}.`
          : `Confidence band ±${confidencePct}% around projected net.`,
        deficit
          ? 'This month runs a projected deficit — review bills and discretionary spend.'
          : 'Cash flow looks positive under the selected scenario.',
      ],
      actions: [
        { label: 'Open planning', type: 'planning', path: '/planning' },
        { label: 'Cash risk', type: 'report', path: '/reports/cash-risk' },
      ],
      source: 'annual-outlook',
    }))
  }

  const applyBudgetSuggestion = async () => {
    if (!data?.budgetSuggestion) return
    setApplyingBudget(true)
    try {
      await saveBudgetLine({ bucketKey: 'all', limitAmount: data.budgetSuggestion.monthlyCap })
      qc.invalidateQueries({ queryKey: ['budget-vs-actual'] })
      qc.invalidateQueries({ queryKey: ['cashflow'] })
      message.success(`Monthly budget set to ${formatMoney(data.budgetSuggestion.monthlyCap)}`)
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Failed to save budget')
    } finally {
      setApplyingBudget(false)
    }
  }

  return (
    <DataPageLayout
      title={title}
      subtitle={subtitle}
      icon={<BarChartOutlined />}
      className="fs-data-page--dense fs-data-page--reports"
      toolbar={(
        <div className="fs-cash-risk-toolbar">
          <Select
            size="small"
            value={year}
            style={{ width: 110 }}
            options={[year - 1, year, year + 1].map((y) => ({ value: y, label: String(y) }))}
            onChange={setYear}
          />
          <Select
            size="small"
            value={scenario}
            style={{ width: 150 }}
            options={FORECAST_SCENARIOS.map((s) => ({ value: s.value, label: s.label }))}
            onChange={(v) => setScenario(v as ForecastScenario)}
          />
        </div>
      )}
    >
      {!flags.forecast && (
        <EmptyState title="Forecast module disabled" description="Enable finsight.forecast.enabled to use Annual Outlook." />
      )}

      {flags.forecast && isError && (
        <Alert
          type="error"
          showIcon
          message="Failed to load annual outlook"
          description={error instanceof Error ? error.message : 'Try another scenario.'}
        />
      )}

      {flags.forecast && data && (
        <>
          <ReportKpiStrip items={kpis} />
          <InsightPanel bullets={insights} title="Outlook" />
          <div style={{ marginTop: 16 }}>
            <CombinedInsightPanel
              title="Forecast-linked recommendations"
              subtitle="Deficit months, trend drivers, and merchant evidence in one view"
            />
          </div>

          {data.budgetSuggestion && (
            <ContentCard title="Budget suggestion" size="small" className="fs-annual-outlook-budget">
              <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
                {data.budgetSuggestion.note}
              </Typography.Paragraph>
              <div className="fs-annual-outlook-budget-actions">
                <span>
                  Suggested monthly cap: <strong>{formatMoney(data.budgetSuggestion.monthlyCap)}</strong>
                  {' · '}
                  Annual: <strong>{formatMoney(data.budgetSuggestion.annualCap)}</strong>
                </span>
                <span>
                  <Button size="small" type="primary" loading={applyingBudget} onClick={applyBudgetSuggestion}>
                    Apply to budget
                  </Button>
                  <Link to="/planning" style={{ marginLeft: 8 }}>Open planning</Link>
                </span>
              </div>
            </ContentCard>
          )}

          <Row gutter={[12, 12]} className="fs-report-body">
            <Col xs={24} lg={14}>
              <ContentCard title={`Cash flow: solid = actual, dashed = forecast, dotted = budget (±${confidencePct}% net band)`} size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile="timeSeries"
                  height={360}
                  loading={loading}
                  option={chartOption}
                  empty={<EmptyState compact title="No forecast data" />}
                  onEvents={{
                    click: (p) => {
                      const ym = (p as { name?: string }).name
                      if (ym) openMonthDrill(ym)
                    },
                  }}
                />
              </ContentCard>
            </Col>
            <Col xs={24} lg={10}>
              <FsDataTable
                title="Monthly breakdown"
                columns={tableCols}
                dataSource={data.months}
                rowKey="yearMonth"
                loading={loading}
                summary={{
                  yearMonth: 'Year total',
                  income: data.yearIncome,
                  expense: data.yearExpense,
                  net: data.yearNet,
                }}
                rowExplanation={(row) => {
                  const parts: string[] = []
                  if (row.actual) parts.push('Actual month in the forecast window')
                  else if (row.forecast) parts.push('Projected month — dashed series in chart')
                  if (isDeficitMonth({ deficitMonths }, String(row.yearMonth))) {
                    parts.push('Projected deficit — review bills and discretionary spend')
                  }
                  const gap = budgetGap(row.expense, row.budgetTarget)
                  if (gap != null) {
                    parts.push(gap >= 0
                      ? `Under budget by ${formatMoney(gap)}`
                      : `Over budget by ${formatMoney(-gap)}`)
                  }
                  if (row.netLower != null && row.netUpper != null && row.forecast) {
                    parts.push(`Confidence band ${formatMoney(row.netLower)} – ${formatMoney(row.netUpper)}`)
                  }
                  return parts.length ? parts.join(' · ') : undefined
                }}
                onRow={(record) => ({
                  onClick: () => openMonthDrill(String(record.yearMonth)),
                  style: {
                    cursor: 'pointer',
                    ...(isDeficitMonth({ deficitMonths }, String(record.yearMonth))
                      ? { background: 'rgba(220, 38, 38, 0.06)' }
                      : {}),
                  },
                })}
                scroll={{ y: 340 }}
              />
            </Col>
          </Row>

          {data.categoryForecasts && data.categoryForecasts.length > 0 && (
            <Row gutter={[12, 12]} className="fs-report-body">
              <Col xs={24} lg={14}>
                <ContentCard
                  title={`Top category expense forecasts (±${confidencePct}% band)`}
                  size="small"
                  styles={{ body: { padding: 8 } }}
                >
                  <FsChart
                    profile="timeSeries"
                    height={320}
                    loading={loading}
                    option={categoryChartOption}
                    empty={<EmptyState compact title="No category forecasts" />}
                  />
                </ContentCard>
              </Col>
              <Col xs={24} lg={10}>
                <FsDataTable
                  title="Category totals"
                  columns={[
                    { title: 'Category', dataIndex: 'categoryName', sortType: 'text' as const },
                    {
                      title: 'Year total',
                      dataIndex: 'yearTotal',
                      cellType: 'money' as const,
                      unit: 'CNY',
                      align: 'right' as const,
                      sortType: 'number' as const,
                    },
                    {
                      title: 'Range',
                      key: 'range',
                      align: 'right' as const,
                      render: (_: unknown, row: { yearTotalLower?: number; yearTotalUpper?: number }) => (
                        row.yearTotalLower != null && row.yearTotalUpper != null
                          ? (
                            <span className="fs-table-cell-with-hint">
                              <ForecastTag kind="band" />
                              <span className="fs-money">{formatMoney(row.yearTotalLower)} – {formatMoney(row.yearTotalUpper)}</span>
                            </span>
                          )
                          : '—'
                      ),
                    },
                    {
                      title: 'Share',
                      dataIndex: 'sharePct',
                      cellType: 'contribution' as const,
                      align: 'right' as const,
                      sortType: 'number' as const,
                    },
                  ]}
                  dataSource={data.categoryForecasts}
                  rowKey="categoryCode"
                  loading={loading}
                  scroll={{ y: 280 }}
                />
              </Col>
            </Row>
          )}
        </>
      )}

      <UnifiedDrillDrawer open={drillOpen} context={drillContext} onClose={closeDrill} />
    </DataPageLayout>
  )
}
