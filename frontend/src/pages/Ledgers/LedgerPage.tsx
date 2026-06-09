import { useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { DatePicker, TreeSelect } from 'antd'
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import { ledgerConfigs } from '../../config/ledgers'
import { listLedger } from '../../api/ledger'
import { type TransactionRow } from '../../api/transaction'
import { useConsumeTreeSelect } from '../../hooks/useConsumeTree'
import { useFilterApply } from '../../hooks/useFilterApply'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { FilterToolbar } from '../../components/FilterToolbar'
import { DataPageLayout } from '../../components/DataPageLayout'
import { TableHeader } from '../../components/TableHeader'
import { formatDateMmDdYyyy, formatNumber } from '../../utils/format'
import { cellText, formatTableDate } from '../../utils/cell'
import { dateRangePresets } from '../../utils/datePresets'

const { RangePicker } = DatePicker

type LedgerFilters = { start: string; end: string; consume: string }

export function LedgerPage() {
  const { ledgerId = '' } = useParams()
  const cfg = ledgerConfigs[ledgerId]
  const actionRef = useRef<ActionType>(null)
  const [tableLoading, setTableLoading] = useState(false)
  const tableHeight = useViewportTableHeight(200)

  const initial: LedgerFilters = {
    start: formatDateMmDdYyyy(dayjs().startOf('year')),
    end: formatDateMmDdYyyy(dayjs()),
    consume: '',
  }

  const { draft, setDraft, applied, applying, applySync } = useFilterApply(initial)
  const { treeData } = useConsumeTreeSelect(cfg?.txnType)

  if (!cfg) return <DataPageLayout title="Ledger not found"><div /></DataPageLayout>

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
      render: (_, r) => <span className="fs-money">{formatNumber(r.balanceMoney)}</span>,
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
      toolbar={(
        <FilterToolbar loading={tableLoading || applying} onApply={reload}>
          <RangePicker
            size="small"
            disabled={disabled}
            value={[dayjs(draft.start, 'MM/DD/YYYY'), dayjs(draft.end, 'MM/DD/YYYY')]}
            presets={dateRangePresets}
            onChange={(v) => v && setDraft((r) => ({ ...r, start: formatDateMmDdYyyy(v[0]!), end: formatDateMmDdYyyy(v[1]!) }))}
          />
          {cfg.txnType && (
            <TreeSelect size="small" allowClear placeholder="Category" disabled={disabled} style={{ width: 160 }} treeData={treeData}
              value={draft.consume || undefined}
              onChange={(v) => setDraft((r) => ({ ...r, consume: v || '' }))} />
          )}
        </FilterToolbar>
      )}
    >
      <div className="fs-table-panel">
        <ProTable<TransactionRow>
          className="fs-data-table"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          loading={tableLoading}
          search={false}
          scroll={{ x: 'max-content', y: tableHeight }}
          request={async (params) => {
            const res = await listLedger(cfg.listEndpoint, {
              page: params.current || 1,
              rows: params.pageSize || 20,
              transactionDateStartStr: applied.start,
              transactionDateEndStr: applied.end,
              consumeID: applied.consume,
              txnTypes: cfg.txnType,
            })
            return { data: res.rows, total: res.total, success: true }
          }}
          columns={columns}
          pagination={{ defaultPageSize: 20, size: 'small' }}
        />
      </div>
    </DataPageLayout>
  )
}
