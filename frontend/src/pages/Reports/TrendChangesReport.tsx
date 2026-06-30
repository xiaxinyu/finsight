import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Segmented, Select, Tag, Tooltip, Typography } from 'antd'
import { ArrowRightOutlined, BarChartOutlined } from '@ant-design/icons'
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
import { DeltaPercentCell } from '../../components/FsTableCellViews'
import {
  buildCategoryContributorChart,
  buildMerchantContributorChart,
  buildTrendInsights,
  buildTrendKpis,
  buildTrendYoYCards,
  findTrendItem,
  moverFromTo,
  moverKey,
  moverLabel,
  trendChartHeight,
  type TrendYoYCard,
} from '../../utils/trendChanges'

type TrendChangesReportProps = {
  title: string
  subtitle?: string
}

type DriverView = 'category' | 'merchant'

function formatYoYDelta(card: TrendYoYCard): string {
  if (card.format === 'percent') {
    return `${card.deltaAmount >= 0 ? '+' : ''}${card.deltaAmount.toFixed(1)} pts`
  }
  return formatMoney(card.deltaAmount)
}

function formatYoYValue(card: TrendYoYCard, value: number): string {
  return card.format === 'percent' ? `${value.toFixed(1)}%` : formatMoney(value)
}

export function TrendChangesReport({ title, subtitle }: TrendChangesReportProps) {
  const { flags } = useFeatureFlags()
  const { open: drillOpen, context: drillContext, openDrill, closeDrill } = useDrillDown()
  const [toYear, setToYear] = useState(dayjs().year())
  const [driverView, setDriverView] = useState<DriverView>('category')
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const fromYear = toYear - 1

  const { data, isLoading, isFetching, isError, error } = useQuery({
    queryKey: ['trend-changes', fromYear, toYear],
    queryFn: () => fetchTrends(fromYear, toYear),
    enabled: flags.forecast,
  })

  const loading = isLoading || isFetching
  const kpis = useMemo(() => (data ? buildTrendKpis(data) : []), [data])
  const insights = useMemo(() => (data ? buildTrendInsights(data) : []), [data])
  const yoyCards = useMemo(() => (data ? buildTrendYoYCards(data) : []), [data])

  const movers = useMemo(() => {
    if (!data) return []
    return driverView === 'category' ? data.topCategoryGrowth : data.topMerchantMovers
  }, [data, driverView])

  const chartOption = useMemo(() => {
    if (!data) return {}
    return driverView === 'category'
      ? buildCategoryContributorChart(data)
      : buildMerchantContributorChart(data)
  }, [data, driverView])

  const chartHeight = trendChartHeight(Math.min(movers.length, 8))

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

  const moverExplanation = (row: TrendMover) => {
    const { from, to } = moverFromTo(row)
    const pct = Number(row.deltaPercent ?? row.pctChange ?? 0)
    const name = moverLabel(row)
    const parts = [
      from != null && to != null ? `${name}: ${formatMoney(from)} → ${formatMoney(to)}` : name,
      `Δ ${formatMoney(row.deltaAmount)} (${pct >= 0 ? '+' : ''}${pct.toFixed(1)}%)`,
      `${Number(row.contributionPct).toFixed(1)}% of expense shift`,
    ]
    return parts.join(' · ')
  }

  const openMoverDrill = (row: TrendMover) => {
    const name = moverLabel(row)
    openTrendDrill(name, row, [
      `${driverView === 'category' ? 'Category' : 'Merchant'} ${name} moved ${formatMoney(row.deltaAmount)} YoY.`,
      moverExplanation(row),
    ])
  }

  const openYoYCardDrill = (card: TrendYoYCard) => {
    if (!data || !card.trendType) return
    const trend = findTrendItem(data, card.trendType)
    if (!trend?.drillDown) return
    openTrendDrill(card.label, trend, [
      `${card.label}: ${formatYoYValue(card, card.from)} → ${formatYoYValue(card, card.to)}`,
      `${formatYoYDelta(card)} (${card.deltaPercent >= 0 ? '+' : ''}${card.deltaPercent.toFixed(1)}% change).`,
    ])
  }

  const fromToCols = [
    {
      title: String(fromYear),
      key: 'fromAmount',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 96,
      render: (_: unknown, row: TrendMover) => {
        const { from } = moverFromTo(row)
        return from == null ? '—' : formatMoney(from)
      },
    },
    {
      title: String(toYear),
      key: 'toAmount',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 96,
      render: (_: unknown, row: TrendMover) => {
        const { to } = moverFromTo(row)
        return to == null ? '—' : formatMoney(to)
      },
    },
  ]

  const moverCols = [
    {
      title: driverView === 'category' ? 'Category' : 'Merchant',
      dataIndex: 'label',
      sortType: 'text' as const,
      ellipsis: true,
      render: (_: unknown, row: TrendMover) => {
        const name = moverLabel(row)
        return (
          <Tooltip title={name}>
            <Typography.Text ellipsis>{name}</Typography.Text>
          </Tooltip>
        )
      },
    },
    ...fromToCols,
    {
      title: 'Delta',
      dataIndex: 'deltaAmount',
      cellType: 'deltaMoney' as const,
      unit: 'CNY',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 108,
    },
    {
      title: 'Change %',
      dataIndex: 'deltaPercent',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 80,
      render: (_: unknown, row: TrendMover) => (
        <DeltaPercentCell value={Number(row.deltaPercent ?? row.pctChange ?? 0)} />
      ),
    },
    {
      title: 'Share of shift',
      dataIndex: 'contributionPct',
      cellType: 'contribution' as const,
      align: 'right' as const,
      sortType: 'number' as const,
      width: 112,
    },
  ]

  const onChartClick = (params: unknown) => {
    if (!data) return
    const name = (p: { name?: string }) => p.name
    const clickedName = name(params as { name?: string })
    if (!clickedName) return
    const row = movers.find((m) => moverLabel(m) === clickedName)
    if (row) {
      setSelectedKey(moverKey(row, driverView))
    }
  }

  const yearOptions = [toYear - 1, toYear, toYear + 1].map((y) => ({ value: y, label: String(y) }))

  return (
    <DataPageLayout
      title={title}
      subtitle={subtitle ? `${subtitle} · ${fromYear} → ${toYear}` : `${fromYear} → ${toYear}`}
      icon={<BarChartOutlined />}
      className="fs-data-page--dense fs-data-page--reports fs-trend-changes"
      toolbar={(
        <div className="fs-trend-toolbar">
          <span className="fs-trend-toolbar__label">Compare</span>
          <Typography.Text type="secondary" className="fs-trend-toolbar__year">{fromYear}</Typography.Text>
          <ArrowRightOutlined className="fs-trend-toolbar__arrow" />
          <Select
            size="small"
            value={toYear}
            className="fs-trend-toolbar__select"
            options={yearOptions}
            onChange={(y) => {
              setToYear(y)
              setSelectedKey(null)
            }}
          />
          {data?.lifestyleInflation.detected && (
            <Tag color="warning" className="fs-trend-toolbar__tag">Lifestyle inflation</Tag>
          )}
        </div>
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
          <section className="fs-trend-yoy-grid" aria-label="Year-over-year summary">
            {yoyCards.map((card) => (
              <button
                key={card.key}
                type="button"
                className={`fs-trend-yoy-card fs-trend-yoy-card--${card.tone}`}
                onClick={() => openYoYCardDrill(card)}
                title={card.trendType ? 'Click to drill into transactions' : undefined}
              >
                <span className="fs-trend-yoy-card__label">{card.label}</span>
                <div className="fs-trend-yoy-card__flow">
                  <span className="fs-trend-yoy-card__amount">{formatYoYValue(card, card.from)}</span>
                  <ArrowRightOutlined className="fs-trend-yoy-card__arrow" />
                  <span className="fs-trend-yoy-card__amount fs-trend-yoy-card__amount--to">{formatYoYValue(card, card.to)}</span>
                </div>
                <span className={`fs-trend-yoy-card__delta${card.deltaAmount >= 0 ? ' fs-delta--up' : ' fs-delta--down'}`}>
                  {formatYoYDelta(card)}
                  <span className="fs-trend-yoy-card__pct">
                    ({card.deltaPercent >= 0 ? '+' : ''}{card.deltaPercent.toFixed(1)}%)
                  </span>
                </span>
              </button>
            ))}
          </section>

          <ReportKpiStrip items={kpis} />
          <InsightPanel bullets={insights} title="What changed" />

          <section className="fs-trend-drivers">
            <div className="fs-trend-drivers__head">
              <div>
                <Typography.Title level={5} className="fs-trend-drivers__title">What drove expense change?</Typography.Title>
                <Typography.Text type="secondary" className="fs-trend-drivers__hint">
                  Bars show YoY delta (orange = spend up, green = spend down). Click a bar to highlight; click a row to drill down.
                </Typography.Text>
              </div>
              <Segmented<DriverView>
                value={driverView}
                onChange={(v) => {
                  setDriverView(v)
                  setSelectedKey(null)
                }}
                options={[
                  { label: `Categories (${data.topCategoryGrowth.length})`, value: 'category' },
                  { label: `Merchants (${data.topMerchantMovers.length})`, value: 'merchant' },
                ]}
              />
            </div>

            <div className="fs-report-split fs-report-split--with-table">
              <section className="fs-report-split__chart">
                <ContentCard
                  title={driverView === 'category' ? 'Category movers' : 'Merchant movers'}
                  size="small"
                  className="fs-trend-drivers__chart-card"
                  styles={{ body: { padding: 8 } }}
                >
                  <FsChart
                    profile="horizontalBar"
                    height={chartHeight}
                    loading={loading}
                    option={chartOption}
                    empty={<EmptyState compact title="No movers" description="Try another comparison year." />}
                    onEvents={{ click: onChartClick }}
                  />
                </ContentCard>
              </section>

              <section className="fs-report-split__table">
                <FsDataTable
                  title="Contributors"
                  columns={moverCols}
                  dataSource={movers}
                  rowKey={(r) => moverKey(r, driverView)}
                  loading={loading}
                  rowExplanation={moverExplanation}
                  onRow={(record) => ({
                    onClick: () => {
                      setSelectedKey(moverKey(record, driverView))
                      openMoverDrill(record)
                    },
                    style: { cursor: 'pointer' },
                  })}
                  rowClassName={(record) => (
                    selectedKey === moverKey(record, driverView) ? 'fs-trend-row--selected' : ''
                  )}
                  scroll={movers.length > 8 ? { y: 320 } : undefined}
                  fixedLayout={false}
                  locale={{
                    emptyText: (
                      <EmptyState
                        compact
                        title={driverView === 'category' ? 'No category movers' : 'No merchant movers'}
                        description="Expense may be flat or evenly distributed."
                      />
                    ),
                  }}
                />
              </section>
            </div>
          </section>
        </>
      )}

      <UnifiedDrillDrawer open={drillOpen} context={drillContext} onClose={closeDrill} />
    </DataPageLayout>
  )
}
