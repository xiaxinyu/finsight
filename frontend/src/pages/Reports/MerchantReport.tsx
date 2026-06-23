import { useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Col, Row, Select, Tag, Typography, message } from 'antd'
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
import { DataQualityStrip } from '../../components/DataQualityStrip'
import { EmptyState } from '../../components/EmptyState'
import { FsChart } from '../../components/FsChart'
import { FsDataTable } from '../../components/FsDataTable'
import { InsightPanel } from '../../components/InsightPanel'
import { UnifiedDrillDrawer } from '../../components/ReportDrillDrawer'
import { buildReportDrillContext } from '../../components/drilldown/buildDrillContext'
import { useDrillDown } from '../../hooks/useDrillDown'
import { ReportKpiStrip } from '../../components/ReportKpiStrip'
import { formatMoney } from '../../utils/format'
import {
  buildConcentrationChart,
  buildConcentrationKpis,
  buildDriftChart,
  buildSubscriptionInsights,
  buildSubscriptionKpis,
  formatStability,
  type MerchantConcentrationRow,
  type MerchantDriftRow,
  type MerchantSubscription,
} from '../../utils/merchantReports'

export type MerchantReportMode = 'subscriptions' | 'concentration' | 'drift'

type MerchantReportProps = {
  title: string
  subtitle?: string
  mode: MerchantReportMode
}

type DrillableRow = {
  displayName: string
  drillDown?: Record<string, string>
}

