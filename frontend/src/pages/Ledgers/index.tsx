import { useRef, useState } from 'react'
import { Alert } from 'antd'
import { useParams } from 'react-router-dom'
import { BookOutlined } from '@ant-design/icons'
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import { ledgerConfigs } from '../../config/ledgers'
import { listLedger } from '../../api/ledger'
import { type TransactionRow } from '../../api/transaction'
import { CategoryFilterSelect } from '../../components/filters/CategoryFilterSelect'
import { useFilterApply } from '../../hooks/useFilterApply'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { FilterToolbar } from '../../components/FilterToolbar'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { MoneyText } from '../../components/MoneyText'
import { moneyTypeFromRow } from '../../utils/moneyType'
import { TableHeader } from '../../components/TableHeader'
import { cellText, formatTableDate } from '../../utils/cell'
import { rowAmount, rowTxnKind } from '../../utils/transactionAmount'
import { PeriodRangePicker } from '../../components/PeriodRangePicker'
import { periodFromStrings, periodToStrings } from '../../utils/periodStrings'
import { defaultPeriodStrings } from '../../utils/periodPresets'

type LedgerFilters = { start: string; end: string; consume: string }

export function LedgersPage() {
  const { ledgerId = '' } = useParams()
  const cfg = ledgerConfigs[ledgerId]
  const actionRef = useRef<ActionType>(null)
  const [tableLoading, setTableLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const tableHeight = useViewportTableHeight(200)

  const initial: LedgerFilters = {
    ...defaultPeriodStrings(),
    consume: '',
  }

  const { draft, setDraft, applied, applying, isDirty, applySync } = useFilterApply(initial)

  if (!cfg) return <DataPageLayout title="Ledger not found"><EmptyState title="Ledger not found" description="This ledger type does not exist." /></DataPageLayout>

  const disabled = tableLoading || applying

  const columns: ProColumns<TransactionRow>[] = [
    {
      title: <TableHeader name="Date" />,
      dataIndex: 'transactionDate',
      width: 100,
      sorter: true,
      render: (_, r) => <span className="fs-mono">{formatTableDate(r.transactionDate)}</span>,
    },
    {
      title: <TableHeader name="Description" />,
      dataIndex: 'transactionDesc',
      ellipsis: true,
      render: (_, r) => <span title={cellText(r.transactionDesc)}>{cellText(r.transactionDesc)}</span>,
    },
    {
      title: <TableHeader name="Amount" unit="CNY" />,
      dataIndex: 'balanceMoney',
      width: 110,
      align: 'right',
      sorter: true,
      render: (_, r) => (
        <MoneyText value={rowAmount(r)} type={moneyTypeFromRow(cfg.txnType ?? rowTxnKind(r), r.balanceMoney)} />
      ),
    },
    {
      title: <TableHeader name="Category" />,
      dataIndex: 'consumeName',
      width: 120,
      ellipsis: true,
      render: (_, r) => <span title={cellText(r.consumeName)}>{cellText(r.consumeName)}</span>,
    },
    {
      title: <TableHeader name="Card" />,
      dataIndex: 'cardTypeName',
      width: 90,
      ellipsis: true,
      render: (_, r) => <span title={cellText(r.cardTypeName)}>{cellText(r.cardTypeName)}</span>,
    },
  ]

  const reload = async () => {
    setTableLoading(true)
    applySync()
    try {
      await actionRef.current?.reload?.()
    } finally {
      setTableLoading(false)
    }
  }

  return (
    <DataPageLayout
      title={cfg.title}
      subtitle="Ledger entries for the selected period"
      icon={<BookOutlined />}
      toolbar={(
        <FilterToolbar loading={tableLoading || applying} onApply={reload} dirty={isDirty}>
          <PeriodRangePicker
            size="small"
            disabled={disabled}
            value={periodFromStrings(draft.start, draft.end)}
            onChange={(range) => setDraft((r) => ({ ...r, ...periodToStrings(range) }))}
          />
          {cfg.txnType && (
            <CategoryFilterSelect
              disabled={disabled}
              txnType={cfg.txnType}
              value={draft.consume}
              onChange={(v) => setDraft((r) => ({ ...r, consume: v }))}
            />
          )}
        </FilterToolbar>
      )}
    >
      {loadError && (
        <Alert type="error" showIcon style={{ marginBottom: 8 }} message="Failed to load ledger" description={loadError} />
      )}
      <div className="fs-table-panel">
        <ProTable<TransactionRow>
          className="fs-data-table"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          loading={tableLoading}
          search={false}
          scroll={{ x: 'max-content', y: tableHeight }}
          locale={{
            emptyText: <EmptyState compact title="No ledger entries" description="Adjust the date range or category filter." />,
          }}
          request={async (params) => {
            try {
              setLoadError(null)
              const res = await listLedger(cfg.listEndpoint, {
                page: params.current || 1,
                rows: params.pageSize || 20,
                transactionDateStartStr: applied.start,
                transactionDateEndStr: applied.end,
                consumeID: applied.consume,
                txnTypes: cfg.txnType,
              })
              return { data: res.rows, total: res.total, success: true }
            } catch (e) {
              const msg = e instanceof Error ? e.message : 'Request failed'
              setLoadError(msg)
              return { data: [], total: 0, success: false }
            }
          }}
          columns={columns}
          pagination={{ defaultPageSize: 20, size: 'small', showTotal: (t) => `${t} rows` }}
        />
      </div>
    </DataPageLayout>
  )
}
