import { useMemo, useState } from 'react'
import { Alert, Breadcrumb, Button, Drawer, List, Space, Spin, Typography } from 'antd'
import { RightOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchDrillBreakdown } from '../../api/transaction'
import { FsDataTable } from '../FsDataTable'
import { EmptyState } from '../EmptyState'
import { MoneyText } from '../MoneyText'
import { rowAmount, rowTxnKind } from '../../utils/transactionAmount'
import { formatMoney } from '../../utils/format'
import { moneyTypeFromRow } from '../../utils/moneyType'
import type { DrillDownContext, DrillDownLayer } from './types'
import { drillBreadcrumbs, previousLayer } from './layerNav'
import { mergeDrillActions } from './buildDrillContext'
import {
  formatPartialDrillMessage,
  isDrillTruncated,
  mapBreakdownRows,
} from '../../utils/drillBreakdown'

type Props = {
  open: boolean
  context: DrillDownContext | null
  onClose: () => void
}

type BreakdownRow = {
  key: string
  label: string
  kind: 'category' | 'merchant'
  count: number
  total: number
}

export function UnifiedDrillDrawer({ open, context, onClose }: Props) {
  const sessionKey = useMemo(() => {
    if (!open || !context) return 'closed'
    return [
      context.title,
      context.params.transactionDateStartStr,
      context.params.transactionDateEndStr,
      context.params.consumeName,
      context.params.semanticFilter,
      context.params.merchantLabel,
      context.params.merchantToken,
    ].filter(Boolean).join('|')
  }, [open, context])

  if (!context) return null

  return (
    <UnifiedDrillDrawerInner
      key={sessionKey}
      open={open}
      context={context}
      onClose={onClose}
    />
  )
}

