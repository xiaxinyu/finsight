import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Col, Row, Select, Typography } from 'antd'
import { PageContainer } from '@ant-design/pro-components'
import { Link } from 'react-router-dom'
import { homeSummary, fetchReport } from '../../api/report'
import { FsChart } from '../../components/FsChart'
import { KpiGrid } from '../../components/KpiGrid'
import { ContentCard } from '../../components/ContentCard'
import { formatMoney, yearOptions, yearRange } from '../../utils/format'

export function DashboardPage() {
  const curYear = new Date().getFullYear()
  const [year, setYear] = useState(curYear)

  const { data: summary, isLoading, isError, error } = useQuery({
    queryKey: ['home-summary', year],
    queryFn: () => homeSummary(year),
  })

  const { data: topCats, isFetching: catsLoading, isError: catsError, error: catsErr } = useQuery({
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

  const pieData = (topCats || []).filter((r) => r.value > 0).slice(0, 8).map((r) => ({ name: r.key, value: r.value }))

  const loadError = isError ? error : catsError ? catsErr : null

  return (
    <PageContainer title="Dashboard" subTitle={`Year ${year}`}>
      {loadError && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          message="Failed to load dashboard data"
          description={loadError instanceof Error ? loadError.message : 'Please sign in again or check the server logs.'}
        />
      )}
      <Select
        value={year}
        onChange={setYear}
        style={{ width: 120, marginBottom: 20 }}
        options={yearOptions(16, curYear)}
      />
      <KpiGrid items={[
        { key: 'income', label: 'Income', value: formatMoney(income), color: '#10b981' },
        { key: 'expense', label: 'Expense', value: formatMoney(expense), color: '#f59e0b' },
        { key: 'surplus', label: 'Surplus', value: formatMoney(surplus) },
        { key: 'savings', label: 'Savings rate', value: `${savingsRate}%` },
      ]} />
      <Row gutter={[20, 20]}>
        <Col xs={24} lg={12}>
          <ContentCard title="Top expense categories">
            <FsChart
              profile="donut"
              height={360}
              loading={isLoading || catsLoading}
              option={{
                title: { text: 'Category mix', left: 'center' },
                series: [{ type: 'pie', radius: ['44%', '70%'], center: ['55%', '52%'], data: pieData, label: { fontSize: 11 } }],
                legend: { orient: 'vertical', left: 'left', top: 'middle' },
              }}
            />
          </ContentCard>
        </Col>
        <Col xs={24} lg={12}>
          <ContentCard title="Quick links">
            <Typography.Paragraph style={{ marginBottom: 16 }}>
              <Link to="/reports/income-vs-expense">Income vs Expense</Link><br />
              <Link to="/reports/monthly-comparison">Monthly Comparison</Link><br />
              <Link to="/transactions">Transaction detail</Link><br />
              <Link to="/ledgers/expense">Expense ledger</Link>
            </Typography.Paragraph>
            {summary?.summary_text != null && (
              <Typography.Text type="secondary" title={String(summary.summary_text)}>
                {String(summary.summary_text)}
              </Typography.Text>
            )}
          </ContentCard>
        </Col>
      </Row>
    </PageContainer>
  )
}
