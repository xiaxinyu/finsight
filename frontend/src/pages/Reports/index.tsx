import { useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Alert } from 'antd'
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
import { UnifiedDrillDrawer } from '../../components/ReportDrillDrawer'
import { buildReportDrillContext, drillParamsForCategorySlice, drillParamsForSemanticTag, type CategoryDrillSlice } from '../../components/drilldown/buildDrillContext'
import { useDrillDown } from '../../hooks/useDrillDown'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { FsDataTable } from '../../components/FsDataTable'
import { PeriodRangePicker } from '../../components/PeriodRangePicker'
import { periodToStrings } from '../../utils/periodStrings'
import { formatMoney } from '../../utils/format'
import { defaultComparePeriodRange, defaultPeriodRange, formatPeriodPreview } from '../../utils/periodPresets'
import { billCalendar, budgetVsActual, listTransfers } from '../../api/finance'
import { fetchMetricPeriodSummary, fetchSemanticBreakdown } from '../../api/analytics'
import { buildReportView } from './buildReportView'
import { isDrillableSemanticTag } from '../../utils/semanticBreakdownReport'
import { AnnualOutlookReport } from './AnnualOutlookReport'
import { CashRiskReport } from './CashRiskReport'
import { MerchantReport } from './MerchantReport'
import { TrendChangesReport } from './TrendChangesReport'
import { SpendingDriftReport } from './SpendingDriftReport'

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
  const { open: drillOpen, context: drillContext, openDrill, closeDrill } = useDrillDown()

  const baseParams = useMemo(() => (cfg ? buildParams(cfg, applied) : {}), [cfg, applied])
  const semanticFilters = useMemo(
    () => ({
      cardId: applied.card || undefined,
      consumeID: applied.consume || undefined,
    }),
    [applied.card, applied.consume],
  )
  const periodLabel = formatPeriodPreview(applied.period[0], applied.period[1])

  const { data, isLoading, isFetching, isError, error, refetch } = useQuery({
    queryKey: ['report', reportId, baseParams, semanticFilters, periodToStrings(applied.comparePeriod)],
    enabled: !!cfg,
    queryFn: async () => {
      if (!cfg) return null
      if (cfg.type === 'billsCalendar') {
        return { calendar: await billCalendar() }
      }
      if (cfg.type === 'trendChanges') {
        return null
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
        const [semanticBreakdown, week] = await Promise.all([
          fetchSemanticBreakdown(r.start, r.end, semanticFilters),
          fetchReport('/transaction-report/week-consume', baseParams),
        ])
        return { semanticBreakdown, week, scope: 'expense' }
      }
      if (cfg.type === 'semanticScope' && cfg.semanticScope) {
        const r = periodToStrings(applied.period)
        const semanticBreakdown = await fetchSemanticBreakdown(r.start, r.end, {
          ...semanticFilters,
          scope: cfg.semanticScope,
        })
        return { semanticBreakdown, scope: cfg.semanticScope }
      }
      if (cfg.type === 'incomeVsExpense') {
        const r = periodToStrings(applied.period)
        const periodSummary = await fetchMetricPeriodSummary(r.start, r.end)
        return { periodSummary }
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
  const defaultChartHeight = 340

  const view = useMemo(
    () => (cfg ? buildReportView(cfg, data ?? undefined, applied) : null),
    [cfg, data, applied],
  )

  const tableScrollY = view?.tableData && view.tableData.length > 10 ? 280 : undefined

  const usesSemanticDrill = cfg?.type === 'homeBuckets' || cfg?.type === 'semanticScope'

  const openDrillDown = (
    categorySlice?: string | CategoryDrillSlice,
    seriesIndex?: number,
    semantic?: { tagId: string; label: string },
  ) => {
    if (!cfg) return
    const range = cfg.type === 'yearCompare' && seriesIndex === 1 ? applied.comparePeriod : applied.period
    const r = periodToStrings(range)
    const next: Record<string, string> = {
      transactionDateStartStr: r.start,
      transactionDateEndStr: r.end,
      txnTypes: cfg.txnType || 'expense',
    }
    if (semantic?.tagId) {
      if (!isDrillableSemanticTag(semantic.tagId)) return
      Object.assign(next, drillParamsForSemanticTag(semantic.tagId, r.start, r.end, (cfg.txnType || 'expense') as 'income' | 'expense'))
    } else if (categorySlice && typeof categorySlice === 'object') {
      Object.assign(next, drillParamsForCategorySlice(categorySlice, r.start, r.end, (cfg.txnType || 'expense') as 'income' | 'expense'))
    } else if (typeof categorySlice === 'string' && categorySlice) {
      Object.assign(next, drillParamsForCategorySlice({ key: categorySlice }, r.start, r.end, (cfg.txnType || 'expense') as 'income' | 'expense'))
    }
    if (applied.card) next.cardId = applied.card
    if (applied.consume && !next.consumeID) next.consumeID = applied.consume
    const periodLabel = formatPeriodPreview(range[0], range[1])
    const sliceLabel = semantic?.label || (typeof categorySlice === 'string' ? categorySlice : categorySlice?.key)
    const title = sliceLabel ? `${sliceLabel} · ${periodLabel}` : `${cfg.title} · ${periodLabel}`
    const insights = view?.insights.map((b) => b.text) || []
    const explanation = sliceLabel
      ? insights.filter((t) => t.toLowerCase().includes(sliceLabel.toLowerCase()))
      : insights
    openDrill(buildReportDrillContext({
      title,
      metricLabel: sliceLabel || cfg.title,
      params: next,
      explanation: explanation.length ? explanation : undefined,
      source: 'report',
      provenance: {
        reportId,
        sourceView: semantic?.tagId ? 'semantic classification slice' : categorySlice ? 'chart category slice' : 'report chart',
      },
    }))
  }

  const onChartClick = (params: unknown) => {
    const p = params as {
      name?: string
      seriesIndex?: number
      data?: { tagId?: string; level1Code?: string; level1Name?: string; code?: string }
    }
    if (usesSemanticDrill && p.data?.tagId) {
      if (!isDrillableSemanticTag(p.data.tagId)) return
      openDrillDown(undefined, p.seriesIndex, { tagId: p.data.tagId, label: p.name || p.data.tagId })
      return
    }
    if (p.name) {
      openDrillDown({
        key: p.name,
        level1Code: p.data?.level1Code,
        level1Name: p.data?.level1Name,
        code: p.data?.code,
      }, p.seriesIndex)
    }
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

  if (cfg.type === 'cashRisk') {
    return (
      <CashRiskReport
        title={cfg.title}
        subtitle={cfg.subtitle}
      />
    )
  }

  if (cfg.type === 'annualOutlook') {
    return (
      <AnnualOutlookReport
        title={cfg.title}
        subtitle={cfg.subtitle}
      />
    )
  }

  if (cfg.type === 'merchantSubscriptions') {
    return <MerchantReport title={cfg.title} subtitle={cfg.subtitle} mode="subscriptions" />
  }

  if (cfg.type === 'merchantConcentration') {
    return <MerchantReport title={cfg.title} subtitle={cfg.subtitle} mode="concentration" />
  }

  if (cfg.type === 'merchantDrift') {
    return <MerchantReport title={cfg.title} subtitle={cfg.subtitle} mode="drift" />
  }

  if (cfg.type === 'yearCompare') {
    return (
      <SpendingDriftReport
        title={cfg.title}
        subtitle={cfg.subtitle}
        txnType={cfg.txnType}
      />
    )
  }

  if (cfg.type === 'trendChanges') {
    return <TrendChangesReport title={cfg.title} subtitle={cfg.subtitle} />
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
      className="fs-data-page--dense fs-data-page--reports"
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

          <div className={`fs-report-split${view.tableData.length ? ' fs-report-split--with-table' : ''}`}>
            <section className="fs-report-split__chart">
              <ContentCard title={view.chartTitle || cfg.title} size="small" styles={{ body: { padding: 8 } }}>
                {view.chartSummary && view.chartSummary.length > 0 && (
                  <div className="fs-chart-summary-strip" role="group" aria-label="Chart summary">
                    {view.chartSummary.map((s) => (
                      <div
                        key={s.key}
                        className={`fs-chart-summary-item${s.tone ? ` fs-chart-summary-item--${s.tone}` : ''}`}
                      >
                        <span className="fs-chart-summary-value">{s.value}</span>
                        <span className="fs-chart-summary-label">{s.label}</span>
                      </div>
                    ))}
                  </div>
                )}
                <FsChart
                  profile={view.chartProfile || cfg.chartProfile || 'timeSeries'}
                  height={view.chartHeight ?? defaultChartHeight}
                  loading={chartLoading}
                  option={view.chartOption}
                  onEvents={{ click: onChartClick }}
                  empty={<EmptyState compact title="No chart data" description="Adjust filters and click Apply." />}
                />
              </ContentCard>
            </section>
            {view.tableData.length > 0 && (
              <section className="fs-report-split__table">
                <FsDataTable
                  title="Breakdown"
                  columns={view.tableCols}
                  dataSource={view.tableData}
                  rowKey={(r) => String(r.key ?? r.month ?? r.label ?? r.bucketKey ?? '')}
                  loading={chartLoading}
                  summary={view.tableSummary}
                  scroll={tableScrollY != null ? { y: tableScrollY } : undefined}
                  fixedLayout={false}
                  onRow={(record) => ({
                    onClick: () => {
                      if (usesSemanticDrill) {
                        const tagId = String(record.tagId ?? record.key ?? '')
                        const label = String(record.classification ?? record.label ?? tagId)
                        if (isDrillableSemanticTag(tagId)) {
                          openDrillDown(undefined, undefined, { tagId, label })
                        }
                        return
                      }
                      const name = String(record.key || record.label || '')
                      if (name && name !== 'Total') {
                        openDrillDown({
                          key: name,
                          level1Code: record.level1Code as string | undefined,
                          level1Name: record.level1Name as string | undefined,
                          code: record.code as string | undefined,
                        })
                      }
                    },
                    style: { cursor: 'pointer' },
                  })}
                />
              </section>
            )}
          </div>
        </>
      )}

      <UnifiedDrillDrawer
        open={drillOpen}
        context={drillContext}
        onClose={closeDrill}
      />
    </DataPageLayout>
  )
}