function UnifiedDrillDrawerInner({ open, context, onClose }: { open: boolean; context: DrillDownContext; onClose: () => void }) {
  const [layer, setLayer] = useState<DrillDownLayer>('insight')
  const contextMerchantToken = context.params.merchantToken || null
  const [categoryFilter, setCategoryFilter] = useState<string | null>(() => context.params.consumeName || null)
  const [merchantFilter, setMerchantFilter] = useState<string | null>(() => (
    contextMerchantToken ? contextMerchantToken : (context.params.merchantLabel || null)
  ))
  const [merchantTokenFilter, setMerchantTokenFilter] = useState<string | null>(() => contextMerchantToken)

  const queryParams = useMemo(() => {
    const next: Record<string, string> = { ...context.params }
    if (categoryFilter && !next.consumeName) next.consumeName = categoryFilter
    const token = merchantTokenFilter || next.merchantToken
    if (token) {
      next.merchantToken = token
      delete next.demoArea
    }
    return next
  }, [context.params, categoryFilter, merchantTokenFilter])

  const { data, isFetching } = useQuery({
    queryKey: ['unified-drill', queryParams],
    enabled: open && !!queryParams.transactionDateStartStr && layer !== 'insight' && layer !== 'actions',
    queryFn: () => fetchDrillBreakdown(queryParams),
  })

  const categories = useMemo(
    () => mapBreakdownRows(data?.categories, 'category') as BreakdownRow[],
    [data?.categories],
  )
  const merchants = useMemo(
    () => mapBreakdownRows(data?.merchants, 'merchant') as BreakdownRow[],
    [data?.merchants],
  )
  const transactionRows = data?.transactions || []
  const truncated = isDrillTruncated(data)
  const partialMessage = data ? formatPartialDrillMessage(data.total, data.sampleSize) : ''
  const provenance = context.provenance

  const actions = mergeDrillActions(context.actions)
  const showCategories = !context.params.consumeName && !categoryFilter

  const handleClose = () => {
    setLayer('insight')
    setCategoryFilter(null)
    setMerchantFilter(null)
    setMerchantTokenFilter(null)
    onClose()
  }

  const goBack = () => {
    const prev = previousLayer(layer, merchantFilter)
    if (!prev) return
    if (layer === 'transactions') {
      setMerchantFilter(null)
      setMerchantTokenFilter(null)
    }
    setLayer(prev)
  }

  const onCrumbClick = (crumbLayer: DrillDownLayer, merchant?: string | null) => {
    setLayer(crumbLayer)
    if (crumbLayer !== 'transactions') {
      setMerchantFilter(null)
      setMerchantTokenFilter(null)
    } else {
      setMerchantFilter(merchant || null)
    }
  }

  const provenanceBlock = provenance || data ? (
    <Typography.Paragraph type="secondary" className="fs-drill-provenance" style={{ marginBottom: 12 }}>
      {provenance?.reportId && <>Report: {provenance.reportId} · </>}
      {provenance?.sourceView && <>Source: {provenance.sourceView} · </>}
      {(provenance?.aggregateTotal != null || data?.aggregateTotal != null) && (
        <>Aggregate: {formatMoney(provenance?.aggregateTotal ?? data?.aggregateTotal ?? 0)} · </>
      )}
      {(provenance?.sampleCount != null || data?.sampleSize != null) && (
        <>Sample: {(provenance?.sampleCount ?? data?.sampleSize ?? 0).toLocaleString()} · </>
      )}
      {(provenance?.truncated ?? truncated) ? 'Truncated sample' : 'Full sample'}
      {provenance?.filterParams && Object.keys(provenance.filterParams).length > 0 && (
        <> · Filters: {Object.entries(provenance.filterParams)
          .filter(([, v]) => v)
          .map(([k, v]) => `${k}=${v}`)
          .join(', ')}</>
      )}
    </Typography.Paragraph>
  ) : null

  const partialAlert = truncated && partialMessage ? (
    <Alert
      type="warning"
      showIcon
      className="fs-drill-partial-alert"
      message="Partial transaction sample"
      description={partialMessage}
      style={{ marginBottom: 12 }}
    />
  ) : null

  const breakdownMeta = data && (
    <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
      {formatMoney(data.aggregateTotal)} total across {data.total.toLocaleString()} transaction{data.total === 1 ? '' : 's'}
      {truncated ? ` · sample list capped at ${data.sampleSize.toLocaleString()}` : ''}
    </Typography.Paragraph>
  )

  return (
    <Drawer
      title={context.title}
      width={780}
      open={open}
      onClose={handleClose}
      className="fs-unified-drill-drawer"
      extra={layer !== 'insight' && (
        <Button type="link" size="small" onClick={goBack}>Back</Button>
      )}
    >
      <Breadcrumb
        style={{ marginBottom: 12 }}
        items={drillBreadcrumbs(layer, merchantFilter).map((c) => ({
          title: c.title,
          onClick: c.layer !== layer || (c.merchant && c.merchant !== merchantFilter)
            ? () => onCrumbClick(c.layer, c.merchant)
            : undefined,
        }))}
      />

      {provenanceBlock}

      {layer === 'insight' && (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <div>
            <Typography.Text type="secondary">Metric</Typography.Text>
            <Typography.Title level={5} style={{ margin: '4px 0 0' }}>{context.metricLabel}</Typography.Title>
          </div>
          <List
            size="small"
            header={<Typography.Text strong>What this means</Typography.Text>}
            dataSource={context.explanation}
            renderItem={(line) => <List.Item style={{ paddingInline: 0 }}>{line}</List.Item>}
          />
          <Button type="primary" onClick={() => setLayer('breakdown')}>
            View breakdown
            <RightOutlined />
          </Button>
        </Space>
      )}

      {layer === 'breakdown' && (
        isFetching ? (
          <div className="fs-report-drill-loading"><Spin tip="Loading breakdown…" /></div>
        ) : showCategories ? (
          <>
            {partialAlert}
            {breakdownMeta}
            <FsDataTable
              columns={[
                { title: 'Category', dataIndex: 'label', sortType: 'text', ellipsis: true },
                { title: 'Txns', dataIndex: 'count', align: 'right', sortType: 'number', width: 72 },
                { title: 'Total', dataIndex: 'total', unit: 'CNY', align: 'right', sortType: 'number', render: (v) => formatMoney(Number(v)) },
              ]}
              dataSource={categories as unknown as Record<string, unknown>[]}
              rowKey="key"
              onRow={(record) => ({
                onClick: () => {
                  setCategoryFilter(String((record as BreakdownRow).label))
                },
                style: { cursor: 'pointer' },
              })}
              locale={{ emptyText: <EmptyState compact title="No categories" description="No rows match this slice." /> }}
            />
          </>
        ) : (
          <>
            {partialAlert}
            {breakdownMeta}
            {categoryFilter && (
              <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
                Category: <strong>{categoryFilter}</strong>
                {!context.params.consumeName && (
                  <Button type="link" size="small" onClick={() => setCategoryFilter(null)}>All categories</Button>
                )}
              </Typography.Paragraph>
            )}
            <FsDataTable
              columns={[
                { title: 'Merchant', dataIndex: 'label', sortType: 'text', ellipsis: true },
                { title: 'Txns', dataIndex: 'count', align: 'right', sortType: 'number', width: 72 },
                { title: 'Total', dataIndex: 'total', unit: 'CNY', align: 'right', sortType: 'number', render: (v) => formatMoney(Number(v)) },
              ]}
              dataSource={merchants as unknown as Record<string, unknown>[]}
              rowKey="key"
              onRow={(record) => ({
                onClick: () => {
                  const row = record as BreakdownRow
                  setMerchantFilter(row.label)
                  setMerchantTokenFilter(row.key)
                  setLayer('transactions')
                },
                style: { cursor: 'pointer' },
              })}
              locale={{ emptyText: <EmptyState compact title="No merchants" description="No rows match this slice." /> }}
            />
            <Button type="link" style={{ marginTop: 12, padding: 0 }} onClick={() => setLayer('actions')}>
              Suggested actions →
            </Button>
          </>
        )
      )}

      {layer === 'transactions' && (
        isFetching ? (
          <div className="fs-report-drill-loading"><Spin tip="Loading transactions…" /></div>
        ) : (
          <>
            {partialAlert}
            {merchantFilter && (
              <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
                Merchant: <strong>{merchantFilter}</strong>
              </Typography.Paragraph>
            )}
            <FsDataTable
              columns={[
                { title: 'Date', dataIndex: 'transactionDate', sortType: 'date', width: 100 },
                { title: 'Description', dataIndex: 'transactionDesc', ellipsis: true },
                { title: 'Category', dataIndex: 'consumeName', ellipsis: true, width: 120 },
                {
                  title: 'Amount',
                  dataIndex: 'balanceMoney',
                  unit: 'CNY',
                  align: 'right',
                  sortType: 'number',
                  render: (_, r) => (
                    <MoneyText
                      value={rowAmount(r as { incomeMoney?: number; balanceMoney?: number })}
                      type={moneyTypeFromRow(
                        rowTxnKind(r as { incomeMoney?: number; balanceMoney?: number }),
                        (r as { balanceMoney?: number }).balanceMoney,
                      )}
                    />
                  ),
                },
              ]}
              dataSource={transactionRows as unknown as Record<string, unknown>[]}
              rowKey="id"
              locale={{ emptyText: <EmptyState compact title="No transactions" description="No rows for this slice." /> }}
            />
            <Button type="link" style={{ marginTop: 12, padding: 0 }} onClick={() => setLayer('actions')}>
              Suggested actions →
            </Button>
          </>
        )
      )}

      {layer === 'actions' && (
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            Turn this insight into a next step — adjust plans, rules, or review underlying transactions.
          </Typography.Paragraph>
          {actions.map((action) => (
            <Link key={`${action.type}-${action.path}`} to={action.path} onClick={handleClose}>
              <Button block icon={<RightOutlined />}>{action.label}</Button>
            </Link>
          ))}
        </Space>
      )}
    </Drawer>
  )
}
