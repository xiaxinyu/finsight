import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Col, Row } from 'antd'
import { BarChartOutlined, SwapOutlined } from '@ant-design/icons'
import { fetchReport } from '../../api/report'
import { CardFilterSelect } from '../../components/filters/CardFilterSelect'
import { CategoryFilterSelect } from '../../components/filters/CategoryFilterSelect'
import { useFilterApply } from '../../hooks/useFilterApply'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { FsChart } from '../../components/FsChart'
import { InsightPanel } from '../../components/InsightPanel'
import { FilterToolbar } from '../../components/FilterToolbar'
import { ReportKpiStrip } from '../../components/ReportKpiStrip'
import { UnifiedDrillDrawer } from '../../components/ReportDrillDrawer'
import { buildReportDrillContext } from '../../components/drilldown/buildDrillContext'
import { useDrillDown } from '../../hooks/useDrillDown'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { DataQualityStrip } from '../../components/DataQualityStrip'
import { EmptyState } from '../../components/EmptyState'
import { FsDataTable } from '../../components/FsDataTable'
import { PeriodRangePicker } from '../../components/PeriodRangePicker'
import { DeltaPercentCell } from '../../components/FsTableCellViews'
import { periodToStrings } from '../../utils/periodStrings'
import { formatMoney } from '../../utils/format'
import {
  defaultComparePeriodRange,
  defaultPeriodRange,
  formatPeriodPreview,
} from '../../utils/periodPresets'
import {
  buildSpendingDriftChart,
  buildSpendingDriftInsights,
  buildSpendingDriftKpis,
  buildSpendingDriftRows,
  normalizeSpendingDriftRows,
  periodDaySpan,
  periodsComparable,
  shiftCompareToPriorYear,
  spendingDriftChartHeight,
  type SpendingDriftRow,
} from '../../utils/spendingDrift'

type SpendingDriftReportProps = {
  title: string
  subtitle?: string
  txnType?: 'income' | 'expense'
}

type Filters = {
  period: ReturnType<typeof defaultPeriodRange>
  comparePeriod: ReturnType<typeof defaultComparePeriodRange>
  card: string
  consume: string
}

function buildParams(f: Filters, txnType: string) {
  const p: Record<string, unknown> = { txnTypes: txnType }
  if (f.card) p.cardId = f.card
  if (f.consume) p.consumeID = f.consume
  return p
}

