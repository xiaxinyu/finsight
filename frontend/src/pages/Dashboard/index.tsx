import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card, Col, Row, Select, Statistic, Typography } from 'antd'
import { PageContainer } from '@ant-design/pro-components'
import { Link } from 'react-router-dom'
import { homeSummary, fetchReport } from '../../api/report'
import { FsChart } from '../../components/FsChart'
import { formatMoney, yearRange } from '../../utils/format'

export function DashboardPage() {
  const curYear = new Date().getFullYear()
  const [year, setYear] = useState(curYear)

  const { data: summary, isLoading } = useQuery({
    queryKey: ['home-summary', year],
    queryFn: () => homeSummary(year),
  })

  const { data: topCats } = useQuery({
    queryKey: ['dash-top', year],
    queryFn: () => {
      const r = yearRange(year)
      return fetchReport('/transaction-report/consume', { transactionDateStartStr: r.start, transactionDateEndStr: r.end, txnTypes: 'expense' })
    },
  })

  const income = Number(summary?.income_total || summary?.incomeTotal || 0)
  const expense = Number(summary?.expense_total || summary?.expenseTotal || 0)
  const surplus = income - expense
  const savingsRate = income > 0 ? ((Math.max(0, surplus) / income) * 100).toFixed(1) : '0.0'

  const pieData = (topCats || []).slice(0, 8).map((r) => ({ name: r.key, value: r.value }))

  return (
    <PageContainer title="Dashboard" subTitle={`Year ${year}`} loading={isLoading}>
      <Select value={year} onChange={setYear} style={{ width: 120, marginBottom: 16 }}
        options={Array.from({ length: 16 }, (_, i) => ({ value: curYear - i, label: String(curYear - i) }))} />
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}><Card><Statistic title="Income" value={formatMoney(income)} valueStyle={{ color: '#10b981' }} /></Card></Col>
        <Col xs={24} sm={12} lg={6}><Card><Statistic title="Expense" value={formatMoney(expense)} valueStyle={{ color: '#f59e0b' }} /></Card></Col>
        <Col xs={24} sm={12} lg={6}><Card><Statistic title="Surplus" value={formatMoney(surplus)} /></Card></Col>
        <Col xs={24} sm={12} lg={6}><Card><Statistic title="Savings rate" value={`${savingsRate}%`} /></Card></Col>
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="Top expense categories">
            <FsChart profile="donut" height={320} option={{ series: [{ type: 'pie', radius: ['42%', '68%'], data: pieData }] }} />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Quick links">
            <Typography.Paragraph>
              <Link to="/reports/income-vs-expense">Income vs Expense</Link><br />
              <Link to="/reports/monthly-comparison">Monthly Comparison</Link><br />
              <Link to="/transactions">Transaction detail</Link><br />
              <Link to="/ledgers/expense">Expense ledger</Link>
            </Typography.Paragraph>
            {summary?.summary_text != null && <Typography.Text type="secondary">{String(summary.summary_text)}</Typography.Text>}
          </Card>
        </Col>
      </Row>
    </PageContainer>
  )
}
