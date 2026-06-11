import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Col, Drawer, Row, Spin } from 'antd'
import { BarChartOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { reportConfigs } from '../../config/reports'
import { fetchReport } from '../../api/report'
import { listTransactions } from '../../api/transaction'
import { CardFilterSelect } from '../../components/filters/CardFilterSelect'
import { CategoryFilterSelect } from '../../components/filters/CategoryFilterSelect'
import { useFilterApply } from '../../hooks/useFilterApply'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { FsChart } from '../../components/FsChart'
import { InsightPanel } from '../../components/InsightPanel'
import { FilterToolbar } from '../../components/FilterToolbar'
import { KpiGrid } from '../../components/KpiGrid'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { finsightColors } from '../../styles/finsight-tokens'
import { FsDataTable, type FsColumn } from '../../components/FsDataTable'
import { emptyChartOption } from '../../components/charts/profiles'
import { PeriodRangePicker, periodToStrings } from '../../components/PeriodRangePicker'
import { formatMoney, MONTH_NAMES } from '../../utils/format'
import { rowAmount, rowTxnKind } from '../../utils/transactionAmount'
import { MoneyText, moneyTypeFromRow } from '../../components/MoneyText'
import { defaultComparePeriodRange, defaultPeriodRange, formatPeriodPreview } from '../../utils/periodPresets'
import { fromCategorySpend, fromIncomeExpense, fromYearCompare } from '../../utils/insights'
import { billCalendar, budgetVsActual } from '../../api/finance'
import { homeSummary } from '../../api/report'

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
  p.transactionDateStartStr = start
  p.transactionDateEndStr = end
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


  const baseParams = useMemo(() => (cfg ? buildParams(cfg, applied) : {}), [cfg, applied])

  const { data, isLoading, isFetching, refetch } = useQuery({
    queryKey: ['report', reportId, baseParams, periodToStrings(applied.comparePeriod)],
    enabled: !!cfg,
    queryFn: async () => {
      if (!cfg) return null
      if (cfg.type === 'billsCalendar') {
        return { calendar: await billCalendar() }
      }
      if (cfg.type === 'budgetVsActual') {
        const bva = await budgetVsActual()
        const meta = bva?.[0] || {}
        const lines = (meta.lines as Record<string, unknown>[]) || []
        return { bva: lines, meta }
      }
      if (cfg.type === 'homeBuckets') {
        const summary = await homeSummary(applied.period[0].year())
        const week = await fetchReport('/transaction-report/week-consume', baseParams)
        return { summary, week }
      }
      if (cfg.type === 'incomeVsExpense') {
        const r = periodToStrings(applied.period)
        const base = { transactionDateStartStr: r.start, transactionDateEndStr: r.end, cardId: applied.card, consumeID: applied.consume }
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

  const { data: drillRows, isFetching: drillLoading } = useQuery({
    queryKey: ['drill', drillParams],
    enabled: drillOpen && !!drillParams.transactionDateStartStr,
    queryFn: () => listTransactions({ ...drillParams, page: 1, rows: 50 }),
  })

  const viewportH = useViewportTableHeight(320)
  const chartHeight = Math.min(viewportH, 380)

  if (!cfg) return <DataPageLayout title="Report not found"><EmptyState title="Report not found" /></DataPageLayout>

  if (cfg.type === 'billsCalendar') {
    const rows = (data && 'calendar' in data ? data.calendar : []) as Record<string, unknown>[]
    return (
      <DataPageLayout
        title={cfg.title}
        subtitle={cfg.subtitle}
        icon={<BarChartOutlined />}
        toolbar={(
          <FilterToolbar loading={chartLoading} onApply={() => refetch()} dirty={false}>
            <span className="fs-import-hint">Upcoming bills from Planning · next 30 days</span>
          </FilterToolbar>
        )}
      >
        {rows.length === 0 && !chartLoading && (
          <EmptyState compact title="No upcoming bills" description="Add bills in Planning to see the calendar." />
        )}
        <FsDataTable
          title="Next 30 days"
          columns={[
            { title: 'Date', dataIndex: 'date', sortType: 'date', width: 110 },
            { title: 'Bill', dataIndex: 'name' },
            { title: 'Amount', dataIndex: 'amount', unit: 'CNY', align: 'right', sortType: 'number' },
          ]}
          dataSource={rows}
          rowKey="billId"
          loading={chartLoading}
          scroll={{ y: viewportH }}
        />
      </DataPageLayout>
    )
  }

  const disabled = chartLoading

  let chartOption = emptyChartOption()
  let insights = [{ text: 'Adjust filters and click Apply.' }]
  let tableData: Record<string, unknown>[] = []
  let tableCols: FsColumn<Record<string, unknown>>[] = []
  let tableSummary: Record<string, number | string> | undefined
  let kpis: { key: string; label: string; value: string; color?: string }[] = []

  if (data && 'bva' in data && data.bva) {
    const lines = (data.bva as Record<string, unknown>[]) || []
    const meta = (data.meta as Record<string, unknown>) || {}
    const totalActual = Number(meta.actualTotal || 0)
    const totalLimit = Number(meta.limitTotal || 0)
    insights = [{
      text: totalLimit > 0 && totalActual / totalLimit > 0.8
        ? 'Spending is above 80% of budget — review limits in Planning.'
        : 'Compare each bucket limit against actual MTD spend.',
    }]
    kpis = [
      { key: 'actual', label: 'Spent MTD', value: formatMoney(totalActual) },
      { key: 'limit', label: 'Budget limit', value: formatMoney(totalLimit) },
      { key: 'rem', label: 'Remaining', value: formatMoney(Math.max(0, totalLimit - totalActual)) },
    ]
    chartOption = {
      title: { text: 'Budget vs actual' },
      legend: { data: ['Limit', 'Actual'] },
      xAxis: { data: lines.map((r) => String(r.bucketKey || r.categoryCode || 'line')) },
      series: [
        { name: 'Limit', type: 'bar', data: lines.map((r) => Number(r.limit || 0)), itemStyle: { color: '#94a3b8' } },
        { name: 'Actual', type: 'bar', data: lines.map((r) => Number(r.actual || 0)), itemStyle: { color: '#2563eb' } },
      ],
    }
    tableCols = [
      { title: 'Bucket', dataIndex: 'bucketKey', sortType: 'text' },
      { title: 'Limit', dataIndex: 'limit', unit: 'CNY', align: 'right', sortType: 'number' },
      { title: 'Actual', dataIndex: 'actual', unit: 'CNY', align: 'right', sortType: 'number' },
      { title: 'Remaining', dataIndex: 'remaining', unit: 'CNY', align: 'right', sortType: 'number' },
    ]
    tableData = lines
    tableSummary = { bucketKey: 'Total', limit: totalLimit, actual: totalActual, remaining: totalLimit - totalActual }
  } else if (data && 'summary' in data && data.summary) {
    const buckets = (data.summary as Record<string, unknown>).buckets_pct as Record<string, number> || {}
    const weekRows = ('week' in data && data.week) ? data.week.filter((r) => r.value > 0) : []
    chartOption = {
      title: { text: 'Expense structure' },
      xAxis: { data: Object.keys(buckets) },
      series: [{ type: 'bar', data: Object.values(buckets), itemStyle: { color: '#2563eb' } }],
    }
    if (weekRows.length) {
      tableCols = [
        { title: 'Weekday', dataIndex: 'key', sortType: 'text' },
        { title: 'Amount', dataIndex: 'value', unit: 'CNY', align: 'right', sortType: 'number' },
      ]
      tableData = weekRows.map((r) => ({ key: r.key, value: r.value }))
    }
    insights = [{ text: `Fixed vs variable view for ${formatPeriodPreview(applied.period[0], applied.period[1])}. Review fixed burden in Planning.` }]
    kpis = [{ key: 'fixed', label: 'Fixed bucket %', value: `${buckets.fixed || 0}%` }]
  } else if (data && 'rows' in data && data.rows) {
    const rows = data.rows.filter((r) => r.key && Number.isFinite(r.value))
    const total = rows.reduce((t, r) => t + r.value, 0)
    insights = fromCategorySpend(rows, total, formatPeriodPreview(applied.period[0], applied.period[1]))
    kpis = [
      { key: 'total', label: 'Total', value: formatMoney(total) },
      { key: 'cats', label: 'Categories', value: String(rows.length) },
    ]
    const top = [...rows].sort((a, b) => b.value - a.value).slice(0, 10)
    if (cfg.chartKind === 'donut') {
      chartOption = { title: { text: cfg.title }, series: [{ type: 'pie', radius: ['42%', '68%'], data: top.map((r) => ({ name: r.key, value: r.value })) }] }
    } else if (cfg.type === 'weekSummary') {
      const labels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
      const vals = [0, 0, 0, 0, 0, 0, 0]
      rows.forEach((r) => { const i = parseInt(r.key, 10); if (i >= 0 && i < 7) vals[i] = r.value })
      chartOption = { title: { text: cfg.title }, xAxis: { data: labels }, series: [{ type: 'bar', data: vals, itemStyle: { color: '#2563eb' } }] }
    } else if (cfg.type === 'monthlyCompare' || cfg.type === 'timeCurve') {
      const vals = MONTH_NAMES.map((_, i) => Number(data.rows?.[i]?.value || 0))
      chartOption = {
        title: { text: cfg.title },
        xAxis: { data: MONTH_NAMES },
        series: [{ name: cfg.title, type: 'line', data: vals, smooth: true, areaStyle: { opacity: 0.08 }, itemStyle: { color: '#2563eb' } }],
      }
      const yearTotal = vals.reduce((a, b) => a + b, 0)
      kpis = [{ key: 'yt', label: 'Year total', value: formatMoney(yearTotal) }]
      insights = [{ text: `${cfg.title} for ${formatPeriodPreview(applied.period[0], applied.period[1])}.` }]
    } else {
      chartOption = {
        title: { text: cfg.title },
        xAxis: { data: top.map((r) => r.key) },
        series: [{ type: 'bar', data: top.map((r) => r.value), itemStyle: { color: '#2563eb' } }],
      }
    }
    tableCols = [
      { title: 'Category', dataIndex: 'key', sortType: 'text', ellipsis: true },
      { title: 'Amount', dataIndex: 'value', unit: 'CNY', align: 'right', sortType: 'number' },
    ]
    tableData = top.map((r) => ({ key: r.key, value: r.value }))
    tableSummary = { key: summaryLabel(), value: total }
  }

  if (data && 'inc' in data && data.inc && data.exp) {
    const rows = MONTH_NAMES.map((m, i) => ({
      month: m,
      income: Number(data.inc[i]?.value || 0),
      expense: Number(data.exp[i]?.value || 0),
      surplus: Number(data.inc[i]?.value || 0) - Number(data.exp[i]?.value || 0),
    }))
    insights = fromIncomeExpense(rows, formatPeriodPreview(applied.period[0], applied.period[1]))
    const incomeTotal = rows.reduce((t, r) => t + r.income, 0)
    const expenseTotal = rows.reduce((t, r) => t + r.expense, 0)
    kpis = [
      { key: 'inc', label: 'Income', value: formatMoney(incomeTotal), color: finsightColors.income },
      { key: 'exp', label: 'Expense', value: formatMoney(expenseTotal), color: finsightColors.expense },
      { key: 'sur', label: 'Surplus', value: formatMoney(incomeTotal - expenseTotal) },
    ]
    chartOption = {
      title: { text: `Income vs Expense · ${formatPeriodPreview(applied.period[0], applied.period[1])}` },
      legend: { data: ['Income', 'Expense', 'Surplus'] },
      xAxis: { data: MONTH_NAMES },
      series: [
        { name: 'Income', type: 'bar', data: rows.map((r) => r.income), itemStyle: { color: '#10b981' } },
        { name: 'Expense', type: 'bar', data: rows.map((r) => r.expense), itemStyle: { color: '#f59e0b' } },
        { name: 'Surplus', type: 'line', data: rows.map((r) => r.surplus), smooth: true, itemStyle: { color: '#2563eb' } },
      ],
    }
    tableCols = [
      { title: 'Month', dataIndex: 'month', sortType: 'text' },
      { title: 'Income', dataIndex: 'income', unit: 'CNY', align: 'right', sortType: 'number' },
      { title: 'Expense', dataIndex: 'expense', unit: 'CNY', align: 'right', sortType: 'number' },
      { title: 'Surplus', dataIndex: 'surplus', unit: 'CNY', align: 'right', sortType: 'number' },
    ]
    tableData = rows
    tableSummary = { month: 'Total', income: incomeTotal, expense: expenseTotal, surplus: incomeTotal - expenseTotal }
  }

  if (data && 'a' in data && data.a) {
    const totalA = data.a.reduce((t, r) => t + Number(r.value), 0)
    const totalB = (data.b || []).reduce((t, r) => t + Number(r.value), 0)
    const labelA = formatPeriodPreview(applied.period[0], applied.period[1])
    const labelB = formatPeriodPreview(applied.comparePeriod[0], applied.comparePeriod[1])
    insights = fromYearCompare(totalA, totalB, labelA, labelB)
    const deltaPct = totalA > 0 ? ((totalB - totalA) / totalA) * 100 : 0
    kpis = [
      { key: 'y1', label: labelA, value: formatMoney(totalA) },
      { key: 'y2', label: labelB, value: formatMoney(totalB) },
      { key: 'delta', label: 'Δ%', value: `${deltaPct >= 0 ? '+' : ''}${deltaPct.toFixed(1)}%`, color: deltaPct > 0 ? finsightColors.expense : '#0891b2' },
    ]
    const topA = [...data.a].sort((x, y) => y.value - x.value).slice(0, 8).map((r) => ({ name: r.key, value: r.value }))
    const topB = [...(data.b || [])].sort((x, y) => y.value - x.value).slice(0, 8).map((r) => ({ name: r.key, value: r.value }))
    chartOption = {
      title: { text: `Category comparison · ${labelA} vs ${labelB}` },
      series: [
        { type: 'pie', radius: ['40%', '65%'], center: ['30%', '55%'], data: topA, label: { fontSize: 11 } },
        { type: 'pie', radius: ['40%', '65%'], center: ['72%', '55%'], data: topB, label: { fontSize: 11 } },
      ],
    }
  }

  const openDrillDown = (categoryName?: string, seriesIndex?: number) => {
    const range = cfg.type === 'yearCompare' && seriesIndex === 1 ? applied.comparePeriod : applied.period
    const r = periodToStrings(range)
    const next: Record<string, string> = {
      transactionDateStartStr: r.start,
      transactionDateEndStr: r.end,
      txnTypes: cfg.txnType || 'expense',
    }
    if (categoryName) next.consumeName = categoryName
    if (applied.card) next.cardId = applied.card
    setDrillParams(next)
    setDrillOpen(true)
  }

  const onChartClick = (params: unknown) => {
    const p = params as { name?: string; seriesIndex?: number }
    openDrillDown(p.name, p.seriesIndex)
  }

  const handleApply = () => apply(() => refetch())

  return (
    <DataPageLayout
      title={cfg.title}
      subtitle={cfg.subtitle}
      icon={<BarChartOutlined />}
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
            txnType={cfg?.txnType}
            value={draft.consume}
            onChange={(v) => setDraft((d) => ({ ...d, consume: v }))}
          />
        </FilterToolbar>
      )}
    >
      <InsightPanel bullets={insights} />
      {kpis.length > 0 && <KpiGrid items={kpis} />}

      <Row gutter={[12, 12]} style={{ flex: 1, minHeight: 0 }}>
        <Col xs={24} lg={tableData.length ? 14 : 24}>
          <ContentCard title={cfg.title} size="small" styles={{ body: { padding: 8 } }}>
            <FsChart
              profile={cfg.chartProfile || 'timeSeries'}
              height={chartHeight}
              loading={chartLoading}
              option={chartOption}
              onEvents={{ click: onChartClick }}
              empty={<EmptyState compact title="No chart data" description="Adjust filters and click Apply." />}
            />
          </ContentCard>
        </Col>
        {tableData.length > 0 && (
          <Col xs={24} lg={10}>
            <FsDataTable
              title="Breakdown"
              columns={tableCols}
              dataSource={tableData}
              rowKey="key"
              loading={chartLoading}
              summary={tableSummary}
              scroll={{ y: chartHeight - 40 }}
              onRow={(record) => ({
                onClick: () => {
                  const name = String(record.key || '')
                  if (name && name !== summaryLabel()) openDrillDown(name)
                },
                style: { cursor: 'pointer' },
              })}
            />
          </Col>
        )}
      </Row>

      <Drawer title="Transaction drill-down" width={680} open={drillOpen} onClose={() => setDrillOpen(false)}>
        {drillLoading ? (
          <div style={{ textAlign: 'center', padding: 48 }}><Spin tip="Loading transactions…" /></div>
        ) : (
          <FsDataTable
            columns={[
              { title: 'Date', dataIndex: 'transactionDate', sortType: 'date', width: 100 },
              { title: 'Description', dataIndex: 'transactionDesc', ellipsis: true },
              {
                title: 'Amount',
                dataIndex: 'balanceMoney',
                unit: 'CNY',
                align: 'right',
                sortType: 'number',
                render: (_, r) => (
                  <MoneyText
                    value={rowAmount(r as { incomeMoney?: number; balanceMoney?: number })}
                    type={moneyTypeFromRow(rowTxnKind(r as { incomeMoney?: number; balanceMoney?: number }), (r as { balanceMoney?: number }).balanceMoney)}
                  />
                ),
              },
            ]}
            dataSource={(drillRows?.rows || []) as unknown as Record<string, unknown>[]}
            rowKey="id"
            locale={{ emptyText: <EmptyState compact title="No transactions" description="No rows match this category and date range." /> }}
          />
        )}
      </Drawer>
    </DataPageLayout>
  )
}

function summaryLabel() {
  return 'Total'
}
