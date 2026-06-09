import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Col, Row, Select, Space, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { homeSummary, fetchReport } from '../../api/report'
import { FsChart } from '../../components/FsChart'
import { KpiGrid } from '../../components/KpiGrid'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { finsightColors } from '../../styles/finsight-tokens'
import { formatMoney, yearOptions, yearRange } from '../../utils/format'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'

export function DashboardPage() {
  const curYear = new Date().getFullYear()
  const [year, setYear] = useState(curYear)
  const chartHeight = Math.min(useViewportTableHeight(280), 400)

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
    <DataPageLayout
      title="Dashboard"
      actions={(
        <Select size="small" value={year} onChange={setYear} style={{ width: 100 }} options={yearOptions(16, curYear)} />
      )}
    >
      {loadError && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 8 }}
          message="Failed to load dashboard data"
          description={loadError instanceof Error ? loadError.message : 'Please sign in again.'}
        />
      )}
      <KpiGrid items={[
        { key: 'income', label: 'Income', value: formatMoney(income), color: finsightColors.income },
        { key: 'expense', label: 'Expense', value: formatMoney(expense), color: finsightColors.expense },
        { key: 'surplus', label: 'Surplus', value: formatMoney(surplus) },
        { key: 'savings', label: 'Savings rate', value: `${savingsRate}%` },
      ]} />
      <Row gutter={[12, 12]}>
        <Col xs={24} lg={14}>
          <ContentCard title="Top expense categories" size="small" styles={{ body: { padding: 8 } }}>
            <FsChart
              profile="donut"
              height={chartHeight}
              loading={isLoading || catsLoading}
              option={{
                title: { text: 'Category mix', left: 'center', textStyle: { fontSize: 13 } },
                series: [{ type: 'pie', radius: ['44%', '70%'], center: ['55%', '52%'], data: pieData, label: { fontSize: 11 } }],
                legend: { orient: 'vertical', left: 'left', top: 'middle', textStyle: { fontSize: 11 } },
              }}
            />
          </ContentCard>
        </Col>
        <Col xs={24} lg={10}>
          <ContentCard title="Quick links" size="small" styles={{ body: { padding: '10px 12px' } }}>
            <Space direction="vertical" size={4}>
              <Link to="/reports/income-vs-expense">Income vs Expense</Link>
              <Link to="/reports/monthly-comparison">Monthly Comparison</Link>
              <Link to="/transactions">Transaction detail</Link>
              <Link to="/ledgers/expense">Expense ledger</Link>
            </Space>
            {summary?.summary_text != null && (
              <Typography.Text type="secondary" style={{ display: 'block', marginTop: 12, fontSize: 12 }} title={String(summary.summary_text)}>
                {String(summary.summary_text)}
              </Typography.Text>
            )}
          </ContentCard>
        </Col>
      </Row>
    </DataPageLayout>
  )
}
