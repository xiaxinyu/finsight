import { useQuery } from '@tanstack/react-query'
import { Col, Progress, Row, Table } from 'antd'
import { BankOutlined } from '@ant-design/icons'
import { wealthSnapshot } from '../../api/finance'
import { DataPageLayout } from '../../components/DataPageLayout'
import { KpiGrid } from '../../components/KpiGrid'
import { ContentCard } from '../../components/ContentCard'
import { finsightColors } from '../../styles/finsight-tokens'
import { formatMoney } from '../../utils/format'

export function WealthPage() {
  const { data, isLoading } = useQuery({ queryKey: ['wealth'], queryFn: wealthSnapshot })

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
              rowKey="key"
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
                <div style={{ fontSize: 11, textTransform: 'capitalize' }}>{k.replace(/([A-Z])/g, ' $1')}</div>
                <Progress percent={Math.min(100, Number(health[k] || 0))} size="small" showInfo />
              </div>
            ))}
          </ContentCard>
        </Col>
      </Row>
    </DataPageLayout>
  )
}
