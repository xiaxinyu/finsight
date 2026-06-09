import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, DatePicker, Drawer, Row, Select, Space, Statistic, Table, TreeSelect } from 'antd'
import { PageContainer } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import { reportConfigs } from './reportConfigs'
import { fetchReport } from '../../api/report'
import { consumeTree, listCards, listTransactions } from '../../api/transaction'
import { FsChart } from '../../components/FsChart'
import { InsightPanel } from '../../components/InsightPanel'
import { emptyChartOption } from '../../components/charts/profiles'
import { formatDateMmDdYyyy, formatMoney, MONTH_NAMES, yearRange } from '../../utils/format'
import { fromCategorySpend, fromIncomeExpense, fromYearCompare } from '../../utils/insights'
import { MoneyText } from '../../components/MoneyText'

const { RangePicker } = DatePicker

export function ReportPageView() {
  const { reportId = '' } = useParams()
  const cfg = reportConfigs[reportId]
  const curYear = new Date().getFullYear()
  const [year, setYear] = useState(curYear)
  const [year2, setYear2] = useState(curYear - 1)
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([dayjs().startOf('year'), dayjs()])
  const [card, setCard] = useState('')
  const [consume, setConsume] = useState('')
  const [drillOpen, setDrillOpen] = useState(false)
  const [drillParams, setDrillParams] = useState<Record<string, string>>({})

  const { data: cards } = useQuery({ queryKey: ['cards'], queryFn: listCards })
  const { data: tree } = useQuery({ queryKey: ['consume-tree', cfg?.txnType], queryFn: () => consumeTree(cfg?.txnType), enabled: !!cfg })

  const baseParams = useMemo(() => {
    const p: Record<string, unknown> = { txnTypes: cfg?.txnType || 'expense' }
    if (card) p.cardTypeName = card
    if (consume) p.consumeID = consume
    if (cfg?.dateRange) {
      p.transactionDateStartStr = formatDateMmDdYyyy(dateRange[0])
      p.transactionDateEndStr = formatDateMmDdYyyy(dateRange[1])
    } else {
      const r = yearRange(year)
      p.transactionDateStartStr = r.start
      p.transactionDateEndStr = r.end
    }
    return p
  }, [cfg, card, consume, dateRange, year])

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['report', reportId, baseParams, year2],
    enabled: !!cfg,
    queryFn: async () => {
      if (!cfg) return null
      if (cfg.type === 'incomeVsExpense') {
        const r = yearRange(year)
        const base = { transactionDateStartStr: r.start, transactionDateEndStr: r.end, cardTypeName: card, consumeID: consume }
        const [inc, exp] = await Promise.all([
          fetchReport('/transaction-report/month-income', { ...base, txnTypes: 'income' }),
          fetchReport('/transaction-report/month-expense', { ...base, txnTypes: 'expense' }),
        ])
        return { inc, exp }
      }
      if (cfg.type === 'yearCompare') {
        const rA = yearRange(year)
        const rB = yearRange(year2)
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

  const { data: drillRows } = useQuery({
    queryKey: ['drill', drillParams],
    enabled: drillOpen && !!drillParams.start,
    queryFn: () => listTransactions({ ...drillParams, page: 1, rows: 50 } as never),
  })

  if (!cfg) return <PageContainer title="Report not found" />

  const treeData = (tree || []).map(function map(n): { title: string; value: string; children?: ReturnType<typeof map>[] } {
    return { title: n.text, value: n.id, children: n.children?.map(map) }
  })

  let chartOption = emptyChartOption()
  let insights = [{ text: 'Loading...' }]
  let tableCols: { title: string; dataIndex: string; align?: 'right' }[] = []
  let tableData: Record<string, unknown>[] = []
  let kpis: { label: string; value: string }[] = []

  if (data && 'rows' in data && data.rows) {
    const rows = data.rows.filter((r) => r.key && r.value)
    const total = rows.reduce((t, r) => t + r.value, 0)
    insights = fromCategorySpend(rows, total, String(year))
    kpis = [{ label: 'Total', value: formatMoney(total) }, { label: 'Categories', value: String(rows.length) }]
    const top = rows.sort((a, b) => b.value - a.value).slice(0, 10)
    if (cfg.chartKind === 'donut') {
      chartOption = { series: [{ type: 'pie', radius: ['42%', '68%'], data: top.map((r) => ({ name: r.key, value: r.value })) }] }
    } else if (cfg.type === 'weekSummary') {
      const labels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
      const vals = [0, 0, 0, 0, 0, 0, 0]
      rows.forEach((r) => { const i = parseInt(r.key, 10); if (i >= 0 && i < 7) vals[i] = r.value })
      chartOption = { title: { text: cfg.title }, xAxis: { data: labels }, series: [{ type: 'bar', data: vals }] }
    } else if (cfg.type === 'monthlyCompare' || cfg.type === 'timeCurve') {
      const vals = MONTH_NAMES.map((_, i) => Number(data.rows?.[i]?.value || 0))
      chartOption = { title: { text: cfg.title }, xAxis: { data: MONTH_NAMES }, series: [{ type: 'line', data: vals, smooth: true, areaStyle: { opacity: 0.1 } }] }
      kpis = [{ label: 'Year total', value: formatMoney(vals.reduce((a, b) => a + b, 0)) }]
      insights = [{ text: `${cfg.title} for ${year}.` }]
    } else {
      chartOption = { title: { text: cfg.title }, xAxis: { data: top.map((r) => r.key) }, series: [{ type: 'bar', data: top.map((r) => r.value) }] }
    }
    tableCols = [{ title: 'Category', dataIndex: 'key' }, { title: 'Amount', dataIndex: 'value', align: 'right' }]
    tableData = top.map((r) => ({ key: r.key, value: r.value }))
  }

  if (data && 'inc' in data && data.inc && data.exp) {
    const rows = MONTH_NAMES.map((m, i) => ({
      month: m,
      income: Number(data.inc[i]?.value || 0),
      expense: Number(data.exp[i]?.value || 0),
      surplus: Number(data.inc[i]?.value || 0) - Number(data.exp[i]?.value || 0),
    }))
    insights = fromIncomeExpense(rows, String(year))
    const incomeTotal = rows.reduce((t, r) => t + r.income, 0)
    const expenseTotal = rows.reduce((t, r) => t + r.expense, 0)
    kpis = [
      { label: 'Income', value: formatMoney(incomeTotal) },
      { label: 'Expense', value: formatMoney(expenseTotal) },
      { label: 'Surplus', value: formatMoney(incomeTotal - expenseTotal) },
    ]
    chartOption = {
      title: { text: `Income vs Expense · ${year}` },
      legend: { data: ['Income', 'Expense', 'Surplus'] },
      xAxis: { data: MONTH_NAMES },
      series: [
        { name: 'Income', type: 'bar', data: rows.map((r) => r.income), itemStyle: { color: '#10b981' } },
        { name: 'Expense', type: 'bar', data: rows.map((r) => r.expense), itemStyle: { color: '#f59e0b' } },
        { name: 'Surplus', type: 'line', data: rows.map((r) => r.surplus), smooth: true },
      ],
    }
    tableCols = [{ title: 'Month', dataIndex: 'month' }, { title: 'Income', dataIndex: 'income', align: 'right' }, { title: 'Expense', dataIndex: 'expense', align: 'right' }, { title: 'Surplus', dataIndex: 'surplus', align: 'right' }]
    tableData = rows
  }

  if (data && 'a' in data && data.a) {
    const totalA = data.a.reduce((t, r) => t + r.value, 0)
    const totalB = (data.b || []).reduce((t, r) => t + r.value, 0)
    insights = fromYearCompare(totalA, totalB, String(year), String(year2))
    kpis = [{ label: `Year ${year}`, value: formatMoney(totalA) }, { label: `Year ${year2}`, value: formatMoney(totalB) }]
    const topA = data.a.sort((x, y) => y.value - x.value).slice(0, 8).map((r) => ({ name: r.key, value: r.value }))
    const topB = (data.b || []).sort((x, y) => y.value - x.value).slice(0, 8).map((r) => ({ name: r.key, value: r.value }))
    chartOption = { series: [
      { type: 'pie', radius: ['40%', '65%'], center: ['30%', '55%'], data: topA },
      { type: 'pie', radius: ['40%', '65%'], center: ['72%', '55%'], data: topB },
    ] }
  }

  const onChartClick = (params: unknown) => {
    const p = params as { name?: string; seriesName?: string }
    const r = cfg.dateRange ? { start: formatDateMmDdYyyy(dateRange[0]), end: formatDateMmDdYyyy(dateRange[1]) } : yearRange(year)
    setDrillParams({
      transactionDateStartStr: r.start,
      transactionDateEndStr: r.end,
      consumeID: p.name || '',
      txnTypes: cfg.txnType || 'expense',
    })
    setDrillOpen(true)
  }

  return (
    <PageContainer title={cfg.title} loading={isLoading}>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select value={year} onChange={setYear} style={{ width: 100 }} options={Array.from({ length: 16 }, (_, i) => ({ value: curYear - i, label: String(curYear - i) }))} />
          {cfg.compareYear && <Select value={year2} onChange={setYear2} style={{ width: 100 }} options={Array.from({ length: 16 }, (_, i) => ({ value: curYear - i, label: String(curYear - i) }))} />}
          {cfg.dateRange && <RangePicker value={dateRange} onChange={(v) => v && setDateRange([v[0]!, v[1]!])} />}
          <Select allowClear placeholder="Card" style={{ width: 140 }} options={(cards || []).map((c) => ({ value: c.key, label: c.value }))} onChange={(v) => setCard(v || '')} />
          <TreeSelect allowClear placeholder="Category" style={{ width: 180 }} treeData={treeData} onChange={(v) => setConsume(v || '')} />
          <Button type="primary" onClick={() => refetch()}>Apply</Button>
        </Space>
      </Card>
      <InsightPanel bullets={insights} />
      <Row gutter={16} style={{ marginBottom: 16 }}>
        {kpis.map((k) => <Col key={k.label} xs={12} sm={8} lg={6}><Card><Statistic title={k.label} value={k.value} /></Card></Col>)}
      </Row>
      <Row gutter={16}>
        <Col xs={24} lg={tableData.length ? 14 : 24}>
          <Card title={cfg.title}>
            <FsChart profile={cfg.chartProfile || 'timeSeries'} height={400} option={chartOption} onEvents={{ click: onChartClick }} />
          </Card>
        </Col>
        {tableData.length > 0 && (
          <Col xs={24} lg={10}>
            <Card title="Breakdown">
              <Table size="small" pagination={false} dataSource={tableData} rowKey="key" columns={tableCols.map((c) => ({
                ...c,
                render: c.dataIndex === 'value' || c.dataIndex === 'income' || c.dataIndex === 'expense' || c.dataIndex === 'surplus'
                  ? (v: number) => <MoneyText value={v} unit /> : undefined,
              }))} />
            </Card>
          </Col>
        )}
      </Row>
      <Drawer title="Transaction drill-down" width={720} open={drillOpen} onClose={() => setDrillOpen(false)}>
        <Table size="small" rowKey="id" dataSource={drillRows?.rows || []} columns={[
          { title: 'Date', dataIndex: 'transactionDate', width: 100 },
          { title: 'Description', dataIndex: 'transactionDesc', ellipsis: true },
          { title: 'Amount', dataIndex: 'balanceMoney', align: 'right', render: (v) => <MoneyText value={v} unit /> },
        ]} />
      </Drawer>
    </PageContainer>
  )
}
