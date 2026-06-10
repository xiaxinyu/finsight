import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  Alert, Col, Progress, Row, Space, Tag,
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
import { decisionCards, financialPulse, type DecisionCard } from '../../api/finance'
import { FsChart } from '../../components/FsChart'
import { KpiGrid } from '../../components/KpiGrid'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { PageSkeleton } from '../../components/PageSkeleton'
import { QuickLinkGrid } from '../../components/QuickLinkGrid'
import { finsightColors } from '../../styles/finsight-tokens'
import { PeriodRangePicker, periodToStrings } from '../../components/PeriodRangePicker'
import { formatMoney } from '../../utils/format'
import { defaultPeriodRange, formatPeriodPreview, type PeriodRange } from '../../utils/periodPresets'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'

export function DashboardPage() {
  const [period, setPeriod] = useState<PeriodRange>(() => defaultPeriodRange())
  const periodKey = periodToStrings(period)
  const chartHeight = Math.min(useViewportTableHeight(280), 400)

  const { data: summary, isLoading, isError, error } = useQuery({
    queryKey: ['home-summary', period[0].year()],
    queryFn: () => homeSummary(period[0].year()),
  })

  const { data: periodTotals, isFetching: totalsLoading } = useQuery({
    queryKey: ['dash-totals', periodKey],
    queryFn: async () => {
      const base = {
        transactionDateStartStr: periodKey.start,
        transactionDateEndStr: periodKey.end,
      }
      const [inc, exp] = await Promise.all([
        fetchReport('/transaction-report/month-income', { ...base, txnTypes: 'income' }),
        fetchReport('/transaction-report/month-expense', { ...base, txnTypes: 'expense' }),
      ])
      return {
        income: inc.reduce((s, r) => s + r.value, 0),
        expense: exp.reduce((s, r) => s + r.value, 0),
      }
    },
  })

  const { data: pulse, isError: pulseError, error: pulseErr } = useQuery({
    queryKey: ['financial-pulse'],
    queryFn: financialPulse,
  })
  const { data: cards, isError: cardsError, error: cardsErr } = useQuery({
    queryKey: ['decision-cards'],
    queryFn: decisionCards,
  })

  const { data: topCats, isFetching: catsLoading, isError: catsError, error: catsErr } = useQuery({
    queryKey: ['dash-top', periodKey],
    queryFn: () => fetchReport('/transaction-report/consume', {
      transactionDateStartStr: periodKey.start,
      transactionDateEndStr: periodKey.end,
      txnTypes: 'expense',
    }),
  })

  const income = Number(periodTotals?.income ?? 0)
  const expense = Number(periodTotals?.expense ?? 0)
  const surplus = income - expense
  const savingsRate = income > 0 ? ((Math.max(0, surplus) / income) * 100).toFixed(1) : '0.0'
  const healthScore = (summary?.health_score || summary?.healthScore) as Record<string, number> | undefined

  const pieData = (topCats || []).filter((r) => r.value > 0).slice(0, 8).map((r) => ({ name: r.key, value: r.value }))
  const loadError = isError ? error : pulseError ? pulseErr : cardsError ? cardsErr : catsError ? catsErr : null
  const loading = isLoading || catsLoading || totalsLoading
  const periodLabel = formatPeriodPreview(period[0], period[1])
  const needsOnboarding = income === 0 && expense === 0 && Number(pulse?.liquidAssets || 0) === 0

  return (
    <DataPageLayout
      title="Financial Pulse"
      subtitle={`Accounts, cash flow, and data quality · ${periodLabel}`}
      icon={<DashboardOutlined />}
      actions={(
        <PeriodRangePicker
          size="small"
          value={period}
          onChange={(range) => setPeriod(range)}
        />
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
      {needsOnboarding && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 8 }}
          message="Get started with FinSight"
          description={(
            <ol style={{ margin: '4px 0 0', paddingLeft: 18 }}>
              <li><Link to="/admin/cards">Add bank cards</Link> in Admin</li>
              <li><Link to="/statements/upload">Import a statement</Link> to populate transactions</li>
              <li><Link to="/planning">Set a monthly budget</Link> and add recurring bills</li>
            </ol>
          )}
        />
      )}
      {loading && !summary ? (
        <PageSkeleton />
      ) : (
        <>
          {(cards || []).map((c: DecisionCard, i: number) => (
            <Alert
              key={i}
              type={c.type === 'warning' ? 'warning' : 'info'}
              showIcon
              message={c.title || c.text}
              description={c.detail}
              action={<Link to={c.actionPath}>{c.actionLabel || 'View'}</Link>}
              style={{ marginBottom: 6 }}
            />
          ))}
          <KpiGrid items={[
            { key: 'income', label: `Income (${periodLabel})`, value: formatMoney(income), color: finsightColors.income, icon: <RiseOutlined style={{ color: finsightColors.income }} /> },
            { key: 'expense', label: `Expense (${periodLabel})`, value: formatMoney(expense), color: finsightColors.expense, icon: <FallOutlined style={{ color: finsightColors.expense }} /> },
            { key: 'savings', label: `Savings rate (${periodLabel})`, value: `${savingsRate}%`, icon: <RiseOutlined style={{ color: finsightColors.income }} /> },
            { key: 'mtd', label: 'Net flow (MTD)', value: formatMoney(Number(pulse?.netFlowMtd || 0)), icon: <FundOutlined /> },
            { key: 'liquid', label: 'Liquid assets (current)', value: formatMoney(Number(pulse?.liquidAssets || 0)), icon: <LineChartOutlined /> },
          ]} />
          {pulse?.dataQuality && (
            <Space wrap style={{ marginBottom: 8 }}>
              <Link to="/transactions?unclassified=1">
                <Tag style={{ cursor: 'pointer' }}>Unclassified: {pulse.dataQuality.unclassifiedCount}</Tag>
              </Link>
              <Tag color={pulse.dataQuality.duplicateCount > 0 ? 'orange' : 'default'}>
                Duplicates: {pulse.dataQuality.duplicateCount}
              </Tag>
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
            <Col xs={24} lg={healthScore ? 10 : 14}>
              <ContentCard title="Top expense categories" size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile="donut"
                  height={chartHeight}
                  loading={loading}
                  empty={<EmptyState compact title="No categories" description={`No expense data for ${periodLabel}.`} />}
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
            {healthScore && (
              <Col xs={24} lg={4}>
                <ContentCard title={`Health score (${period[0].year()})`} size="small" styles={{ body: { padding: '8px 12px' } }}>
                  <div style={{ marginBottom: 8 }}><strong>{Math.round(Number(healthScore.total || 0))}</strong> / 100</div>
                  {Object.entries(healthScore).filter(([k]) => k !== 'total').map(([k, v]) => (
                    <div key={k} style={{ marginBottom: 6 }}>
                      <div style={{ fontSize: 10, textTransform: 'capitalize' }}>{k.replace(/_/g, ' ')}</div>
                      <Progress percent={Math.min(100, Number(v))} size="small" showInfo={false} />
                    </div>
                  ))}
                </ContentCard>
              </Col>
            )}
            <Col xs={24} lg={healthScore ? 10 : 10}>
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
