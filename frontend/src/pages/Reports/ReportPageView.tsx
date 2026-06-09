import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Col, DatePicker, Drawer, Row, Select, Spin, TreeSelect } from 'antd'
import { BarChartOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { reportConfigs } from '../../config/reports'
import { fetchReport } from '../../api/report'
import { listCards, listTransactions } from '../../api/transaction'
import { useConsumeTreeSelect } from '../../hooks/useConsumeTree'
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
import { formatDateMmDdYyyy, formatMoney, MONTH_NAMES, yearOptions, yearRange } from '../../utils/format'
import { dateRangePresets } from '../../utils/datePresets'
import { fromCategorySpend, fromIncomeExpense, fromYearCompare } from '../../utils/insights'

const { RangePicker } = DatePicker

type ReportFilters = {
  year: number
  year2: number
  dateRange: [dayjs.Dayjs, dayjs.Dayjs]
  card: string
  consume: string
}

function buildParams(cfg: NonNullable<(typeof reportConfigs)[string]>, f: ReportFilters) {
  const p: Record<string, unknown> = { txnTypes: cfg.txnType || 'expense' }
  if (f.card) p.cardTypeName = f.card
  if (f.consume) p.consumeID = f.consume
  if (cfg.dateRange) {
    p.transactionDateStartStr = formatDateMmDdYyyy(f.dateRange[0])
    p.transactionDateEndStr = formatDateMmDdYyyy(f.dateRange[1])
  } else {
    const r = yearRange(f.year)
    p.transactionDateStartStr = r.start
    p.transactionDateEndStr = r.end
  }
  return p
}

export function ReportPageView() {
  const { reportId = '' } = useParams()
  const cfg = reportConfigs[reportId]
  const curYear = new Date().getFullYear()

  const initialFilters: ReportFilters = {
    year: curYear,
    year2: curYear - 1,
    dateRange: [dayjs().startOf('year'), dayjs()],
    card: '',
    consume: '',
  }

  const { draft, setDraft, applied, applying, isDirty, apply } = useFilterApply(initialFilters)
  const [drillOpen, setDrillOpen] = useState(false)
  const [drillParams, setDrillParams] = useState<Record<string, string>>({})

  const { data: cards } = useQuery({ queryKey: ['cards'], queryFn: listCards })
  const { treeData } = useConsumeTreeSelect(cfg?.txnType)

  const baseParams = useMemo(() => (cfg ? buildParams(cfg, applied) : {}), [cfg, applied])

  const { data, isLoading, isFetching, refetch } = useQuery({
    queryKey: ['report', reportId, baseParams, applied.year2],
    enabled: !!cfg,
    queryFn: async () => {
      if (!cfg) return null
      if (cfg.type === 'incomeVsExpense') {
        const r = yearRange(applied.year)
        const base = { transactionDateStartStr: r.start, transactionDateEndStr: r.end, cardTypeName: applied.card, consumeID: applied.consume }
        const [inc, exp] = await Promise.all([
          fetchReport('/transaction-report/month-income', { ...base, txnTypes: 'income' }),
          fetchReport('/transaction-report/month-expense', { ...base, txnTypes: 'expense' }),
        ])
        return { inc, exp }
      }
      if (cfg.type === 'yearCompare') {
        const rA = yearRange(applied.year)
        const rB = yearRange(applied.year2)
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
    enabled: drillOpen && !!drillParams.start,
    queryFn: () => listTransactions({ ...drillParams, page: 1, rows: 50 } as never),
  })

  const viewportH = useViewportTableHeight(320)
  const chartHeight = Math.min(viewportH, 380)

  if (!cfg) return <DataPageLayout title="Report not found"><EmptyState title="Report not found" /></DataPageLayout>

  const disabled = chartLoading

  let chartOption = emptyChartOption()
  let insights = [{ text: 'Adjust filters and click Apply.' }]
  let tableData: Record<string, unknown>[] = []
  let tableCols: FsColumn<Record<string, unknown>>[] = []
  let tableSummary: Record<string, number | string> | undefined
  let kpis: { key: string; label: string; value: string; color?: string }[] = []

  if (data && 'rows' in data && data.rows) {
    const rows = data.rows.filter((r) => r.key && Number.isFinite(r.value))
    const total = rows.reduce((t, r) => t + r.value, 0)
    insights = fromCategorySpend(rows, total, String(applied.year))
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
      insights = [{ text: `${cfg.title} for ${applied.year}.` }]
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
    insights = fromIncomeExpense(rows, String(applied.year))
    const incomeTotal = rows.reduce((t, r) => t + r.income, 0)
    const expenseTotal = rows.reduce((t, r) => t + r.expense, 0)
    kpis = [
      { key: 'inc', label: 'Income', value: formatMoney(incomeTotal), color: finsightColors.income },
      { key: 'exp', label: 'Expense', value: formatMoney(expenseTotal), color: finsightColors.expense },
      { key: 'sur', label: 'Surplus', value: formatMoney(incomeTotal - expenseTotal) },
    ]
    chartOption = {
      title: { text: `Income vs Expense · ${applied.year}` },
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
    insights = fromYearCompare(totalA, totalB, String(applied.year), String(applied.year2))
    const deltaPct = totalA > 0 ? ((totalB - totalA) / totalA) * 100 : 0
    kpis = [
      { key: 'y1', label: `Year ${applied.year}`, value: formatMoney(totalA) },
      { key: 'y2', label: `Year ${applied.year2}`, value: formatMoney(totalB) },
      { key: 'delta', label: 'Δ%', value: `${deltaPct >= 0 ? '+' : ''}${deltaPct.toFixed(1)}%`, color: deltaPct > 0 ? finsightColors.expense : '#0891b2' },
    ]
    const topA = [...data.a].sort((x, y) => y.value - x.value).slice(0, 8).map((r) => ({ name: r.key, value: r.value }))
    const topB = [...(data.b || [])].sort((x, y) => y.value - x.value).slice(0, 8).map((r) => ({ name: r.key, value: r.value }))
    chartOption = {
      title: { text: `Category comparison · ${applied.year} vs ${applied.year2}` },
      series: [
        { type: 'pie', radius: ['40%', '65%'], center: ['30%', '55%'], data: topA, label: { fontSize: 11 } },
        { type: 'pie', radius: ['40%', '65%'], center: ['72%', '55%'], data: topB, label: { fontSize: 11 } },
      ],
    }
  }

  const onChartClick = (params: unknown) => {
    const p = params as { name?: string }
    const r = cfg.dateRange
      ? { start: formatDateMmDdYyyy(applied.dateRange[0]), end: formatDateMmDdYyyy(applied.dateRange[1]) }
      : yearRange(applied.year)
    setDrillParams({
      transactionDateStartStr: r.start,
      transactionDateEndStr: r.end,
      consumeID: p.name || '',
      txnTypes: cfg.txnType || 'expense',
    })
    setDrillOpen(true)
  }

  const handleApply = () => apply(() => refetch())

  return (
    <DataPageLayout
      title={cfg.title}
      subtitle={cfg.subtitle}
      icon={<BarChartOutlined />}
      toolbar={(
        <FilterToolbar loading={chartLoading} onApply={handleApply} dirty={isDirty}>
          <Select size="small" value={draft.year} disabled={disabled} onChange={(v) => setDraft((d) => ({ ...d, year: v }))} style={{ width: 90 }} options={yearOptions(16, curYear)} />
          {cfg.compareYear && (
            <Select size="small" value={draft.year2} disabled={disabled} onChange={(v) => setDraft((d) => ({ ...d, year2: v }))} style={{ width: 90 }} options={yearOptions(16, curYear)} />
          )}
          {cfg.dateRange && (
            <RangePicker
              size="small"
              value={draft.dateRange}
              disabled={disabled}
              presets={dateRangePresets}
              onChange={(v) => v && setDraft((d) => ({ ...d, dateRange: [v[0]!, v[1]!] }))}
            />
          )}
          <Select size="small" allowClear placeholder="Card" disabled={disabled} style={{ width: 120 }}
            options={(cards || []).map((c) => ({ value: c.key, label: c.value }))}
            value={draft.card || undefined} onChange={(v) => setDraft((d) => ({ ...d, card: v || '' }))} />
          <TreeSelect size="small" allowClear placeholder="Category" disabled={disabled} style={{ width: 150 }} treeData={treeData}
            value={draft.consume || undefined} onChange={(v) => setDraft((d) => ({ ...d, consume: v || '' }))} />
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
              { title: 'Amount', dataIndex: 'balanceMoney', unit: 'CNY', align: 'right', sortType: 'number' },
            ]}
            dataSource={(drillRows?.rows || []) as unknown as Record<string, unknown>[]}
            rowKey="id"
          />
        )}
      </Drawer>
    </DataPageLayout>
  )
}

function summaryLabel() {
  return 'Total'
}
