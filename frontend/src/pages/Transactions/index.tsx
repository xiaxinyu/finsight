import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { Button, Input, Modal, Popconfirm, Space, Tooltip, message } from 'antd'
import {
  DeleteOutlined, EditOutlined,
  SwapOutlined, ThunderboltOutlined, UnorderedListOutlined,
} from '@ant-design/icons'
import { createTransfer } from '../../api/finance'
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import {
  classifyTransactions, classifyUnclassifiedInFilter, parseReclassifyResult, deleteTransaction, expenseToIncome,
  fetchTransactionStats, incomeToExpense, listTransactions, updateTransaction,
  type ReclassifyPreviewRow, type ReclassifyResult, type TransactionQuery, type TransactionRow,
} from '../../api/transaction'
import { useConsumeTreeSelect } from '../../hooks/useConsumeTree'
import { useCardTree } from '../../hooks/useCardTree'
import { CardFilterSelect } from '../../components/filters/CardFilterSelect'
import { CategoryFilterSelect } from '../../components/filters/CategoryFilterSelect'
import { TransactionEditPanel, type TransactionEditDraft } from '../../components/TransactionEditPanel'
import { useFilterApply } from '../../hooks/useFilterApply'
import { useFillTableHeight } from '../../hooks/useFillTableHeight'
import { FilterToolbar } from '../../components/FilterToolbar'
import { TransactionKpiStrip } from '../../components/TransactionKpiStrip'
import { TransactionActiveFilters, type ActiveFilterChip } from '../../components/TransactionActiveFilters'
import { TransactionLedgerCell, TransactionTypeBadge } from '../../components/TransactionLedgerCell'
import { TransactionSelectionBar } from '../../components/TransactionSelectionBar'
import { ClassifyConfirmModal, type ClassifyEditRow } from '../../components/ClassifyConfirmModal'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { MoneyText } from '../../components/MoneyText'
import { moneyTypeFromRow } from '../../utils/moneyType'
import { TransactionCardCell } from '../../components/TransactionCardCell'
import { TableHeader } from '../../components/TableHeader'
import { formatDateMmDdYyyy } from '../../utils/format'
import { cellText, formatTableDate } from '../../utils/cell'
import { PeriodRangePicker } from '../../components/PeriodRangePicker'
import { periodFromStrings, periodToStrings } from '../../utils/periodStrings'
import { defaultPeriodStrings, formatPeriodPreview } from '../../utils/periodPresets'
import { rowAmount, rowTxnKind } from '../../utils/transactionAmount'
import { mapTransactionTableSort } from '../../utils/transactionTableSort'

