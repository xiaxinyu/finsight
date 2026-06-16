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
import { UnifiedDrillDrawer } from '../../components/ReportDrillDrawer'
import { buildReportDrillContext } from '../../components/drilldown/buildDrillContext'
import { useDrillDown } from '../../hooks/useDrillDown'
import { ReportKpiStrip } from '../../components/ReportKpiStrip'
import { formatMoney } from '../../utils/format'
import {
  buildContributorChart,
  buildTrendInsights,
  buildTrendKpis,
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
  const chartOption = useMemo(() => (data ? buildContributorChart(data) : {}), [data])

  const openTrendDrill = (label: string, item: TrendItem | TrendMover, explanation: string[]) => {
    const drillDown = item.drillDown || {}
    openDrill(buildReportDrillContext({
      title: `${label} · ${fromYear}→${toYear}`,
      metricLabel: label,
      params: drillDown,
      explanation,
      source: 'report',
    }))
  }

  const moverCols = [
    { title: 'Name', dataIndex: 'label', sortType: 'text' as const, ellipsis: true,
      render: (_: unknown, row: TrendMover) => row.label || row.categoryName || row.categoryCode },
    { title: 'Delta', dataIndex: 'deltaAmount', unit: 'CNY', align: 'right' as const, sortType: 'number' as const },
    {
      title: 'Change %',
      dataIndex: 'deltaPercent',
      align: 'right' as const,
      sortType: 'number' as const,
      render: (_: unknown, row: TrendMover) => `${Number(row.deltaPercent ?? row.pctChange ?? 0).toFixed(0)}%`,
    },
    {
      title: 'Contribution',
      dataIndex: 'contributionPct',
      align: 'right' as const,
      sortType: 'number' as const,
      render: (v: number) => `${Number(v).toFixed(1)}%`,
    },
  ]

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
            <Col xs={24} lg={14}>
              <ContentCard title="Contribution to expense change" size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile="categoryBar"
                  height={320}
                  loading={loading}
                  option={chartOption}
                  empty={<EmptyState compact title="No movers" />}
                  onEvents={{
                    click: (p) => {
                      const name = (p as { name?: string }).name
                      const cat = data.topCategoryGrowth.find((c) => c.categoryName === name)
                      const merchant = data.topMerchantMovers.find((m) => m.label === name)
                      if (cat) {
                        openTrendDrill(String(cat.categoryName), cat, [
                          `Category ${cat.categoryName} changed ${formatMoney(cat.deltaAmount)} YoY.`,
                          `Represents ${cat.contributionPct}% of total expense change.`,
                        ])
                      } else if (merchant) {
                        openTrendDrill(String(merchant.label), merchant, [
                          `Merchant ${merchant.label} changed ${formatMoney(merchant.deltaAmount)} YoY.`,
                          `Represents ${merchant.contributionPct}% of total expense change.`,
                        ])
                      }
                    },
                  }}
                />
              </ContentCard>

              <FsDataTable
                title="Trend summary"
                columns={[
                  { title: 'Type', dataIndex: 'type', width: 130, render: (t: string) => trendTypeLabel(t) },
                  { title: 'Label', dataIndex: 'label', sortType: 'text' },
                  { title: 'Delta', dataIndex: 'deltaAmount', unit: 'CNY', align: 'right', sortType: 'number' },
                  { title: 'Change %', dataIndex: 'deltaPercent', align: 'right', sortType: 'number',
                    render: (v: number) => `${v.toFixed(1)}%` },
                  { title: 'Contribution', dataIndex: 'contributionPct', align: 'right', sortType: 'number',
                    render: (v: number) => v ? `${v.toFixed(1)}%` : '—' },
                ]}
                dataSource={data.trends}
                rowKey={(r) => `${r.type}-${r.label}`}
                loading={loading}
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
                onRow={(record) => ({
                  onClick: () => openTrendDrill(String(record.categoryName), record, [
                    `Category ${record.categoryName} moved ${formatMoney(record.deltaAmount)}.`,
                  ]),
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
                onRow={(record) => ({
                  onClick: () => openTrendDrill(String(record.label), record, [
                    `Merchant ${record.label} moved ${formatMoney(record.deltaAmount)}.`,
                  ]),
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
