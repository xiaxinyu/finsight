import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  Alert, Col, Row, Select, Space, Tag,
} from 'antd'
import {
  DashboardOutlined,
  FallOutlined,
  FundOutlined,
  LineChartOutlined,
  RiseOutlined,
  SwapOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons'
import { homeSummary, fetchReport } from '../../api/report'
import { decisionCards, financialPulse } from '../../api/finance'
import { FsChart } from '../../components/FsChart'
import { KpiGrid } from '../../components/KpiGrid'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { PageSkeleton } from '../../components/PageSkeleton'
import { QuickLinkGrid } from '../../components/QuickLinkGrid'
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

  const { data: pulse } = useQuery({ queryKey: ['financial-pulse'], queryFn: financialPulse })
  const { data: cards } = useQuery({ queryKey: ['decision-cards'], queryFn: decisionCards })

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
  const loading = isLoading || catsLoading

  return (
    <DataPageLayout
      title="Financial Pulse"
      subtitle="Accounts, cash flow, and data quality at a glance"
      icon={<DashboardOutlined />}
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
      {loading && !summary ? (
        <PageSkeleton />
      ) : (
        <>
          {(cards || []).map((c, i) => (
            <Alert key={i} type={c.type === 'warning' ? 'warning' : 'info'} showIcon message={c.text}
              action={<Link to={c.actionPath}>View</Link>} style={{ marginBottom: 6 }} />
          ))}
          <KpiGrid items={[
            { key: 'income', label: 'Income YTD', value: formatMoney(income), color: finsightColors.income, icon: <RiseOutlined style={{ color: finsightColors.income }} /> },
            { key: 'expense', label: 'Expense YTD', value: formatMoney(expense), color: finsightColors.expense, icon: <FallOutlined style={{ color: finsightColors.expense }} /> },
            { key: 'mtd', label: 'Net flow MTD', value: formatMoney(Number(pulse?.netFlowMtd || 0)), icon: <FundOutlined /> },
            { key: 'liquid', label: 'Liquid assets', value: formatMoney(Number(pulse?.liquidAssets || 0)), icon: <LineChartOutlined /> },
            { key: 'savings', label: 'Savings rate YTD', value: `${savingsRate}%`, icon: <RiseOutlined style={{ color: finsightColors.income }} /> },
          ]} />
          {pulse?.dataQuality && (
            <Space wrap style={{ marginBottom: 8 }}>
              <Tag>Unclassified: {pulse.dataQuality.unclassifiedCount}</Tag>
              <Tag color={pulse.dataQuality.duplicateCount > 0 ? 'orange' : 'default'}>Duplicates: {pulse.dataQuality.duplicateCount}</Tag>
              <Tag>Transfers: {pulse.dataQuality.transferPairCount}</Tag>
            </Space>
          )}
          {pulse?.accounts && pulse.accounts.length > 0 && (
            <ContentCard title="Account balances" size="small" style={{ marginBottom: 12 }} styles={{ body: { padding: '8px 12px' } }}>
              <Space wrap>
                {pulse.accounts.map((a) => (
                  <Tag key={a.key}>{a.key}: {formatMoney(Number(a.value))}</Tag>
                ))}
              </Space>
            </ContentCard>
          )}
          <Row gutter={[12, 12]}>
            <Col xs={24} lg={14}>
              <ContentCard title="Top expense categories" size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile="donut"
                  height={chartHeight}
                  loading={loading}
                  empty={<EmptyState compact title="No categories" description="No expense data for this year." />}
                  option={{
                    title: { text: 'Category mix', left: 'center', textStyle: { fontSize: 13 } },
                    series: [{
                      type: 'pie',
                      radius: ['44%', '70%'],
                      center: ['55%', '52%'],
                      data: pieData,
                      label: { fontSize: 11 },
                    }],
                    legend: { orient: 'vertical', left: 'left', top: 'middle', textStyle: { fontSize: 11 } },
                  }}
                />
              </ContentCard>
            </Col>
            <Col xs={24} lg={10}>
              <ContentCard title="Quick links" size="small" styles={{ body: { padding: '10px 12px' } }}>
                <QuickLinkGrid items={[
                  { key: 'plan', label: 'Planning', to: '/planning', icon: <FundOutlined />, description: 'Budget & bills' },
                  { key: 'cf', label: 'Cashflow', to: '/reports/cashflow', icon: <SwapOutlined />, description: 'Monthly surplus' },
                  { key: 'wealth', label: 'Wealth', to: '/wealth', icon: <LineChartOutlined />, description: 'Net worth' },
                  { key: 'tx', label: 'Transactions', to: '/transactions', icon: <UnorderedListOutlined />, description: 'Search & transfer' },
                ]} />
                {summary?.summary_text != null && (
                  <Alert type="info" showIcon message={String(summary.summary_text)} style={{ marginTop: 12 }} />
                )}
              </ContentCard>
            </Col>
          </Row>
        </>
      )}
    </DataPageLayout>
  )
}