type TxFilters = {
  start: string
  end: string
  card: string
  consume: string
  keyword: string
  unclassified: boolean
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

type ClassifyPending =
  | { mode: 'ids'; ids: string }
  | { mode: 'unclassified'; filters: TransactionQuery }

function enrichPreviewRows(preview: ReclassifyPreviewRow[], selected: TransactionRow[]): ReclassifyPreviewRow[] {
  const byId = new Map(selected.map((r) => [r.id, r]))
  return preview.map((p) => {
    const row = byId.get(p.id)
    return {
      ...p,
      transactionDesc: p.transactionDesc || row?.transactionDesc,
      transactionDate: p.transactionDate || row?.transactionDate,
    }
  })
}

function findCardTitle(nodes: { id: string; text: string; children?: typeof nodes }[], id: string): string {
  for (const n of nodes) {
    if (n.id === id) return n.text
    if (n.children) {
      const t = findCardTitle(n.children, id)
      if (t) return t
    }
  }
  return ''
}

export function TransactionsPage() {
  const actionRef = useRef<ActionType>(null)
  const tablePanelRef = useRef<HTMLDivElement>(null)
  const qc = useQueryClient()
  const [searchParams] = useSearchParams()
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([])
  const [selectedRows, setSelectedRows] = useState<TransactionRow[]>([])
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editDraft, setEditDraft] = useState<TransactionEditDraft | null>(null)
  const [editSaving, setEditSaving] = useState(false)
  const [tableLoading, setTableLoading] = useState(false)
  const [transferOpen, setTransferOpen] = useState(false)
  const [classifyPreviewOpen, setClassifyPreviewOpen] = useState(false)
  const [classifyPreview, setClassifyPreview] = useState<ReclassifyResult | null>(null)
  const [classifyBusy, setClassifyBusy] = useState(false)
  const tableHeight = useFillTableHeight(tablePanelRef)

  const unclassifiedFromUrl = searchParams.get('unclassified') === '1'
  const cardFromUrl = searchParams.get('cardId') || ''

  const initial: TxFilters = useMemo(() => ({
    ...defaultPeriodStrings(),
    card: cardFromUrl,
    consume: '',
    keyword: '',
    unclassified: unclassifiedFromUrl,
  }), [cardFromUrl, unclassifiedFromUrl])

  const { draft, setDraft, applied, applying, isDirty, applySync, patchBoth, resetBoth } = useFilterApply(initial)

  useEffect(() => {
    if (unclassifiedFromUrl) {
      setDraft((f) => ({ ...f, unclassified: true }))
    }
  }, [unclassifiedFromUrl, setDraft])

  const { treeData } = useConsumeTreeSelect()
  const { tree: cardTree } = useCardTree()

  const reload = useCallback(async () => {
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
  }, [applySync, qc])

  const applyFilterPatch = useCallback((patch: Partial<TxFilters>) => {
    patchBoth(patch)
    void reload()
  }, [patchBoth, reload])

  const appliedPeriod = useMemo(
    () => periodFromStrings(applied.start, applied.end),
    [applied.start, applied.end],
  )
  const periodLabel = useMemo(() => {
    if (!applied.start && !applied.end) return 'All time'
    return formatPeriodPreview(appliedPeriod[0], appliedPeriod[1])
  }, [applied.start, applied.end, appliedPeriod])

  const activeFilterChips: ActiveFilterChip[] = useMemo(() => {
    const chips: ActiveFilterChip[] = []
    if (applied.card) {
      const label = findCardTitle(cardTree, applied.card) || applied.card
      chips.push({
        key: 'card',
        label: `Card: ${label}`,
        onRemove: () => applyFilterPatch({ card: '' }),
      })
    }
    if (applied.consume) {
      const label = findTreeTitle(treeData, applied.consume) || applied.consume
      chips.push({
        key: 'consume',
        label: `Category: ${label}`,
        onRemove: () => applyFilterPatch({ consume: '' }),
      })
    }
    if (applied.keyword.trim()) {
      chips.push({
        key: 'keyword',
        label: `Keyword: ${applied.keyword.trim()}`,
        onRemove: () => applyFilterPatch({ keyword: '' }),
      })
    }
    if (applied.unclassified) {
      chips.push({
        key: 'unclassified',
        label: 'Unclassified only',
        onRemove: () => applyFilterPatch({ unclassified: false }),
      })
    }
    return chips
  }, [applied, cardTree, treeData, applyFilterPatch])

  const toggleUnclassifiedFilter = useCallback(() => {
    applyFilterPatch({ unclassified: !applied.unclassified })
  }, [applied.unclassified, applyFilterPatch])

  const clearAllFilters = useCallback(() => {
    resetBoth(initial)
    void reload()
  }, [initial, resetBoth, reload])

  const clearSelection = useCallback(() => {
    setSelectedRowKeys([])
    setSelectedRows([])
  }, [])

  const statsParams = useMemo(() => ({
    transactionDateStartStr: applied.start || undefined,
    transactionDateEndStr: applied.end || undefined,
    cardId: applied.card || undefined,
    consumeID: applied.consume || undefined,
    demoArea: applied.keyword || undefined,
    emptyConsume: applied.unclassified ? '1' : undefined,
  }), [applied])

  const { data: stats, isFetching: statsLoading } = useQuery({
    queryKey: ['tx-stats', statsParams],
    queryFn: () => fetchTransactionStats(statsParams),
    staleTime: 20_000,
  })

  const disabled = tableLoading || applying || editingId != null
  const statsBusy = statsLoading || applying

  const startEdit = useCallback((row: TransactionRow) => {
    setEditingId(row.id)
    setEditDraft({
      transactionDate: row.transactionDate,
      transactionDesc: cellText(row.transactionDesc),
      txnKind: row.txnKind || rowTxnKind(row),
      editAmount: rowAmount(row),
      consumeCode: row.consumeCode || row.consumeID || '',
      demoArea: cellText(row.demoArea),
    })
  }, [])

  const cancelEdit = useCallback(() => {
    setEditingId(null)
    setEditDraft(null)
  }, [])

  const onSaveRow = useCallback(async (
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
      cancelEdit()
      await reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Save failed')
      throw e
    }
  }, [treeData, reload, cancelEdit])

  const saveEdit = useCallback(async () => {
    if (!editingId || !editDraft) return
    setEditSaving(true)
    try {
      await onSaveRow(editingId, {
        id: editingId,
        ...editDraft,
        consumeID: editDraft.consumeCode,
      } as TransactionRow & { editAmount?: number })
    } catch {
      // onSaveRow shows error
    } finally {
      setEditSaving(false)
    }
  }, [editingId, editDraft, onSaveRow])

  const columns: ProColumns<TransactionRow>[] = useMemo(() => {
    const base: ProColumns<TransactionRow>[] = [
      {
        title: <TableHeader name="Date" />,
        dataIndex: 'transactionDate',
        width: 88,
        sorter: true,
        defaultSortOrder: 'descend',
        render: (_, r) => <span className="fs-tx-date">{formatTableDate(r.transactionDate)}</span>,
      },
      {
        title: <TableHeader name="Transaction" />,
        dataIndex: 'transactionDesc',
        className: 'fs-col-tx-desc',
        ellipsis: true,
        render: (_, r) => <TransactionLedgerCell row={r} />,
      },
      {
        title: <TableHeader name="Type" />,
        dataIndex: 'txnKind',
        width: 76,
        sorter: true,
        render: (_, r) => <TransactionTypeBadge kind={r.txnKind || rowTxnKind(r)} />,
      },
      {
        title: <TableHeader name="Amount" unit="CNY" />,
        dataIndex: 'editAmount',
        width: 108,
        align: 'right',
        sorter: true,
        render: (_, r) => (
          <MoneyText value={rowAmount(r)} type={moneyTypeFromRow(rowTxnKind(r), r.balanceMoney)} />
        ),
      },
      {
        title: <TableHeader name="Card" />,
        dataIndex: 'bankCode',
        width: 100,
        ellipsis: true,
        editable: false,
        sorter: true,
        render: (_, r) => <TransactionCardCell row={r} />,
      },
      {
        title: '',
        valueType: 'option',
        width: 64,
        fixed: 'right',
        className: 'fs-col-actions',
        render: (_, record) => (
          <div className="fs-inline-actions">
            <Tooltip title="Edit">
              <Button
                type="text"
                size="small"
                icon={<EditOutlined />}
                className="fs-row-action"
                disabled={editingId != null}
                onClick={() => startEdit(record)}
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
                  disabled={editingId != null} />
              </Tooltip>
            </Popconfirm>
          </div>
        ),
      },
    ]
    return base
  }, [editingId, startEdit])

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

  const openClassifyPreview = useCallback(async () => {
    let pending: ClassifyPending
    if (selectedRowKeys.length) {
      pending = { mode: 'ids', ids: selectedRowKeys.join(',') }
    } else if (applied.unclassified) {
      pending = { mode: 'unclassified', filters: statsParams }
    } else {
      message.warning('Select rows, or filter Unclassified and click again')
      return
    }
    setClassifyBusy(true)
    try {
      const raw = pending.mode === 'ids'
        ? await classifyTransactions(pending.ids, { persist: false })
        : await classifyUnclassifiedInFilter({ ...pending.filters, persist: false } as TransactionQuery & { persist?: boolean })
      const r = parseReclassifyResult(raw)
      if (!r) {
        message.error('Could not preview classification')
        return
      }
      setClassifyPreview(r)
      setClassifyPreviewOpen(true)
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Preview failed')
    } finally {
      setClassifyBusy(false)
    }
  }, [selectedRowKeys, applied.unclassified, statsParams])

  const confirmClassify = useCallback(async (editRows: ClassifyEditRow[]) => {
    if (!editRows.length) return
    setClassifyBusy(true)
    try {
      await Promise.all(editRows.map((r) => updateTransaction({
        id: r.id,
        consumeCode: r.categoryCode,
        consumeID: r.categoryCode,
        consumeName: findTreeTitle(treeData, r.categoryCode) || r.categoryName,
      })))
      message.success(`Applied ${editRows.length} categor${editRows.length === 1 ? 'y' : 'ies'}`)
      setClassifyPreviewOpen(false)
      setClassifyPreview(null)
      setSelectedRowKeys([])
      await reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Apply failed')
    } finally {
      setClassifyBusy(false)
    }
  }, [treeData, reload])

  const previewRows = useMemo(
    () => enrichPreviewRows(classifyPreview?.preview ?? [], selectedRows),
    [classifyPreview, selectedRows],
  )

  const batchActions = (
    <Space size="small" wrap>
      <Button size="small" disabled={disabled || selectedRowKeys.length !== 2} icon={<SwapOutlined />} onClick={() => setTransferOpen(true)}>Mark transfer</Button>
      <Button size="small" danger disabled={disabled} icon={<DeleteOutlined />} onClick={() => runBatch(() => Promise.all(selectedRowKeys.map(deleteTransaction)), 'Deleted')}>Delete</Button>
      <Button size="small" disabled={disabled || classifyBusy} loading={classifyBusy} icon={<ThunderboltOutlined />} onClick={() => void openClassifyPreview()}>Auto-classify</Button>
      <Button size="small" disabled={disabled} icon={<SwapOutlined />} onClick={() => runBatch(() => incomeToExpense(selectedRowKeys.join(',')), 'Moved to expense')}>→ Expense</Button>
      <Button size="small" disabled={disabled} icon={<SwapOutlined />} onClick={() => runBatch(() => expenseToIncome(selectedRowKeys.join(',')), 'Moved to income')}>→ Income</Button>
    </Space>
  )

  return (
    <DataPageLayout
      title="Transactions"
      subtitle={periodLabel}
      icon={<UnorderedListOutlined />}
      className={`fs-data-page--dense fs-data-page--fill fs-data-page--transactions${editingId ? ' fs-data-page--editing' : ''}`}
      toolbar={(
        <FilterToolbar
          loading={tableLoading || applying}
          onApply={reload}
          dirty={isDirty}
        >
          <PeriodRangePicker
            size="small"
            disabled={disabled}
            value={periodFromStrings(draft.start, draft.end)}
            onChange={(range, presetId) => {
              const { start, end } = periodToStrings(range, presetId)
              setDraft((f) => ({ ...f, start, end }))
            }}
          />
          <CardFilterSelect
            disabled={disabled}
            value={draft.card}
            onChange={(v) => setDraft((f) => ({ ...f, card: v }))}
          />
          <CategoryFilterSelect
            disabled={disabled}
            value={draft.consume}
            onChange={(v) => setDraft((f) => ({ ...f, consume: v }))}
          />
          <Input.Search
            className="fs-filter-control fs-filter-control--keyword"
            size="small"
            placeholder="Search description or memo"
            allowClear
            disabled={disabled}
            value={draft.keyword}
            onChange={(e) => setDraft((f) => ({ ...f, keyword: e.target.value }))}
            onSearch={(v) => { setDraft((f) => ({ ...f, keyword: v })); reload() }}
          />
        </FilterToolbar>
      )}
    >
      <TransactionKpiStrip
        total={stats?.total}
        income={stats?.income}
        expense={stats?.expense}
        net={stats?.net}
        unclassified={stats?.unclassified}
        transfers={stats?.transfers}
        truncated={stats?.truncated}
        loading={statsBusy}
        unclassifiedActive={applied.unclassified}
        onUnclassifiedClick={toggleUnclassifiedFilter}
      />
      <TransactionActiveFilters chips={activeFilterChips} onClearAll={clearAllFilters} />
      <div
        ref={tablePanelRef}
        className={`fs-table-panel fs-table-panel--editable fs-table-panel--transactions fs-table-panel--tx-overlay${selectedRowKeys.length > 0 ? ' fs-table-panel--has-selection' : ''}`}
      >
        <ProTable<TransactionRow>
          className="fs-data-table fs-data-table--transactions fs-data-table--compact"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          scroll={{ x: 'max-content', y: tableHeight }}
          search={false}
          loading={tableLoading}
          options={{ density: true, reload: () => reload(), setting: true, fullScreen: false }}
          tableAlertRender={false}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys, rows) => {
              setSelectedRowKeys(keys as string[])
              setSelectedRows(rows as TransactionRow[])
            },
          }}
          rowClassName={(record) => (record.id === editingId ? 'fs-row-editing' : 'fs-table-row')}
          onRow={(record) => ({
            onDoubleClick: () => {
              if (editingId != null) return
              startEdit(record)
            },
          })}
          expandable={{
            expandedRowKeys: editingId ? [editingId] : [],
            showExpandColumn: false,
            expandIcon: () => null,
            expandedRowRender: (record) => {
              if (record.id !== editingId || !editDraft) return null
              const cardSummary = [cellText(record.bankCode), cellText(record.cardTypeName), cellText(record.bankCardName)]
                .filter(Boolean)
                .join(' · ')
              return (
                <TransactionEditPanel
                  draft={editDraft}
                  onChange={setEditDraft}
                  treeData={treeData}
                  cardSummary={cardSummary}
                  saving={editSaving}
                  onSave={() => void saveEdit()}
                  onCancel={cancelEdit}
                />
              )
            },
          }}
          locale={{
            emptyText: <EmptyState compact title="No transactions" description="Try widening the date range or clearing filters." />,
          }}
          request={async (params, sort) => {
            const filters = applied
            const { sortField, sortOrder } = mapTransactionTableSort(sort)
            try {
              const res = await listTransactions({
                page: params.current || 1,
                rows: params.pageSize || 20,
                transactionDateStartStr: filters.start || undefined,
                transactionDateEndStr: filters.end || undefined,
                cardId: filters.card || undefined,
                consumeID: filters.consume,
                demoArea: filters.keyword,
                emptyConsume: filters.unclassified ? '1' : undefined,
                sortField,
                sortOrder,
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
          pagination={{
            defaultPageSize: 50,
            pageSizeOptions: [20, 50, 100, 200],
            showSizeChanger: true,
            size: 'small',
            showTotal: (t) => `${t.toLocaleString()} txns`,
          }}
        />
        <TransactionSelectionBar count={selectedRowKeys.length} disabled={disabled} onClear={clearSelection}>
          {batchActions}
        </TransactionSelectionBar>
      </div>
      <ClassifyConfirmModal
        open={classifyPreviewOpen}
        busy={classifyBusy}
        preview={classifyPreview}
        rows={previewRows}
        treeData={treeData}
        onCancel={() => {
          if (classifyBusy) return
          setClassifyPreviewOpen(false)
          setClassifyPreview(null)
        }}
        onConfirm={confirmClassify}
      />
      <Modal title="Mark as transfer" open={transferOpen} onOk={markTransfer} onCancel={() => setTransferOpen(false)}>
        Pair two transactions as an internal transfer (excluded from income/expense reports).
        {selectedRows.length === 2 && (
          <div style={{ marginTop: 12, fontSize: 12, display: 'grid', gap: 8 }}>
            {selectedRows.map((r, i) => (
              <div key={r.id}>
                <strong>{i === 0 ? 'From' : 'To'}:</strong>{' '}
                {formatTableDate(r.transactionDate)} · {cellText(r.transactionDesc)} ·{' '}
                <MoneyText value={rowAmount(r)} type={moneyTypeFromRow(rowTxnKind(r), r.balanceMoney)} />
              </div>
            ))}
          </div>
        )}
      </Modal>
    </DataPageLayout>
  )
}
