import { useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Button, DatePicker, Input, Select, Space, TreeSelect, message } from 'antd'
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import {
  classifyTransactions, deleteTransaction, expenseToIncome,
  incomeToExpense, listCards, listTransactions, updateTransaction, type TransactionRow,
} from '../../api/transaction'
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

type TxFilters = {
  start: string
  end: string
  card: string
  consume: string
  keyword: string
}

export function TransactionsPage() {
  const actionRef = useRef<ActionType>(null)
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([])
  const [tableLoading, setTableLoading] = useState(false)
  const tableHeight = useViewportTableHeight(200)

  const initial: TxFilters = {
    start: formatDateMmDdYyyy(dayjs().startOf('year')),
    end: formatDateMmDdYyyy(dayjs()),
    card: '',
    consume: '',
    keyword: '',
  }

  const { draft, setDraft, applied, applying, applySync } = useFilterApply(initial)

  const { data: cards } = useQuery({ queryKey: ['cards'], queryFn: listCards })
  const { treeData } = useConsumeTreeSelect()

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
      title: <TableHeader name="Card" />,
      dataIndex: 'cardTypeName',
      width: 90,
      ellipsis: true,
      render: (_, r) => <span title={cellText(r.cardTypeName)}>{cellText(r.cardTypeName)}</span>,
    },
    {
      title: <TableHeader name="Category" />,
      dataIndex: 'consumeName',
      width: 120,
      ellipsis: true,
      render: (_, r) => <span title={cellText(r.consumeName)}>{cellText(r.consumeName)}</span>,
    },
    {
      title: <TableHeader name="Memo" />,
      dataIndex: 'demoArea',
      width: 100,
      ellipsis: true,
      render: (_, r) => <span title={cellText(r.demoArea)}>{cellText(r.demoArea)}</span>,
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

  const runBatch = async (fn: () => Promise<unknown>, okMsg: string) => {
    if (!selectedRowKeys.length) { message.warning('Select rows first'); return }
    try {
      await fn()
      message.success(okMsg)
      setSelectedRowKeys([])
      await reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Failed')
    }
  }

  const batchActions = (
    <Space size="small" wrap>
      <Button size="small" danger disabled={disabled} onClick={() => runBatch(() => Promise.all(selectedRowKeys.map(deleteTransaction)), 'Deleted')}>Delete</Button>
      <Button size="small" disabled={disabled} onClick={() => runBatch(() => classifyTransactions(selectedRowKeys.join(',')), 'Classified')}>Auto-classify</Button>
      <Button size="small" disabled={disabled} onClick={() => runBatch(() => incomeToExpense(selectedRowKeys.join(',')), 'Moved to expense')}>→ Expense</Button>
      <Button size="small" disabled={disabled} onClick={() => runBatch(() => expenseToIncome(selectedRowKeys.join(',')), 'Moved to income')}>→ Income</Button>
    </Space>
  )

  return (
    <DataPageLayout
      title="Transactions"
      toolbar={(
        <FilterToolbar loading={tableLoading || applying} onApply={reload} actions={batchActions}>
          <RangePicker
            size="small"
            disabled={disabled}
            value={[dayjs(draft.start, 'MM/DD/YYYY'), dayjs(draft.end, 'MM/DD/YYYY')]}
            presets={dateRangePresets}
            onChange={(v) => v && setDraft((f) => ({ ...f, start: formatDateMmDdYyyy(v[0]!), end: formatDateMmDdYyyy(v[1]!) }))}
          />
          <Select size="small" allowClear placeholder="Card" disabled={disabled} style={{ width: 120 }}
            options={(cards || []).map((c) => ({ value: c.key, label: c.value }))}
            value={draft.card || undefined}
            onChange={(v) => setDraft((f) => ({ ...f, card: v || '' }))} />
          <TreeSelect size="small" allowClear placeholder="Category" disabled={disabled} style={{ width: 160 }} treeData={treeData} treeDefaultExpandAll
            value={draft.consume || undefined}
            onChange={(v) => setDraft((f) => ({ ...f, consume: v || '' }))} />
          <Input.Search size="small" placeholder="Keyword" allowClear disabled={disabled} style={{ width: 140 }}
            value={draft.keyword}
            onChange={(e) => setDraft((f) => ({ ...f, keyword: e.target.value }))}
            onSearch={(v) => { setDraft((f) => ({ ...f, keyword: v })); reload() }} />
        </FilterToolbar>
      )}
    >
      <div className="fs-table-panel">
        <ProTable<TransactionRow>
          className="fs-data-table"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          scroll={{ x: 'max-content', y: tableHeight }}
          search={false}
          loading={tableLoading}
          options={{ density: true, reload: true, setting: true }}
          rowSelection={{ selectedRowKeys, onChange: (keys) => setSelectedRowKeys(keys as string[]) }}
          request={async (params) => {
            try {
              const res = await listTransactions({
                page: params.current || 1,
                rows: params.pageSize || 20,
                transactionDateStartStr: applied.start,
                transactionDateEndStr: applied.end,
                cardTypeName: applied.card,
                consumeID: applied.consume,
                demoArea: applied.keyword,
              })
              return { data: res.rows, total: res.total, success: true }
            } catch (e) {
              message.error(e instanceof Error ? e.message : 'Failed to load transactions')
              return { data: [], total: 0, success: false }
            }
          }}
          columns={columns}
          editable={{
            type: 'single',
            onSave: async (_, row) => { await updateTransaction(row); message.success('Saved'); reload() },
            editableKeys: ['demoArea'],
          }}
          pagination={{ defaultPageSize: 20, showSizeChanger: true, size: 'small' }}
        />
      </div>
    </DataPageLayout>
  )
}
