import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Col, Row, Select } from 'antd'
import { BarChartOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { fetchTrends } from '../../api/analytics'
import type { TrendItem, TrendMover } from '../../utils/trendChanges'
import { useFeatureFlags } from '../../hooks/useFeatureFlags'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { FsChart } from '../../components/FsChart'
import { FsDataTable } from '../../components/FsDataTable'
import { InsightPanel } from '../../components/InsightPanel'
import { DataQualityStrip } from '../../components/DataQualityStrip'
import { UnifiedDrillDrawer } from '../../components/ReportDrillDrawer'
import { buildReportDrillContext } from '../../components/drilldown/buildDrillContext'
import { useDrillDown } from '../../hooks/useDrillDown'
import { ReportKpiStrip } from '../../components/ReportKpiStrip'
import { formatMoney } from '../../utils/format'
import { DeltaPercentCell } from '../../components/FsTableCellViews'
import {
  buildCategoryContributorChart,
  buildMerchantContributorChart,
  buildTrendInsights,
  buildTrendKpis,
  moverFromTo,
  trendTypeLabel,
} from '../../utils/trendChanges'

type TrendChangesReportProps = {
  title: string
  subtitle?: string
}

export function TrendChangesReport({ title, subtitle }: TrendChangesReportProps) {
  const { flags } = useFeatureFlags()
  const { open: drillOpen, context: drillContext, openDrill, closeDrill } = useDrillDown()
  const [toYear, setToYear] = useState(dayjs().year())
  const fromYear = toYear - 1

  const { data, isLoading, isFetching, isError, error } = useQuery({
    queryKey: ['trend-changes', fromYear, toYear],
    queryFn: () => fetchTrends(fromYear, toYear),
    enabled: flags.forecast,
  })

  const loading = isLoading || isFetching
  const kpis = useMemo(() => (data ? buildTrendKpis(data) : []), [data])
  const insights = useMemo(() => (data ? buildTrendInsights(data) : []), [data])
  const categoryChart = useMemo(() => (data ? buildCategoryContributorChart(data) : {}), [data])
  const merchantChart = useMemo(() => (data ? buildMerchantContributorChart(data) : {}), [data])

  const openTrendDrill = (label: string, item: TrendItem | TrendMover, explanation: string[]) => {
    openDrill(buildReportDrillContext({
      title: `${label} · ${fromYear}→${toYear}`,
      metricLabel: label,
      params: item.drillDown || {},
      explanation,
      source: 'report',
      provenance: {
        reportId: 'trend-changes',
        sourceView: `${label} contributor`,
        aggregateTotal: 'deltaAmount' in item ? item.deltaAmount : undefined,
      },
    }))
  }

  const fromToCols = [
    {
      title: 'From',
      key: 'fromAmount',
      align: 'right' as const,
      sortType: 'number' as const,
      render: (_: unknown, row: TrendMover) => {
        const { from } = moverFromTo(row)
        return from == null ? '—' : formatMoney(from)
      },
    },
    {
      title: 'To',
      key: 'toAmount',
      align: 'right' as const,
      sortType: 'number' as const,
      render: (_: unknown, row: TrendMover) => {
        const { to } = moverFromTo(row)
        return to == null ? '—' : formatMoney(to)
      },
    },
  ]

  const moverCols = [
    {
      title: 'Name',
      dataIndex: 'label',
      sortType: 'text' as const,
      ellipsis: true,
      render: (_: unknown, row: TrendMover) => row.label || row.categoryName || row.categoryCode,
    },
    ...fromToCols,
    {
      title: 'Delta',
      dataIndex: 'deltaAmount',
      cellType: 'deltaMoney' as const,
      unit: 'CNY',
      align: 'right' as const,
      sortType: 'number' as const,
    },
    {
      title: 'Change %',
      dataIndex: 'deltaPercent',
      align: 'right' as const,
      sortType: 'number' as const,
      render: (_: unknown, row: TrendMover) => (
        <DeltaPercentCell
          value={Number(row.deltaPercent ?? row.pctChange ?? 0)}
          amount={row.deltaAmount}
        />
      ),
    },
    {
      title: 'Contribution',
      dataIndex: 'contributionPct',
      cellType: 'contribution' as const,
      align: 'right' as const,
      sortType: 'number' as const,
    },
  ]

  const moverExplanation = (row: TrendMover) => {
    const { from, to } = moverFromTo(row)
    const pct = Number(row.deltaPercent ?? row.pctChange ?? 0)
    const name = row.label || row.categoryName || 'Mover'
    const parts = [
      from != null && to != null ? `${name}: ${formatMoney(from)} → ${formatMoney(to)}` : name,
      `Δ ${formatMoney(row.deltaAmount)} (${pct >= 0 ? '+' : ''}${pct.toFixed(1)}%)`,
      `${Number(row.contributionPct).toFixed(1)}% of expense shift`,
    ]
    return parts.join(' · ')
  }

  const openMoverDrill = (row: TrendMover, kind: 'category' | 'merchant') => {
    const name = String(row.label || row.categoryName || row.categoryCode)
    openTrendDrill(name, row, [
      `${kind === 'category' ? 'Category' : 'Merchant'} ${name} moved ${formatMoney(row.deltaAmount)} YoY.`,
      moverExplanation(row),
    ])
  }

  return (
    <DataPageLayout
      title={title}
      subtitle={subtitle}
      icon={<BarChartOutlined />}
      className="fs-data-page--dense fs-data-page--reports"
      toolbar={(
        <Select
          size="small"
          value={toYear}
          style={{ width: 110 }}
          options={[toYear - 1, toYear, toYear + 1].map((y) => ({ value: y, label: String(y) }))}
          onChange={setToYear}
        />
      )}
    >
      <DataQualityStrip metricsSource="report_sql" compact />
      {!flags.forecast && (
        <EmptyState title="Forecast module disabled" description="Enable finsight.forecast.enabled to use trend decomposition." />
      )}

      {flags.forecast && isError && (
        <Alert type="error" showIcon message="Failed to load trends" description={error instanceof Error ? error.message : 'Try another year.'} />
      )}

      {flags.forecast && data && (
        <>
          <ReportKpiStrip items={kpis} />
          <InsightPanel bullets={insights} title="What changed" />

          <Row gutter={[12, 12]} className="fs-report-body">
            <Col xs={24} lg={12}>
              <ContentCard title="Category contribution to expense change" size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile="categoryBar"
                  height={280}
                  loading={loading}
                  option={categoryChart}
                  empty={<EmptyState compact title="No category movers" />}
                  onEvents={{
                    click: (p) => {
                      const name = (p as { name?: string }).name
                      const cat = data.topCategoryGrowth.find((c) => c.categoryName === name)
                      if (cat) openMoverDrill(cat, 'category')
                    },
                  }}
                />
              </ContentCard>
            </Col>
            <Col xs={24} lg={12}>
              <ContentCard title="Merchant contribution to expense change" size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile="categoryBar"
                  height={280}
                  loading={loading}
                  option={merchantChart}
                  empty={<EmptyState compact title="No merchant movers" />}
                  onEvents={{
                    click: (p) => {
                      const name = (p as { name?: string }).name
                      const merchant = data.topMerchantMovers.find((m) => m.label === name)
                      if (merchant) openMoverDrill(merchant, 'merchant')
                    },
                  }}
                />
              </ContentCard>
            </Col>
          </Row>

          <Row gutter={[12, 12]} className="fs-report-body">
            <Col xs={24} lg={14}>
              <FsDataTable
                title="Trend summary"
                columns={[
                  { title: 'Type', dataIndex: 'type', width: 130, render: (t: string) => trendTypeLabel(t) },
                  { title: 'Label', dataIndex: 'label', sortType: 'text' },
                  ...fromToCols,
                  {
                    title: 'Delta',
                    dataIndex: 'deltaAmount',
                    cellType: 'deltaMoney',
                    unit: 'CNY',
                    align: 'right',
                    sortType: 'number',
                  },
                  {
                    title: 'Change %',
                    dataIndex: 'deltaPercent',
                    cellType: 'deltaPercent',
                    deltaAmountKey: 'deltaAmount',
                    align: 'right',
                    sortType: 'number',
                  },
                  {
                    title: 'Contribution',
                    dataIndex: 'contributionPct',
                    cellType: 'contribution',
                    align: 'right',
                    sortType: 'number',
                  },
                ]}
                dataSource={data.trends}
                rowKey={(r) => `${r.type}-${r.label}`}
                loading={loading}
                rowExplanation={(record) => {
                  const { from, to } = moverFromTo(record, data.summary.expense)
                  const parts = [
                    from != null && to != null
                      ? `${record.label}: ${formatMoney(from)} → ${formatMoney(to)}`
                      : record.label,
                    `${formatMoney(record.deltaAmount)} (${record.deltaPercent.toFixed(1)}% change)`,
                  ]
                  if (record.contributionPct) {
                    parts.push(`${record.contributionPct.toFixed(1)}% of expense shift`)
                  }
                  return parts.join(' · ')
                }}
                onRow={(record) => ({
                  onClick: () => openTrendDrill(record.label, record, [
                    `${record.label}: ${formatMoney(record.deltaAmount)} (${record.deltaPercent.toFixed(1)}% change).`,
                    record.contributionPct
                      ? `${record.contributionPct.toFixed(1)}% of total expense shift.`
                      : 'Open breakdown and transactions for this slice.',
                  ]),
                  style: { cursor: 'pointer' },
                })}
                scroll={{ y: 260 }}
              />
            </Col>

            <Col xs={24} lg={10}>
              <FsDataTable
                title="Top category movers"
                columns={moverCols}
                dataSource={data.topCategoryGrowth}
                rowKey={(r) => String(r.categoryCode)}
                loading={loading}
                rowExplanation={moverExplanation}
                onRow={(record) => ({
                  onClick: () => openMoverDrill(record, 'category'),
                  style: { cursor: 'pointer' },
                })}
                scroll={{ y: 150 }}
              />
              <FsDataTable
                title="Top merchant movers"
                columns={moverCols}
                dataSource={data.topMerchantMovers}
                rowKey={(r) => String(r.merchantToken || r.key)}
                loading={loading}
                rowExplanation={moverExplanation}
                onRow={(record) => ({
                  onClick: () => openMoverDrill(record, 'merchant'),
                  style: { cursor: 'pointer' },
                })}
                scroll={{ y: 150 }}
              />
            </Col>
          </Row>
        </>
      )}

      <UnifiedDrillDrawer open={drillOpen} context={drillContext} onClose={closeDrill} />
    </DataPageLayout>
  )
}