export function SpendingDriftReport({ title, subtitle, txnType = 'expense' }: SpendingDriftReportProps) {
  const initialPeriod = defaultPeriodRange()
  const initialFilters: Filters = {
    period: initialPeriod,
    comparePeriod: defaultComparePeriodRange(initialPeriod),
    card: '',
    consume: '',
  }

  const { draft, setDraft, applied, applying, isDirty, apply } = useFilterApply(initialFilters)
  const { open: drillOpen, context: drillContext, openDrill, closeDrill } = useDrillDown()
  const viewportH = useViewportTableHeight(280)

  const baseParams = useMemo(() => buildParams(applied, txnType), [applied, txnType])
  const periodLabel = formatPeriodPreview(applied.period[0], applied.period[1])
  const compareLabel = formatPeriodPreview(applied.comparePeriod[0], applied.comparePeriod[1])

  const { data, isLoading, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['spending-drift', baseParams, periodToStrings(applied.period), periodToStrings(applied.comparePeriod)],
    queryFn: async () => {
      const rA = periodToStrings(applied.period)
      const rB = periodToStrings(applied.comparePeriod)
      const [a, b] = await Promise.all([
        fetchReport('/transaction-report/consume', { ...baseParams, transactionDateStartStr: rA.start, transactionDateEndStr: rA.end }),
        fetchReport('/transaction-report/consume', { ...baseParams, transactionDateStartStr: rB.start, transactionDateEndStr: rB.end }),
      ])
      return { a, b }
    },
  })

  const loading = isLoading || isFetching || applying
  const spanA = periodDaySpan(applied.period)
  const spanB = periodDaySpan(applied.comparePeriod)
  const comparable = periodsComparable(spanA, spanB)

  const rows = useMemo(() => {
    if (!data) return []
    const raw = buildSpendingDriftRows(data.a, data.b)
    return comparable ? raw : normalizeSpendingDriftRows(raw, spanA, spanB)
  }, [data, comparable, spanA, spanB])

  const kpis = useMemo(
    () => (data ? buildSpendingDriftKpis(data.a, data.b, applied.period, applied.comparePeriod) : []),
    [data, applied.period, applied.comparePeriod],
  )
  const insights = useMemo(
    () => (data ? buildSpendingDriftInsights(data.a, data.b, applied.period, applied.comparePeriod) : []),
    [data, applied.period, applied.comparePeriod],
  )
  const chartOption = useMemo(
    () => buildSpendingDriftChart(rows, periodLabel, compareLabel, 10),
    [rows, periodLabel, compareLabel],
  )
  const chartHeight = spendingDriftChartHeight(Math.min(rows.length, 10))

  const openDrillDown = (categoryName: string, range: Filters['period']) => {
    const r = periodToStrings(range)
    openDrill(buildReportDrillContext({
      title: `${categoryName} · ${formatPeriodPreview(range[0], range[1])}`,
      metricLabel: categoryName,
      params: {
        transactionDateStartStr: r.start,
        transactionDateEndStr: r.end,
        txnTypes: txnType,
        consumeName: categoryName,
        ...(applied.card ? { cardId: applied.card } : {}),
      },
      explanation: insights.map((b) => b.text),
      source: 'report',
      provenance: {
        reportId: 'spending-drift',
        sourceView: 'category drift row',
      },
    }))
  }

  const alignComparePeriod = () => {
    setDraft((d) => ({ ...d, comparePeriod: shiftCompareToPriorYear(d.period) }))
  }

  const tableCols = [
    {
      title: 'Category',
      dataIndex: 'key',
      sortType: 'text' as const,
      ellipsis: true,
      width: 200,
    },
    {
      title: periodLabel,
      dataIndex: 'periodA',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 110,
      render: (v: number) => formatMoney(v),
    },
    {
      title: compareLabel,
      dataIndex: 'periodB',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 110,
      render: (v: number) => formatMoney(v),
    },
    {
      title: 'Δ',
      dataIndex: 'delta',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 100,
      render: (v: number) => (
        <span className={v > 0 ? 'fs-delta--up' : v < 0 ? 'fs-delta--down' : undefined}>
          {v >= 0 ? '+' : ''}{formatMoney(v)}
        </span>
      ),
    },
    {
      title: comparable ? 'Δ%' : 'Pace Δ%',
      dataIndex: comparable ? 'deltaPct' : 'monthlyDeltaPct',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 88,
      render: (_: unknown, row: SpendingDriftRow) => (
        <DeltaPercentCell
          value={Number(comparable ? row.deltaPct : row.monthlyDeltaPct)}
          amount={row.delta}
        />
      ),
    },
    {
      title: 'Share of shift',
      dataIndex: 'shareOfShift',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 100,
      render: (v: number) => `${Math.abs(v).toFixed(0)}%`,
    },
  ]

  return (
    <DataPageLayout
      title={title}
      subtitle={subtitle ?? 'Category spending vs a comparison period'}
      icon={<BarChartOutlined />}
      className="fs-data-page--dense fs-data-page--fill fs-data-page--reports fs-spending-drift"
      toolbar={(
        <FilterToolbar loading={loading} onApply={() => apply(() => refetch())} dirty={isDirty}>
          <PeriodRangePicker
            size="small"
            disabled={loading}
            placeholder="Current period"
            value={draft.period}
            onChange={(range) => setDraft((d) => ({ ...d, period: range }))}
          />
          <PeriodRangePicker
            size="small"
            disabled={loading}
            placeholder="Compare period"
            value={draft.comparePeriod}
            onChange={(range) => setDraft((d) => ({ ...d, comparePeriod: range }))}
          />
          <Button
            size="small"
            icon={<SwapOutlined />}
            disabled={loading}
            onClick={alignComparePeriod}
          >
            Prior-year dates
          </Button>
          <CardFilterSelect
            disabled={loading}
            value={draft.card}
            onChange={(v) => setDraft((d) => ({ ...d, card: v }))}
          />
          <CategoryFilterSelect
            disabled={loading}
            txnType={txnType}
            value={draft.consume}
            onChange={(v) => setDraft((d) => ({ ...d, consume: v }))}
          />
        </FilterToolbar>
      )}
    >
      <DataQualityStrip metricsSource="report_sql" compact />
      {isError && (
        <Alert
          type="error"
          showIcon
          message="Failed to load spending drift"
          description={error instanceof Error ? error.message : 'Adjust filters and click Apply.'}
        />
      )}

      {data && (
        <>
          <ReportKpiStrip items={kpis} />

          {!comparable && (
            <Alert
              type="warning"
              showIcon
              className="fs-spending-drift__warn"
              message="Uneven comparison periods"
              description="Totals and raw % change are skewed when periods differ in length. Use monthly pace and the breakdown table, or click Prior-year dates for a fair comparison."
            />
          )}

          <InsightPanel bullets={insights.slice(0, 4)} title="What moved" />

          <Row gutter={[12, 12]} className="fs-report-body fs-spending-drift__body">
            <Col xs={24} xl={10}>
              <ContentCard
                title={`Top movers · ${periodLabel} vs ${compareLabel}`}
                size="small"
                className="fs-spending-drift__chart-card"
                styles={{ body: { padding: '8px 4px 4px' } }}
              >
                <FsChart
                  profile="categoryBar"
                  height={chartHeight}
                  loading={loading}
                  option={chartOption}
                  empty={<EmptyState compact title="No category data" description="Adjust filters and Apply." />}
                  onEvents={{
                    click: (p) => {
                      const name = (p as { name?: string }).name
                      if (name) openDrillDown(name, applied.period)
                    },
                  }}
                />
              </ContentCard>
            </Col>
            <Col xs={24} xl={14}>
              <FsDataTable
                title="Category breakdown"
                columns={tableCols}
                dataSource={rows}
                rowKey="key"
                loading={loading}
                scroll={{ y: Math.max(chartHeight, viewportH - 48) }}
                summary={{
                  key: 'Total',
                  periodA: rows.reduce((s, r) => s + r.periodA, 0),
                  periodB: rows.reduce((s, r) => s + r.periodB, 0),
                  delta: rows.reduce((s, r) => s + r.delta, 0),
                }}
                onRow={(record) => ({
                  onClick: () => openDrillDown(record.key, applied.period),
                  style: { cursor: 'pointer' },
                })}
                locale={{
                  emptyText: <EmptyState compact title="No categories" description="Try a wider date range." />,
                }}
              />
            </Col>
          </Row>
        </>
      )}

      <UnifiedDrillDrawer open={drillOpen} context={drillContext} onClose={closeDrill} />
    </DataPageLayout>
  )
}
