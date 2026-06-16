import { useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Col, Row, Select, Tag, message } from 'antd'
import { BarChartOutlined, ReloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  fetchMerchantConcentration,
  fetchMerchantDrift,
  fetchSubscriptionReport,
  refreshMerchantProfiles,
} from '../../api/analytics'
import { useFeatureFlags } from '../../hooks/useFeatureFlags'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { FsChart } from '../../components/FsChart'
import { FsDataTable } from '../../components/FsDataTable'
import { InsightPanel } from '../../components/InsightPanel'
import { ReportKpiStrip } from '../../components/ReportKpiStrip'
import { formatMoney } from '../../utils/format'
import {
  buildConcentrationChart,
  buildConcentrationKpis,
  buildDriftChart,
  buildSubscriptionInsights,
  buildSubscriptionKpis,
} from '../../utils/merchantReports'

export type MerchantReportMode = 'subscriptions' | 'concentration' | 'drift'

type MerchantReportProps = {
  title: string
  subtitle?: string
  mode: MerchantReportMode
}

export function MerchantReport({ title, subtitle, mode }: MerchantReportProps) {
  const { flags } = useFeatureFlags()
  const qc = useQueryClient()
  const [year, setYear] = useState(dayjs().year())
  const [refreshing, setRefreshing] = useState(false)

  const enabled = flags.merchantMining

  const subscriptionsQuery = useQuery({
    queryKey: ['merchant-subscriptions'],
    queryFn: fetchSubscriptionReport,
    enabled: enabled && (mode === 'subscriptions' || mode === 'concentration'),
  })

  const concentrationQuery = useQuery({
    queryKey: ['merchant-concentration'],
    queryFn: fetchMerchantConcentration,
    enabled: enabled && mode === 'concentration',
  })

  const driftQuery = useQuery({
    queryKey: ['merchant-drift', year],
    queryFn: () => fetchMerchantDrift(year),
    enabled: enabled && mode === 'drift',
  })

  const loading = subscriptionsQuery.isLoading || subscriptionsQuery.isFetching
    || concentrationQuery.isLoading || concentrationQuery.isFetching
    || driftQuery.isLoading || driftQuery.isFetching

  const error = subscriptionsQuery.error || concentrationQuery.error || driftQuery.error
  const isError = subscriptionsQuery.isError || concentrationQuery.isError || driftQuery.isError

  const onRefresh = async () => {
    setRefreshing(true)
    try {
      const result = await refreshMerchantProfiles()
      message.success(`Refreshed ${result.upserted} merchants (${result.subscriptions} subscriptions)`)
      await qc.invalidateQueries({ queryKey: ['merchant-subscriptions'] })
      await qc.invalidateQueries({ queryKey: ['merchant-concentration'] })
      await qc.invalidateQueries({ queryKey: ['merchant-drift'] })
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Refresh failed')
    } finally {
      setRefreshing(false)
    }
  }

  const subscriptionKpis = useMemo(
    () => (subscriptionsQuery.data ? buildSubscriptionKpis(subscriptionsQuery.data) : []),
    [subscriptionsQuery.data],
  )
  const subscriptionInsights = useMemo(
    () => (subscriptionsQuery.data ? buildSubscriptionInsights(subscriptionsQuery.data) : []),
    [subscriptionsQuery.data],
  )
  const concentrationKpis = useMemo(
    () => (concentrationQuery.data ? buildConcentrationKpis(concentrationQuery.data) : []),
    [concentrationQuery.data],
  )
  const concentrationChart = useMemo(
    () => (concentrationQuery.data ? buildConcentrationChart(concentrationQuery.data) : {}),
    [concentrationQuery.data],
  )
  const driftChart = useMemo(
    () => (driftQuery.data ? buildDriftChart(driftQuery.data) : {}),
    [driftQuery.data],
  )

  const toolbar = (
    <div className="fs-cash-risk-toolbar">
      {mode === 'drift' && (
        <Select
          size="small"
          value={year}
          style={{ width: 110 }}
          options={[year - 1, year, year + 1].map((y) => ({ value: y, label: String(y) }))}
          onChange={setYear}
        />
      )}
      <Button size="small" icon={<ReloadOutlined />} loading={refreshing} onClick={onRefresh}>
        Refresh merchants
      </Button>
    </div>
  )

  return (
    <DataPageLayout
      title={title}
      subtitle={subtitle}
      icon={<BarChartOutlined />}
      className="fs-data-page--dense fs-data-page--reports"
      toolbar={toolbar}
    >
      {!enabled && (
        <EmptyState title="Merchant mining disabled" description="Enable finsight.merchant-mining.enabled to use merchant reports." />
      )}

      {enabled && isError && (
        <Alert
          type="error"
          showIcon
          message="Failed to load merchant report"
          description={error instanceof Error ? error.message : 'Try refreshing merchant profiles.'}
        />
      )}

      {enabled && mode === 'subscriptions' && subscriptionsQuery.data && (
        <>
          <ReportKpiStrip items={subscriptionKpis} />
          <InsightPanel bullets={subscriptionInsights} title="Subscriptions" />
          <FsDataTable
            title="Suspected subscriptions"
            columns={[
              { title: 'Merchant', dataIndex: 'displayName', sortType: 'text' },
              { title: 'Cadence', dataIndex: 'cadence', width: 100 },
              {
                title: 'Monthly eq.',
                dataIndex: 'monthlyEquivalent',
                unit: 'CNY',
                align: 'right',
                sortType: 'number',
              },
              { title: 'Avg charge', dataIndex: 'avgAmount', unit: 'CNY', align: 'right', sortType: 'number' },
              { title: 'Count', dataIndex: 'txnCount', align: 'right', sortType: 'number', width: 80 },
              {
                title: 'Confidence',
                dataIndex: 'confidence',
                align: 'right',
                width: 100,
                render: (v: number) => `${Math.round(v * 100)}%`,
              },
            ]}
            dataSource={subscriptionsQuery.data.subscriptions}
            rowKey="merchantToken"
            loading={loading}
            locale={{ emptyText: <EmptyState compact title="No subscriptions" description="Refresh merchants after importing expense transactions." /> }}
          />
        </>
      )}

      {enabled && mode === 'concentration' && concentrationQuery.data && (
        <>
          <ReportKpiStrip items={concentrationKpis} />
          <Row gutter={[12, 12]} className="fs-report-body">
            <Col xs={24} lg={14}>
              <ContentCard title="Merchant share" size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile="categoryBar"
                  height={360}
                  loading={loading}
                  option={concentrationChart}
                  empty={<EmptyState compact title="No merchant data" />}
                />
              </ContentCard>
            </Col>
            <Col xs={24} lg={10}>
              <FsDataTable
                title="Top merchants"
                columns={[
                  { title: 'Merchant', dataIndex: 'displayName', sortType: 'text', ellipsis: true },
                  { title: 'Spend', dataIndex: 'totalSpend', unit: 'CNY', align: 'right', sortType: 'number' },
                  {
                    title: 'Share',
                    dataIndex: 'sharePct',
                    align: 'right',
                    sortType: 'number',
                    render: (v: number) => `${v.toFixed(1)}%`,
                  },
                  {
                    title: 'Type',
                    key: 'type',
                    width: 100,
                    render: (_: unknown, row: { suspectedSubscription: boolean }) => (
                      row.suspectedSubscription ? <Tag color="blue">Subscription</Tag> : null
                    ),
                  },
                ]}
                dataSource={concentrationQuery.data.merchants}
                rowKey="merchantToken"
                loading={loading}
                scroll={{ y: 320 }}
              />
            </Col>
          </Row>
        </>
      )}

      {enabled && mode === 'drift' && driftQuery.data && (
        <>
          <ReportKpiStrip items={[
            { key: 'year', label: 'Year', value: String(driftQuery.data.year) },
            { key: 'prior', label: 'Compare', value: String(driftQuery.data.priorYear) },
            { key: 'movers', label: 'Movers', value: String(driftQuery.data.movers.length) },
          ]} />
          <Row gutter={[12, 12]} className="fs-report-body">
            <Col xs={24} lg={14}>
              <ContentCard title="Spend change by merchant" size="small" styles={{ body: { padding: 8 } }}>
                <FsChart
                  profile="compareBars"
                  height={360}
                  loading={loading}
                  option={driftChart}
                  empty={<EmptyState compact title="No drift data" />}
                />
              </ContentCard>
            </Col>
            <Col xs={24} lg={10}>
              <FsDataTable
                title="Top movers"
                columns={[
                  { title: 'Merchant', dataIndex: 'displayName', sortType: 'text', ellipsis: true },
                  { title: 'Prior', dataIndex: 'priorSpend', unit: 'CNY', align: 'right', sortType: 'number' },
                  { title: 'Current', dataIndex: 'currentSpend', unit: 'CNY', align: 'right', sortType: 'number' },
                  {
                    title: 'Change',
                    dataIndex: 'deltaAmount',
                    unit: 'CNY',
                    align: 'right',
                    sortType: 'number',
                    render: (v: number) => (
                      <span style={{ color: v > 0 ? '#dc2626' : '#16a34a', fontWeight: 600 }}>
                        {v > 0 ? '+' : ''}{formatMoney(v)}
                      </span>
                    ),
                  },
                ]}
                dataSource={driftQuery.data.movers}
                rowKey="merchantToken"
                loading={loading}
                scroll={{ y: 320 }}
              />
            </Col>
          </Row>
        </>
      )}
    </DataPageLayout>
  )
}