export function MerchantReport({ title, subtitle, mode }: MerchantReportProps) {
  const { flags } = useFeatureFlags()
  const qc = useQueryClient()
  const { open: drillOpen, context: drillContext, openDrill, closeDrill } = useDrillDown()
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

  const openMerchantDrill = (row: DrillableRow, explanation: string[]) => {
    if (!row.drillDown) return
    openDrill(buildReportDrillContext({
      title: `${row.displayName} · transactions`,
      metricLabel: row.displayName,
      params: row.drillDown,
      explanation,
      source: 'report',
      provenance: {
        reportId: `merchant-${mode}`,
        sourceView: 'merchant row',
      },
    }))
  }

  const drillRowProps = (row: DrillableRow, explanation: string[]) => ({
    onClick: () => openMerchantDrill(row, explanation),
    style: { cursor: row.drillDown ? 'pointer' : undefined },
  })

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

  const driftCols = [
    { title: 'Merchant', dataIndex: 'displayName', sortType: 'text' as const, ellipsis: true },
    { title: 'Prior', dataIndex: 'priorSpend', cellType: 'money' as const, unit: 'CNY', align: 'right' as const, sortType: 'number' as const },
    { title: 'Current', dataIndex: 'currentSpend', cellType: 'money' as const, unit: 'CNY', align: 'right' as const, sortType: 'number' as const },
    { title: 'Change', dataIndex: 'deltaAmount', cellType: 'deltaMoney' as const, unit: 'CNY', align: 'right' as const, sortType: 'number' as const },
    {
      title: 'Change %',
      dataIndex: 'pctChange',
      align: 'right' as const,
      sortType: 'number' as const,
      render: (v: number | null) => (v == null ? '—' : `${v >= 0 ? '+' : ''}${v.toFixed(1)}%`),
    },
  ]

  const driftRowExplanation = (row: MerchantDriftRow) => (
    `${row.displayName}: ${formatMoney(row.priorSpend)} → ${formatMoney(row.currentSpend)} (Δ ${formatMoney(row.deltaAmount)}). Click to review underlying transactions.`
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
      <DataQualityStrip metricsSource="fin_metric_monthly" compact />
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
              {
                title: 'Last charge',
                dataIndex: 'lastSeen',
                width: 110,
                render: (v: string) => (v ? String(v).slice(0, 10) : '—'),
              },
              {
                title: 'Stability',
                key: 'stability',
                width: 140,
                render: (_: unknown, row: MerchantSubscription) => (
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {formatStability(row)}
                  </Typography.Text>
                ),
              },
              { title: 'Count', dataIndex: 'txnCount', align: 'right', sortType: 'number', width: 72 },
              {
                title: 'Confidence',
                dataIndex: 'confidence',
                align: 'right',
                width: 88,
                render: (v: number) => `${Math.round(v * 100)}%`,
              },
            ]}
            dataSource={subscriptionsQuery.data.subscriptions}
            rowKey="merchantToken"
            loading={loading}
            rowExplanation={(row) => {
              const sub = row as MerchantSubscription
              const parts = [
                `${sub.displayName} · ${formatMoney(sub.monthlyEquivalent)}/mo (${sub.cadence})`,
                sub.evidence || `Confidence ${Math.round(sub.confidence * 100)}%`,
              ]
              if (sub.lastSeen) parts.push(`Last charge ${String(sub.lastSeen).slice(0, 10)}`)
              return parts.join(' · ')
            }}
            onRow={(record) => drillRowProps(record as MerchantSubscription, [
              `${record.displayName}: ${formatMoney(record.monthlyEquivalent)}/month (${record.cadence}).`,
              record.evidence || 'Review recurring charge pattern and underlying transactions.',
            ])}
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
              <FsDataTable<MerchantConcentrationRow>
                title="Top merchants"
                columns={[
                  { title: 'Merchant', dataIndex: 'displayName', sortType: 'text', ellipsis: true },
                  { title: 'Spend', dataIndex: 'totalSpend', cellType: 'money' as const, unit: 'CNY', align: 'right', sortType: 'number' },
                  {
                    title: 'Share',
                    dataIndex: 'sharePct',
                    cellType: 'contribution' as const,
                    align: 'right',
                    sortType: 'number',
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
                rowExplanation={(row: MerchantConcentrationRow) => (
                  `${row.displayName} · ${Number(row.sharePct).toFixed(1)}% of tracked spend. Click to drill into transactions.`
                )}
                onRow={(record) => drillRowProps(record, [
                  `${record.displayName} accounts for ${Number(record.sharePct).toFixed(1)}% of spend (${formatMoney(record.totalSpend)}).`,
                ])}
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
            { key: 'new', label: 'New', value: String(driftQuery.data.newMerchants?.length ?? 0) },
            { key: 'growing', label: 'Growing', value: String(driftQuery.data.growingMerchants?.length ?? 0) },
            { key: 'declining', label: 'Declining', value: String(driftQuery.data.decliningMerchants?.length ?? 0) },
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
                title="All movers"
                columns={driftCols}
                dataSource={driftQuery.data.movers}
                rowKey="merchantToken"
                loading={loading}
                rowExplanation={driftRowExplanation}
                onRow={(record) => drillRowProps(record, [driftRowExplanation(record as MerchantDriftRow)])}
                scroll={{ y: 200 }}
              />
            </Col>
          </Row>
          <Row gutter={[12, 12]} className="fs-report-body">
            <Col xs={24} lg={8}>
              <FsDataTable
                title="New merchants"
                columns={driftCols}
                dataSource={driftQuery.data.newMerchants ?? []}
                rowKey="merchantToken"
                loading={loading}
                rowExplanation={driftRowExplanation}
                onRow={(record) => drillRowProps(record, [driftRowExplanation(record as MerchantDriftRow)])}
                scroll={{ y: 180 }}
              />
            </Col>
            <Col xs={24} lg={8}>
              <FsDataTable
                title="Growing merchants"
                columns={driftCols}
                dataSource={driftQuery.data.growingMerchants ?? []}
                rowKey="merchantToken"
                loading={loading}
                rowExplanation={driftRowExplanation}
                onRow={(record) => drillRowProps(record, [driftRowExplanation(record as MerchantDriftRow)])}
                scroll={{ y: 180 }}
              />
            </Col>
            <Col xs={24} lg={8}>
              <FsDataTable
                title="Declining merchants"
                columns={driftCols}
                dataSource={driftQuery.data.decliningMerchants ?? []}
                rowKey="merchantToken"
                loading={loading}
                rowExplanation={driftRowExplanation}
                onRow={(record) => drillRowProps(record, [driftRowExplanation(record as MerchantDriftRow)])}
                scroll={{ y: 180 }}
              />
            </Col>
          </Row>
        </>
      )}

      <UnifiedDrillDrawer open={drillOpen} context={drillContext} onClose={closeDrill} />
    </DataPageLayout>
  )
}
