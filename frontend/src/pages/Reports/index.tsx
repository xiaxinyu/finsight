import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Alert, Col, Row } from 'antd'
import { BarChartOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { reportConfigs } from '../../config/reports'
import { fetchReport } from '../../api/report'
import { CardFilterSelect } from '../../components/filters/CardFilterSelect'
import { CategoryFilterSelect } from '../../components/filters/CategoryFilterSelect'
import { useFilterApply } from '../../hooks/useFilterApply'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { FsChart } from '../../components/FsChart'
import { InsightPanel } from '../../components/InsightPanel'
import { FilterToolbar } from '../../components/FilterToolbar'
import { ReportKpiStrip } from '../../components/ReportKpiStrip'
import { ReportDrillDrawer } from '../../components/ReportDrillDrawer'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { FsDataTable } from '../../components/FsDataTable'
import { PeriodRangePicker, periodToStrings } from '../../components/PeriodRangePicker'
import { formatMoney } from '../../utils/format'
import { defaultComparePeriodRange, defaultPeriodRange, formatPeriodPreview } from '../../utils/periodPresets'
import { billCalendar, budgetVsActual, listTransfers } from '../../api/finance'
import { homeSummary } from '../../api/report'
import { buildReportView } from './buildReportView'

type ReportFilters = {
  period: [dayjs.Dayjs, dayjs.Dayjs]
  comparePeriod: [dayjs.Dayjs, dayjs.Dayjs]
  card: string
  consume: string
}

function buildParams(cfg: NonNullable<(typeof reportConfigs)[string]>, f: ReportFilters) {
  const p: Record<string, unknown> = { txnTypes: cfg.txnType || 'expense' }
  if (f.card) p.cardId = f.card
  if (f.consume) p.consumeID = f.consume
  const { start, end } = periodToStrings(f.period)
  if (start) p.transactionDateStartStr = start
  if (end) p.transactionDateEndStr = end
  return p
}

export function ReportsPage() {
  const { reportId = '' } = useParams()
  const cfg = reportConfigs[reportId]
  const initialFilters: ReportFilters = {
    period: defaultPeriodRange(),
    comparePeriod: defaultComparePeriodRange(),
    card: '',
    consume: '',
  }

  const { draft, setDraft, applied, applying, isDirty, apply } = useFilterApply(initialFilters)
  const [drillOpen, setDrillOpen] = useState(false)
  const [drillParams, setDrillParams] = useState<Record<string, string>>({})
  const [drillTitle, setDrillTitle] = useState('Transaction drill-down')

  const baseParams = useMemo(() => (cfg ? buildParams(cfg, applied) : {}), [cfg, applied])
  const periodLabel = formatPeriodPreview(applied.period[0], applied.period[1])

  const { data, isLoading, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['report', reportId, baseParams, periodToStrings(applied.comparePeriod)],
    enabled: !!cfg,
    queryFn: async () => {
      if (!cfg) return null
      if (cfg.type === 'billsCalendar') {
        return { calendar: await billCalendar() }
      }
      if (cfg.type === 'budgetVsActual') {
        const r = periodToStrings(applied.period)
        const bva = await budgetVsActual({
          transactionDateStartStr: r.start,
          transactionDateEndStr: r.end,
        })
        const meta = bva?.[0] || {}
        const lines = (meta.lines as Record<string, unknown>[]) || []
        return { bva: lines, meta }
      }
      if (cfg.type === 'transfers') {
        const rows = await listTransfers()
        const r = periodToStrings(applied.period)
        const startMs = dayjs(r.start).startOf('day').valueOf()
        const endMs = dayjs(r.end).endOf('day').valueOf()
        const filtered = rows.filter((row) => {
          const d = dayjs(String(row.transferDate || ''))
          return d.isValid() && d.valueOf() >= startMs && d.valueOf() <= endMs
        })
        return { transfers: filtered }
      }
      if (cfg.type === 'homeBuckets') {
        const r = periodToStrings(applied.period)
        const summary = await homeSummary(applied.period[0].year(), r)
        const week = await fetchReport('/transaction-report/week-consume', baseParams)
        return { summary, week }
      }
      if (cfg.type === 'incomeVsExpense') {
        const r = periodToStrings(applied.period)
        const base = {
          transactionDateStartStr: r.start,
          transactionDateEndStr: r.end,
          cardId: applied.card || undefined,
          consumeID: applied.consume || undefined,
        }
        const [inc, exp] = await Promise.all([
          fetchReport('/transaction-report/month-income', { ...base, txnTypes: 'income' }),
          fetchReport('/transaction-report/month-expense', { ...base, txnTypes: 'expense' }),
        ])
        return { inc, exp }
      }
      if (cfg.type === 'yearCompare') {
        const rA = periodToStrings(applied.period)
        const rB = periodToStrings(applied.comparePeriod)
        const [a, b] = await Promise.all([
          fetchReport('/transaction-report/consume', { ...baseParams, transactionDateStartStr: rA.start, transactionDateEndStr: rA.end }),
          fetchReport('/transaction-report/consume', { ...baseParams, transactionDateStartStr: rB.start, transactionDateEndStr: rB.end }),
        ])
        return { a, b }
      }
      const ep = cfg.type === 'timeCurve'
        ? (cfg.txnType === 'income' ? '/transaction-report/month-income' : '/transaction-report/month-expense')
        : cfg.type === 'weekSummary' ? '/transaction-report/week-consume'
        : cfg.type === 'monthlyCompare' ? '/transaction-report/month-consume'
        : cfg.endpoint || '/transaction-report/consume'
      const rows = await fetchReport(ep, baseParams)
      return { rows }
    },
  })

  const chartLoading = isLoading || isFetching || applying
  const viewportH = useViewportTableHeight(320)
  const chartHeight = Math.min(viewportH, 400)

  const view = useMemo(
    () => (cfg ? buildReportView(cfg, data ?? undefined, applied) : null),
    [cfg, data, applied],
  )

  const openDrillDown = (categoryName?: string, seriesIndex?: number) => {
    if (!cfg) return
    const range = cfg.type === 'yearCompare' && seriesIndex === 1 ? applied.comparePeriod : applied.period
    const r = periodToStrings(range)
    const next: Record<string, string> = {
      transactionDateStartStr: r.start,
      transactionDateEndStr: r.end,
      txnTypes: cfg.txnType || 'expense',
    }
    if (categoryName) next.consumeName = categoryName
    if (applied.card) next.cardId = applied.card
    setDrillTitle(categoryName ? `${categoryName} · ${formatPeriodPreview(range[0], range[1])}` : 'Transaction drill-down')
    setDrillParams(next)
    setDrillOpen(true)
  }

  const onChartClick = (params: unknown) => {
    const p = params as { name?: string; seriesIndex?: number }
    if (p.name) openDrillDown(p.name, p.seriesIndex)
  }

  const handleApply = () => apply(() => refetch())

  if (!cfg) {
    return (
      <DataPageLayout title="Report not found" icon={<BarChartOutlined />}>
        <EmptyState title="Report not found" description="Choose a report from the sidebar." />
      </DataPageLayout>
    )
  }

  if (cfg.type === 'transfers') {
    const rows = (data && 'transfers' in data ? data.transfers : []) as Record<string, unknown>[]
    return (
      <DataPageLayout
        title={cfg.title}
        subtitle={`${cfg.subtitle ?? 'Transfers'} · ${periodLabel}`}
        icon={<BarChartOutlined />}
        className="fs-data-page--dense fs-data-page--reports"
        toolbar={(
          <FilterToolbar loading={chartLoading} onApply={handleApply} dirty={isDirty}>
            <PeriodRangePicker
              size="small"
              disabled={chartLoading}
              value={draft.period}
              onChange={(range) => setDraft((d) => ({ ...d, period: range }))}
            />
          </FilterToolbar>
        )}
      >
        <ReportKpiStrip items={[
          { key: 'pairs', label: 'Transfer pairs', value: String(rows.length) },
          { key: 'hint', label: 'Scope', value: 'Excluded from spend' },
        ]} />
        <FsDataTable
          title="Internal transfers"
          columns={[
            { title: 'Date', dataIndex: 'transferDate', sortType: 'date', width: 120 },
            { title: 'Group ID', dataIndex: 'transferGroupId', ellipsis: true },
            { title: 'Transactions', dataIndex: 'transactionCount', align: 'right', sortType: 'number', width: 120 },
          ]}
          dataSource={rows}
          rowKey="transferGroupId"
          loading={chartLoading}
          scroll={{ y: viewportH }}
          locale={{ emptyText: <EmptyState compact title="No transfers in period" description="Mark transfer pairs on the Transactions page." /> }}
        />
      </DataPageLayout>
    )
  }

  if (cfg.type === 'billsCalendar') {
    const rows = (data && 'calendar' in data ? data.calendar : []) as Record<string, unknown>[]
    const total = rows.reduce((s, r) => s + Number(r.amount || 0), 0)
    return (
      <DataPageLayout
        title={cfg.title}
        subtitle={cfg.subtitle}
        icon={<BarChartOutlined />}
        className="fs-data-page--dense fs-data-page--reports"
      >
        <ReportKpiStrip items={[
          { key: 'bills', label: 'Upcoming bills', value: String(rows.length) },
          { key: 'amt', label: 'Next 30 days', value: formatMoney(total), tone: 'expense' },
        ]} />
        <FsDataTable
          title="Bills calendar"
          columns={[
            { title: 'Date', dataIndex: 'date', sortType: 'date', width: 110 },
            { title: 'Bill', dataIndex: 'name' },
            { title: 'Amount', dataIndex: 'amount', unit: 'CNY', align: 'right', sortType: 'number' },
          ]}
          dataSource={rows}
          rowKey="billId"
          loading={chartLoading}
          scroll={{ y: viewportH }}
          locale={{ emptyText: <EmptyState compact title="No upcoming bills" description="Add bills in Planning." /> }}
        />
      </DataPageLayout>
    )
  }

  const disabled = chartLoading

  return (
    <DataPageLayout
      title={cfg.title}
      subtitle={`${cfg.subtitle ?? 'Analysis'} · ${periodLabel}`}
      icon={<BarChartOutlined />}
      className="fs-data-page--dense fs-data-page--fill fs-data-page--reports"
      toolbar={(
        <FilterToolbar loading={chartLoading} onApply={handleApply} dirty={isDirty}>
          <PeriodRangePicker
            size="small"
            disabled={disabled}
            value={draft.period}
            onChange={(range) => setDraft((d) => ({ ...d, period: range }))}
          />
          {cfg.compareYear && (
            <PeriodRangePicker
              size="small"
              disabled={disabled}
              placeholder="Compare period"
              value={draft.comparePeriod}
              onChange={(range) => setDraft((d) => ({ ...d, comparePeriod: range }))}
            />
          )}
          <CardFilterSelect
            disabled={disabled}
            value={draft.card}
            onChange={(v) => setDraft((d) => ({ ...d, card: v }))}
          />
          <CategoryFilterSelect
            disabled={disabled}
            txnType={cfg.txnType}
            value={draft.consume}
            onChange={(v) => setDraft((d) => ({ ...d, consume: v }))}
          />
        </FilterToolbar>
      )}
    >
      {isError && (
        <Alert
          type="error"
          showIcon
          message="Failed to load report"
          description={error instanceof Error ? error.message : 'Check filters and try Apply again.'}
        />
      )}
      {view && (
        <>
          <ReportKpiStrip items={view.kpis} />
          <InsightPanel bullets={view.insights} title="Analysis" />

          <Row gutter={[12, 12]} className="fs-report-body">
            <Col xs={24} lg={view.tableData.length ? 14 : 24}>
              <ContentCard title={view.chartTitle || cfg.title} size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile={cfg.chartProfile || 'timeSeries'}
                  height={chartHeight}
                  loading={chartLoading}
                  option={view.chartOption}
                  onEvents={{ click: onChartClick }}
                  empty={<EmptyState compact title="No chart data" description="Adjust filters and click Apply." />}
                />
              </ContentCard>
            </Col>
            {view.tableData.length > 0 && (
              <Col xs={24} lg={10}>
                <FsDataTable
                  title="Breakdown"
                  columns={view.tableCols}
                  dataSource={view.tableData}
                  rowKey={(r) => String(r.key ?? r.month ?? r.label ?? r.bucketKey ?? '')}
                  loading={chartLoading}
                  summary={view.tableSummary}
                  scroll={{ y: chartHeight - 24 }}
                  onRow={(record) => ({
                    onClick: () => {
                      const name = String(record.key || record.label || '')
                      if (name && name !== 'Total') openDrillDown(name)
                    },
                    style: { cursor: 'pointer' },
                  })}
                />
              </Col>
            )}
          </Row>
        </>
      )}

      <ReportDrillDrawer
        open={drillOpen}
        params={drillParams}
        title={drillTitle}
        onClose={() => setDrillOpen(false)}
      />
    </DataPageLayout>
  )
}
