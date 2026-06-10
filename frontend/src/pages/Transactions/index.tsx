import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { Button, Input, Modal, Popconfirm, Select, Space, Tag, Tooltip, TreeSelect, message } from 'antd'
import {
  CheckOutlined, CloseOutlined, DeleteOutlined, EditOutlined,
  SwapOutlined, ThunderboltOutlined, UnorderedListOutlined,
} from '@ant-design/icons'
import { createTransfer } from '../../api/finance'
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import {
  classifyTransactions, deleteTransaction, expenseToIncome,
  fetchTransactionStats, incomeToExpense, listCards, listTransactions, updateTransaction, type TransactionRow,
} from '../../api/transaction'
import { useConsumeTreeSelect } from '../../hooks/useConsumeTree'
import { useFilterApply } from '../../hooks/useFilterApply'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { FilterToolbar } from '../../components/FilterToolbar'
import { TransactionSummaryBar } from '../../components/TransactionSummaryBar'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { MoneyText, moneyTypeFromRow } from '../../components/MoneyText'
import { TableHeader } from '../../components/TableHeader'
import { formatDateMmDdYyyy } from '../../utils/format'
import { cellText, formatTableDate } from '../../utils/cell'
import { PeriodRangePicker, periodFromStrings, periodToStrings } from '../../components/PeriodRangePicker'
import { defaultPeriodRange } from '../../utils/periodPresets'

type TxFilters = {
  start: string
  end: string
  card: string
  consume: string
  keyword: string
  unclassified: boolean
}

function rowAmount(r: TransactionRow): number {
  if (r.incomeMoney && Math.abs(r.incomeMoney) > 0) return Math.abs(r.incomeMoney)
  return Math.abs(Number(r.balanceMoney || 0))
}

function rowTxnKind(r: TransactionRow): string {
  if (r.txnKind) return r.txnKind
  if (r.incomeMoney && r.incomeMoney > 0) return 'income'
  if (r.balanceMoney != null && r.balanceMoney < 0) return 'income'
  return 'expense'
}

function findTreeTitle(nodes: { title: string; value: string; children?: typeof nodes }[], value: string): string {
  for (const n of nodes) {
    if (n.value === value) return n.title
    if (n.children) {
      const t = findTreeTitle(n.children, value)
      if (t) return t
    }
  }
  return ''
}

