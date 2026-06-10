import { useQuery } from '@tanstack/react-query'
import { Alert, Col, Progress, Row, Table, Tooltip } from 'antd'
import { BankOutlined, InfoCircleOutlined } from '@ant-design/icons'
import { wealthSnapshot } from '../../api/finance'
import { DataPageLayout } from '../../components/DataPageLayout'
import { KpiGrid } from '../../components/KpiGrid'
import { ContentCard } from '../../components/ContentCard'
import { EmptyState } from '../../components/EmptyState'
import { finsightColors } from '../../styles/finsight-tokens'
import { formatMoney } from '../../utils/format'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { Link } from 'react-router-dom'

const HEALTH_FORMULAS: Record<string, string> = {
  liquidity: 'Runway months × 16.67 (target ~6 months)',
  savingsRate: 'YTD savings rate as % of income',
  fixedBurden: 'Fixed costs / YTD income',
  debtPressure: 'Liabilities / assets',
  emergencyMonths: 'Liquid assets / monthly burn rate',
}

export function WealthPage() {
  const tableHeight = useViewportTableHeight(280)
  const { data, isLoading, isError, error } = useQuery({ queryKey: ['wealth'], queryFn: wealthSnapshot })

  const netWorth = Number(data?.netWorth || 0)
  const assets = Number(data?.assets || 0)
  const liabilities = Number(data?.liabilities || 0)
  const savingsRate = Number(data?.savingsRate || 0)
  const health = (data?.healthScore || {}) as Record<string, number>
  const accounts = (data?.accounts || []) as { key: string; value: number }[]

  return (
    <DataPageLayout
      title="Wealth"
      subtitle="Net worth, balance sheet, and health score"
      icon={<BankOutlined />}
    >
      {isError && (
        <Alert type="error" showIcon style={{ marginBottom: 8 }}
          message="Failed to load wealth data"
          description={error instanceof Error ? error.message : 'Please sign in again.'} />
      )}
      {accounts.length === 0 && !isLoading && (
        <Alert type="info" showIcon style={{ marginBottom: 8 }}
          message="No account balances yet"
          description={<>Import statements or <Link to="/admin/cards">add bank cards</Link> — balances are inferred from the latest imported closing balance.</>} />
      )}
      <KpiGrid items={[
        { key: 'nw', label: 'Net worth', value: formatMoney(netWorth) },
        { key: 'a', label: 'Assets', value: formatMoney(assets), color: finsightColors.income },
        { key: 'l', label: 'Liabilities', value: formatMoney(liabilities), color: finsightColors.expense },
        { key: 'sr', label: 'Savings rate', value: `${(savingsRate * 100).toFixed(1)}%` },
      ]} />

      <Row gutter={[12, 12]}>
        <Col xs={24} lg={14}>
          <ContentCard title="Accounts" size="small">
            <Table
              className="fs-data-table"
              size="small"
              loading={isLoading}
              pagination={false}
              scroll={{ y: tableHeight }}
              rowKey="key"
              locale={{ emptyText: <EmptyState compact title="No accounts" description="Sync from bank cards after import." /> }}
              dataSource={accounts}
              columns={[
                { title: 'Account', dataIndex: 'key' },
                { title: 'Balance', dataIndex: 'value', align: 'right', render: (v) => formatMoney(Number(v)) },
              ]}
            />
          </ContentCard>
        </Col>
        <Col xs={24} lg={10}>
          <ContentCard title="Health score 2.0" size="small">
            <div style={{ marginBottom: 12 }}>
              <strong>Overall: {Math.round(Number(health.total || 0))}</strong>
            </div>
            {['liquidity', 'savingsRate', 'fixedBurden', 'debtPressure', 'emergencyMonths'].map((k) => (
              <div key={k} style={{ marginBottom: 8 }}>
                <div style={{ fontSize: 11, textTransform: 'capitalize', display: 'flex', alignItems: 'center', gap: 4 }}>
                  {k.replace(/([A-Z])/g, ' $1')}
                  <Tooltip title={HEALTH_FORMULAS[k]}>
                    <InfoCircleOutlined style={{ fontSize: 10, opacity: 0.5 }} />
                  </Tooltip>
                </div>
                <Progress percent={Math.min(100, Number(health[k] || 0))} size="small" showInfo />
              </div>
            ))}
          </ContentCard>
        </Col>
      </Row>
    </DataPageLayout>
  )
}
