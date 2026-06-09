import { useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { DatePicker, TreeSelect } from 'antd'
import { PageContainer, ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import { ledgerConfigs } from '../../config/ledgers'
import { listLedger } from '../../api/ledger'
import { type TransactionRow } from '../../api/transaction'
import { useConsumeTreeSelect } from '../../hooks/useConsumeTree'
import { useFilterApply } from '../../hooks/useFilterApply'
import { FilterToolbar } from '../../components/FilterToolbar'
import { ContentCard } from '../../components/ContentCard'
import { TableHeader } from '../../components/TableHeader'
import { formatDateMmDdYyyy, formatNumber } from '../../utils/format'
import { dateRangePresets } from '../../utils/datePresets'

const { RangePicker } = DatePicker

type LedgerFilters = { start: string; end: string; consume: string }

export function LedgerPage() {
  const { ledgerId = '' } = useParams()
  const cfg = ledgerConfigs[ledgerId]
  const actionRef = useRef<ActionType>(null)
  const [tableLoading, setTableLoading] = useState(false)

  const initial: LedgerFilters = {
    start: formatDateMmDdYyyy(dayjs().startOf('year')),
    end: formatDateMmDdYyyy(dayjs()),
    consume: '',
  }

  const { draft, setDraft, applied, applying, applySync } = useFilterApply(initial)
  const { treeData } = useConsumeTreeSelect(cfg?.txnType)

  if (!cfg) return <PageContainer title="Ledger not found" />

  const disabled = tableLoading || applying

  const columns: ProColumns<TransactionRow>[] = [
    { title: <TableHeader name="Date" />, dataIndex: 'transactionDate', width: 110, sorter: true },
    {
      title: <TableHeader name="Description" />,
      dataIndex: 'transactionDesc',
      ellipsis: true,
      render: (_, r) => <span title={r.transactionDesc}>{r.transactionDesc}</span>,
    },
    {
      title: <TableHeader name="Amount" unit="CNY" />,
      dataIndex: 'balanceMoney',
      align: 'right',
      sorter: true,
      render: (_, r) => <span className="fs-money">{formatNumber(r.balanceMoney)}</span>,
    },
    { title: <TableHeader name="Category" />, dataIndex: 'consumeName', width: 140, ellipsis: true, render: (v) => <span title={String(v ?? '')}>{String(v ?? '')}</span> },
    { title: <TableHeader name="Card" />, dataIndex: 'cardTypeName', width: 100, ellipsis: true, render: (v) => <span title={String(v ?? '')}>{String(v ?? '')}</span> },
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
    <PageContainer title={cfg.title}>
      <FilterToolbar loading={tableLoading || applying} onApply={reload}>
        <RangePicker
          disabled={disabled}
          value={[dayjs(draft.start, 'MM/DD/YYYY'), dayjs(draft.end, 'MM/DD/YYYY')]}
          presets={dateRangePresets}
          onChange={(v) => v && setDraft((r) => ({ ...r, start: formatDateMmDdYyyy(v[0]!), end: formatDateMmDdYyyy(v[1]!) }))}
        />
        {cfg.txnType && (
          <TreeSelect allowClear placeholder="Category" disabled={disabled} style={{ width: 200 }} treeData={treeData}
            value={draft.consume || undefined}
            onChange={(v) => setDraft((r) => ({ ...r, consume: v || '' }))} />
        )}
      </FilterToolbar>

      <ContentCard className="fs-table-card">
        <ProTable<TransactionRow>
          className="fs-data-table"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          loading={tableLoading}
          search={false}
          scroll={{ y: 480 }}
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
          pagination={{ defaultPageSize: 20 }}
        />
      </ContentCard>
    </PageContainer>
  )
}