export function TransactionsPage() {
  const actionRef = useRef<ActionType>(null)
  const qc = useQueryClient()
  const [searchParams] = useSearchParams()
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([])
  const [editableKeys, setEditableKeys] = useState<React.Key[]>([])
  const [tableLoading, setTableLoading] = useState(false)
  const [transferOpen, setTransferOpen] = useState(false)
  const tableHeight = useViewportTableHeight(210)

  const unclassifiedFromUrl = searchParams.get('unclassified') === '1'

  const initial: TxFilters = {
    start: periodToStrings(defaultPeriodRange()).start,
    end: periodToStrings(defaultPeriodRange()).end,
    card: '',
    consume: '',
    keyword: '',
    unclassified: unclassifiedFromUrl,
  }

  const { draft, setDraft, applied, applying, isDirty, applySync } = useFilterApply(initial)

  useEffect(() => {
    if (unclassifiedFromUrl) {
      setDraft((f) => ({ ...f, unclassified: true }))
    }
  }, [unclassifiedFromUrl, setDraft])

  const { data: cards } = useQuery({ queryKey: ['cards'], queryFn: listCards })
  const { treeData } = useConsumeTreeSelect()

  const statsParams = useMemo(() => ({
    transactionDateStartStr: applied.start,
    transactionDateEndStr: applied.end,
    cardTypeName: applied.card || undefined,
    consumeID: applied.consume || undefined,
    demoArea: applied.keyword || undefined,
    emptyConsume: applied.unclassified ? '1' : undefined,
  }), [applied])

  const { data: stats, isFetching: statsLoading } = useQuery({
    queryKey: ['tx-stats', statsParams],
    queryFn: () => fetchTransactionStats(statsParams),
    staleTime: 20_000,
  })

  const disabled = tableLoading || applying
  const statsBusy = statsLoading || applying

  const onSaveRow = async (
    _key: unknown,
    row: TransactionRow & { editAmount?: number },
  ) => {
    const kind = row.txnKind || rowTxnKind(row)
    const payload: Partial<TransactionRow> & Record<string, unknown> = {
      id: row.id,
      demoArea: row.demoArea,
      transactionDesc: row.transactionDesc,
      txnKind: kind,
      consumeCode: row.consumeCode,
      consumeID: row.consumeCode || row.consumeID,
    }
    if (row.consumeCode) {
      payload.consumeName = findTreeTitle(treeData, row.consumeCode) || row.consumeName
    }
    if (row.transactionDate) {
      const d = dayjs(row.transactionDate)
      if (d.isValid()) {
        payload.transactionDate = formatDateMmDdYyyy(d) as unknown as string
      }
    }
    const amt = row.editAmount != null ? Math.abs(Number(row.editAmount)) : rowAmount(row)
    if (kind === 'income') {
      payload.incomeMoney = amt
      payload.balanceMoney = 0
    } else {
      payload.balanceMoney = amt
      payload.incomeMoney = 0
    }
    try {
      await updateTransaction(payload)
      message.success('Saved')
      setEditableKeys([])
      await reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Save failed')
      throw e
    }
  }

  const columns: ProColumns<TransactionRow>[] = useMemo(() => [
    {
      title: <TableHeader name="Date" />,
      dataIndex: 'transactionDate',
      width: 120,
      sorter: true,
      editable: () => true,
      valueType: 'date',
      fieldProps: { size: 'small', format: 'MM/DD/YYYY' },
      render: (_, r) => <span className="fs-mono">{formatTableDate(r.transactionDate)}</span>,
    },
    {
      title: <TableHeader name="Type" />,
      dataIndex: 'txnKind',
      width: 100,
      editable: () => true,
      valueType: 'select',
      valueEnum: {
        expense: { text: 'Expense' },
        income: { text: 'Income' },
        transfer: { text: 'Transfer' },
      },
      render: (_, r) => {
        const k = rowTxnKind(r)
        if (k === 'transfer') return <Tag className="fs-tag" color="orange">Transfer</Tag>
        return <Tag className="fs-tag" color={k === 'income' ? 'green' : 'default'}>{k}</Tag>
      },
    },
    {
      title: <TableHeader name="Description" />,
      dataIndex: 'transactionDesc',
      ellipsis: true,
      editable: () => true,
      fieldProps: { size: 'small' },
      render: (_, r) => <span className="fs-cell-text" title={cellText(r.transactionDesc)}>{cellText(r.transactionDesc)}</span>,
    },
    {
      title: <TableHeader name="Amount" unit="CNY" />,
      dataIndex: 'editAmount',
      width: 120,
      align: 'right',
      sorter: true,
      editable: () => true,
      valueType: 'digit',
      fieldProps: { size: 'small', min: 0, precision: 2, style: { width: '100%' } },
      render: (_, r) => (
        <MoneyText value={rowAmount(r)} type={moneyTypeFromRow(rowTxnKind(r), r.balanceMoney)} />
      ),
    },
    {
      title: <TableHeader name="Card" />,
      dataIndex: 'cardTypeName',
      width: 100,
      ellipsis: true,
      editable: false,
      render: (_, r) => <span className="fs-cell-muted" title={cellText(r.cardTypeName)}>{cellText(r.cardTypeName)}</span>,
    },
    {
      title: <TableHeader name="Category" />,
      dataIndex: 'consumeCode',
      width: 160,
      ellipsis: true,
      editable: () => true,
      valueType: 'treeSelect',
      fieldProps: { treeData, treeDefaultExpandAll: true, allowClear: true, size: 'small', style: { width: '100%' } },
      render: (_, r) => <span className="fs-cell-text" title={cellText(r.consumeName)}>{cellText(r.consumeName)}</span>,
    },
    {
      title: <TableHeader name="Memo" />,
      dataIndex: 'demoArea',
      width: 120,
      ellipsis: true,
      editable: () => true,
      fieldProps: { size: 'small' },
      render: (_, r) => <span className="fs-cell-muted" title={cellText(r.demoArea)}>{cellText(r.demoArea)}</span>,
    },
    {
      title: '',
      valueType: 'option',
      width: 72,
      fixed: 'right',
      className: 'fs-col-actions',
      render: (_, record, __, action) => (
        <div className="fs-inline-actions">
          <Tooltip title="Edit">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              className="fs-row-action"
              disabled={editableKeys.length > 0}
              onClick={() => {
                setEditableKeys([record.id])
                action?.startEditable?.(record.id)
              }}
            />
          </Tooltip>
          <Popconfirm
            title="Delete this transaction?"
            onConfirm={async () => {
              await deleteTransaction(record.id)
              message.success('Deleted')
              await reload()
            }}
          >
            <Tooltip title="Delete">
              <Button type="text" size="small" danger icon={<DeleteOutlined />} className="fs-row-action"
                disabled={editableKeys.length > 0} />
            </Tooltip>
          </Popconfirm>
        </div>
      ),
    },
  ], [treeData, editableKeys])

  const reload = async () => {
    setTableLoading(true)
    applySync()
    try {
      await actionRef.current?.reload?.()
      qc.invalidateQueries({ queryKey: ['financial-pulse'] })
      qc.invalidateQueries({ queryKey: ['decision-cards'] })
      qc.invalidateQueries({ queryKey: ['wealth'] })
      qc.invalidateQueries({ queryKey: ['cashflow'] })
      qc.invalidateQueries({ queryKey: ['tx-stats'] })
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

  const markTransfer = async () => {
    if (selectedRowKeys.length !== 2) {
      message.warning('Select exactly 2 rows for a transfer pair')
      return
    }
    try {
      await createTransfer(selectedRowKeys[0], selectedRowKeys[1], 'Marked as transfer')
      message.success('Marked as transfer')
      setSelectedRowKeys([])
      setTransferOpen(false)
      await reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Failed')
    }
  }

  const batchActions = (
    <Space size="small" wrap>
      <Button size="small" disabled={disabled || selectedRowKeys.length !== 2} icon={<SwapOutlined />} onClick={() => setTransferOpen(true)}>Mark transfer</Button>
      <Button size="small" danger disabled={disabled} icon={<DeleteOutlined />} onClick={() => runBatch(() => Promise.all(selectedRowKeys.map(deleteTransaction)), 'Deleted')}>Delete</Button>
      <Button size="small" disabled={disabled} icon={<ThunderboltOutlined />} onClick={() => runBatch(() => classifyTransactions(selectedRowKeys.join(',')), 'Classified')}>Auto-classify</Button>
      <Button size="small" disabled={disabled} icon={<SwapOutlined />} onClick={() => runBatch(() => incomeToExpense(selectedRowKeys.join(',')), 'Moved to expense')}>→ Expense</Button>
      <Button size="small" disabled={disabled} icon={<SwapOutlined />} onClick={() => runBatch(() => expenseToIncome(selectedRowKeys.join(',')), 'Moved to income')}>→ Income</Button>
    </Space>
  )

  return (
    <DataPageLayout
      title="Transactions"
      icon={<UnorderedListOutlined />}
      className="fs-data-page--dense"
      toolbar={(
        <FilterToolbar
          loading={tableLoading || applying}
          onApply={reload}
          dirty={isDirty}
          selectedCount={selectedRowKeys.length}
          actions={batchActions}
          summary={(
            <TransactionSummaryBar
              density="toolbar"
              total={stats?.total}
              income={stats?.income}
              expense={stats?.expense}
              net={stats?.net}
              unclassified={stats?.unclassified}
              transfers={stats?.transfers}
              truncated={stats?.truncated}
              loading={statsBusy}
            />
          )}
        >
          <PeriodRangePicker
            size="small"
            disabled={disabled}
            value={periodFromStrings(draft.start, draft.end)}
            onChange={(range) => {
              const { start, end } = periodToStrings(range)
              setDraft((f) => ({ ...f, start, end }))
            }}
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
          <Button
            size="small"
            type={draft.unclassified ? 'primary' : 'default'}
            onClick={() => setDraft((f) => ({ ...f, unclassified: !f.unclassified }))}
          >
            Unclassified
          </Button>
        </FilterToolbar>
      )}
    >
      {editableKeys.length > 0 && (
        <div className="fs-edit-banner">
          <span>Editing row — press Save or Cancel when done</span>
          <Space size={8}>
            <Button
              type="primary"
              size="small"
              icon={<CheckOutlined />}
              onClick={async () => {
                const key = editableKeys[0]
                if (key != null) await actionRef.current?.saveEditable?.(key)
              }}
            >
              Save
            </Button>
            <Button
              size="small"
              icon={<CloseOutlined />}
              onClick={async () => {
                const key = editableKeys[0]
                if (key != null) await actionRef.current?.cancelEditable?.(key)
              }}
            >
              Cancel
            </Button>
          </Space>
        </div>
      )}
      <div className="fs-table-panel fs-table-panel--editable">
        <ProTable<TransactionRow>
          className="fs-data-table fs-data-table--no-title"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          scroll={{ x: 'max-content', y: tableHeight }}
          search={false}
          loading={tableLoading}
          options={{ density: true, reload: true, setting: true }}
          rowSelection={{ selectedRowKeys, onChange: (keys) => setSelectedRowKeys(keys as string[]) }}
          rowClassName={(record) => (editableKeys.includes(record.id) ? 'fs-row-editing' : 'fs-table-row')}
          onRow={(record) => ({
            onDoubleClick: () => {
              if (editableKeys.includes(record.id)) return
              setEditableKeys([record.id])
              actionRef.current?.startEditable?.(record.id)
            },
          })}
          locale={{
            emptyText: <EmptyState compact title="No transactions" description="Try widening the date range or clearing filters." />,
          }}
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
                emptyConsume: applied.unclassified ? '1' : undefined,
              })
              const rows = res.rows.map((r) => ({
                ...r,
                txnKind: rowTxnKind(r),
                editAmount: rowAmount(r),
                consumeCode: r.consumeCode || r.consumeID,
              }))
              return { data: rows, total: res.total, success: true }
            } catch (e) {
              message.error(e instanceof Error ? e.message : 'Failed to load transactions')
              return { data: [], total: 0, success: false }
            }
          }}
          columns={columns}
          editable={{
            type: 'single',
            editableKeys,
            onChange: setEditableKeys,
            onSave: onSaveRow,
            saveText: <CheckOutlined />,
            cancelText: <CloseOutlined />,
            actionRender: (_row, _config, { save, cancel }) => [
              <div key="edit-actions" className="fs-inline-actions fs-inline-actions--edit">
                <Tooltip title="Save">
                  <span className="fs-editable-save">{save}</span>
                </Tooltip>
                <Tooltip title="Cancel">
                  <span className="fs-editable-cancel">{cancel}</span>
                </Tooltip>
              </div>,
            ],
          }}
          pagination={{ defaultPageSize: 20, showSizeChanger: true, size: 'small', showTotal: (t) => `${t} rows` }}
        />
      </div>
      <Modal title="Mark as transfer" open={transferOpen} onOk={markTransfer} onCancel={() => setTransferOpen(false)}>
        Pair two transactions as an internal transfer (excluded from income/expense reports).
        <div style={{ marginTop: 8, fontSize: 12 }}>From: {selectedRowKeys[0]} → To: {selectedRowKeys[1]}</div>
      </Modal>
    </DataPageLayout>
  )
}
