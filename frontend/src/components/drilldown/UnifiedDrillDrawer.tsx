import { useMemo, useState } from 'react'
import { Breadcrumb, Button, Drawer, List, Space, Spin, Typography } from 'antd'
import { RightOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { listTransactions, type TransactionRow } from '../../api/transaction'
import { FsDataTable } from '../FsDataTable'
import { EmptyState } from '../EmptyState'
import { MoneyText } from '../MoneyText'
import { rowAmount, rowTxnKind } from '../../utils/transactionAmount'
import { formatMoney } from '../../utils/format'
import { moneyTypeFromRow } from '../../utils/moneyType'
import type { DrillDownContext, DrillDownLayer } from './types'
import { drillBreadcrumbs, previousLayer } from './layerNav'
import { mergeDrillActions } from './buildDrillContext'
import { rowMatchesMerchantToken } from '../../utils/merchantNormalize'

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

function merchantLabel(row: TransactionRow): string {
  const opp = (row as { opponentName?: string }).opponentName
  if (opp && opp.trim()) return opp.trim()
  return (row.transactionDesc || 'Unknown').trim()
}

function categoryLabel(row: TransactionRow): string {
  return (row.consumeName || 'Uncategorized').trim()
}

export function UnifiedDrillDrawer({ open, context, onClose }: Props) {
  const sessionKey = useMemo(() => {
    if (!open || !context) return 'closed'
    return [
      context.title,
      context.params.transactionDateStartStr,
      context.params.transactionDateEndStr,
      context.params.consumeName,
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
  const merchantToken = context.params.merchantToken || null
  const [categoryFilter, setCategoryFilter] = useState<string | null>(() => context.params.consumeName || null)
  const [merchantFilter, setMerchantFilter] = useState<string | null>(() => (
    merchantToken ? merchantToken : (context.params.merchantLabel || null)
  ))

  const queryParams = useMemo(() => {
    if (!context) return {}
    const next = { ...context.params }
    if (categoryFilter && !next.consumeName) next.consumeName = categoryFilter
    if (next.merchantToken) {
      delete next.demoArea
    }
    return next
  }, [context, categoryFilter])

  const { data, isFetching } = useQuery({
    queryKey: ['unified-drill', queryParams],
    enabled: open && !!context && !!queryParams.transactionDateStartStr && layer !== 'insight' && layer !== 'actions',
    queryFn: () => listTransactions({ ...queryParams, page: 1, rows: 200 }),
  })

  const categories = useMemo(() => {
    const map = new Map<string, BreakdownRow>()
    for (const row of data?.rows || []) {
      const label = categoryLabel(row)
      const key = label.toLowerCase()
      const amt = rowAmount(row)
      const existing = map.get(key)
      if (existing) {
        existing.count += 1
        existing.total += amt
      } else {
        map.set(key, { key, label, kind: 'category', count: 1, total: amt })
      }
    }
    return Array.from(map.values()).sort((a, b) => b.total - a.total)
  }, [data?.rows])

  const merchants = useMemo(() => {
    const map = new Map<string, BreakdownRow>()
    for (const row of data?.rows || []) {
      const label = merchantLabel(row)
      const key = label.toLowerCase()
      const amt = rowAmount(row)
      const existing = map.get(key)
      if (existing) {
        existing.count += 1
        existing.total += amt
      } else {
        map.set(key, { key, label, kind: 'merchant', count: 1, total: amt })
      }
    }
    return Array.from(map.values()).sort((a, b) => b.total - a.total)
  }, [data?.rows])

  const filteredRows = useMemo(() => {
    if (!merchantFilter) return data?.rows || []
    if (merchantToken) {
      return (data?.rows || []).filter((r) => rowMatchesMerchantToken(
        (r as { opponentName?: string }).opponentName,
        r.transactionDesc,
        merchantToken,
      ))
    }
    return (data?.rows || []).filter((r) => merchantLabel(r).toLowerCase() === merchantFilter.toLowerCase())
  }, [data?.rows, merchantFilter, merchantToken])

  const actions = mergeDrillActions(context.actions)
  const showCategories = !context.params.consumeName && !categoryFilter

  const handleClose = () => {
    setLayer('insight')
    setCategoryFilter(null)
    setMerchantFilter(null)
    onClose()
  }

  const goBack = () => {
    const prev = previousLayer(layer, merchantFilter)
    if (!prev) return
    if (layer === 'transactions') setMerchantFilter(null)
    setLayer(prev)
  }

  const onCrumbClick = (crumbLayer: DrillDownLayer, merchant?: string | null) => {
    setLayer(crumbLayer)
    if (crumbLayer !== 'transactions') setMerchantFilter(null)
    else setMerchantFilter(merchant || null)
  }

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
        ) : (
          <>
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
                  setMerchantFilter(String((record as BreakdownRow).label))
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
              dataSource={filteredRows as unknown as Record<string, unknown>[]}
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
