import { useMemo, useState } from 'react'
import { Breadcrumb, Drawer, Spin } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { listTransactions, type TransactionRow } from '../api/transaction'
import { FsDataTable } from './FsDataTable'
import { EmptyState } from './EmptyState'
import { MoneyText, moneyTypeFromRow } from './MoneyText'
import { rowAmount, rowTxnKind } from '../utils/transactionAmount'
import { formatMoney } from '../utils/format'

type Props = {
  open: boolean
  params: Record<string, string>
  title?: string
  onClose: () => void
}

type MerchantRow = {
  key: string
  merchant: string
  count: number
  total: number
}

function merchantLabel(row: TransactionRow): string {
  const opp = (row as { opponentName?: string }).opponentName
  if (opp && opp.trim()) return opp.trim()
  return (row.transactionDesc || 'Unknown').trim()
}

export function ReportDrillDrawer({ open, params, title = 'Transaction drill-down', onClose }: Props) {
  const [merchantFilter, setMerchantFilter] = useState<string | null>(null)

  const { data, isFetching } = useQuery({
    queryKey: ['report-drill', params],
    enabled: open && !!params.transactionDateStartStr,
    queryFn: () => listTransactions({ ...params, page: 1, rows: 200 }),
  })

  const merchants = useMemo(() => {
    const map = new Map<string, MerchantRow>()
    for (const row of data?.rows || []) {
      const label = merchantLabel(row)
      const key = label.toLowerCase()
      const amt = rowAmount(row)
      const existing = map.get(key)
      if (existing) {
        existing.count += 1
        existing.total += amt
      } else {
        map.set(key, { key, merchant: label, count: 1, total: amt })
      }
    }
    return Array.from(map.values()).sort((a, b) => b.total - a.total)
  }, [data?.rows])

  const filteredRows = useMemo(() => {
    if (!merchantFilter) return data?.rows || []
    return (data?.rows || []).filter((r) => merchantLabel(r).toLowerCase() === merchantFilter.toLowerCase())
  }, [data?.rows, merchantFilter])

  const categoryCrumb = params.consumeName || params.consumeID || 'All categories'

  return (
    <Drawer
      title={title}
      width={760}
      open={open}
      onClose={() => {
        setMerchantFilter(null)
        onClose()
      }}
      className="fs-report-drill-drawer"
    >
      <Breadcrumb
        style={{ marginBottom: 12 }}
        items={[
          { title: categoryCrumb },
          { title: merchantFilter || 'Merchants', onClick: merchantFilter ? () => setMerchantFilter(null) : undefined },
          ...(merchantFilter ? [{ title: 'Transactions' }] : []),
        ]}
      />
      {isFetching ? (
        <div className="fs-report-drill-loading"><Spin tip="Loading transactions…" /></div>
      ) : !merchantFilter ? (
        <FsDataTable
          columns={[
            { title: 'Merchant', dataIndex: 'merchant', sortType: 'text', ellipsis: true },
            { title: 'Txns', dataIndex: 'count', align: 'right', sortType: 'number', width: 72 },
            {
              title: 'Total',
              dataIndex: 'total',
              unit: 'CNY',
              align: 'right',
              sortType: 'number',
              render: (v) => formatMoney(Number(v)),
            },
          ]}
          dataSource={merchants as unknown as Record<string, unknown>[]}
          rowKey="key"
          onRow={(record) => ({
            onClick: () => setMerchantFilter(String((record as MerchantRow).merchant)),
            style: { cursor: 'pointer' },
          })}
          locale={{ emptyText: <EmptyState compact title="No merchants" description="No rows match this slice." /> }}
        />
      ) : (
        <FsDataTable
          columns={[
            { title: 'Date', dataIndex: 'transactionDate', sortType: 'date', width: 100 },
            { title: 'Description', dataIndex: 'transactionDesc', ellipsis: true },
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
          locale={{ emptyText: <EmptyState compact title="No transactions" description="No rows for this merchant." /> }}
        />
      )}
    </Drawer>
  )
}
