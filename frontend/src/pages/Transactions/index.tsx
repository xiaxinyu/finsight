import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { Button, Input, Modal, Select, Space, message, Segmented } from 'antd'
import {
  DeleteOutlined,
  SwapOutlined, ThunderboltOutlined, UnorderedListOutlined,
} from '@ant-design/icons'
import { createTransfer } from '../../api/finance'
import { ProTable, type ActionType } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import {
  deleteTransaction, expenseToIncome,
  fetchTransactionStats, incomeToExpense, listTransactions, updateTransaction,
  type ReclassifyResult, type TransactionRow,
} from '../../api/transaction'
import {
  applyReclassification,
  previewReclassificationByIds,
  previewReclassificationUnclassified,
} from '../../api/classification'
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
import { TransactionSelectionBar } from '../../components/TransactionSelectionBar'
import { ClassifyConfirmModal, type ClassifyEditRow } from '../../components/ClassifyConfirmModal'
import { buildClassifyPreviewRows } from '../../utils/classifyPreview'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { MoneyText } from '../../components/MoneyText'
import { moneyTypeFromRow } from '../../utils/moneyType'
import { formatDateMmDdYyyy } from '../../utils/format'
import { cellText, formatTableDate } from '../../utils/cell'
import { PeriodRangePicker } from '../../components/PeriodRangePicker'
import { periodFromStrings, periodToStrings } from '../../utils/periodStrings'
import { defaultPeriodStrings, formatPeriodPreview } from '../../utils/periodPresets'
import { rowAmount, rowTxnKind } from '../../utils/transactionAmount'
import { mapTransactionTableSort } from '../../utils/transactionTableSort'
import { summarizeSelection } from '../../utils/transactionSelection'
import {
  buildReportingClassificationFilterOptions,
  reportingClassificationFilterLabel,
  reportingClassificationFilterSelectOptions,
} from '../../utils/reportTaxonomy'
import { useSemanticsCatalog } from '../../hooks/useSemanticsCatalog'
import {
  type ClassifyPending,
  TX_FILTER_PRESETS,
  defaultTxFilters,
  findCardTitle,
  findTreeTitle,
  txFiltersDiffer,
  type TxFilters,
} from './transactionFilters'
import { useTransactionColumns } from './useTransactionColumns'

export function TransactionsPage() {
  const actionRef = useRef<ActionType>(null)
  const tablePanelRef = useRef<HTMLDivElement>(null)
  const qc = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
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
  const [pageMaxAmount, setPageMaxAmount] = useState(0)
  const tableHeight = useFillTableHeight(tablePanelRef)

  const unclassifiedFromUrl = searchParams.get('unclassified') === '1'
  const cardFromUrl = searchParams.get('cardId') || ''
  const consumeFromUrl = searchParams.get('consume') || ''
  const semanticFromUrl = searchParams.get('semantic') || ''
  const semanticUnclassifiedFromUrl = semanticFromUrl === 'unclassified'

  const initial: TxFilters = useMemo(() => ({
    ...defaultPeriodStrings(),
    card: cardFromUrl,
    consume: consumeFromUrl,
    keyword: '',
    unclassified: unclassifiedFromUrl || semanticUnclassifiedFromUrl,
    semanticFilter: semanticUnclassifiedFromUrl ? '' : semanticFromUrl,
  }), [cardFromUrl, consumeFromUrl, unclassifiedFromUrl, semanticUnclassifiedFromUrl, semanticFromUrl])

  const { draft, setDraft, applied, applying, isDirty, applySync, patchBoth, resetBoth } = useFilterApply(initial)

  const { treeData } = useConsumeTreeSelect()
  const { tree: cardTree } = useCardTree()
  const { data: semanticsCatalog } = useSemanticsCatalog()
  const classificationFilterOptions = useMemo(
    () => buildReportingClassificationFilterOptions(semanticsCatalog),
    [semanticsCatalog],
  )
  const classificationSelectOptions = useMemo(
    () => reportingClassificationFilterSelectOptions(classificationFilterOptions),
    [classificationFilterOptions],
  )

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

  const syncUnclassifiedUrl = useCallback((active: boolean) => {
    setSearchParams((prev) => {
      const params = new URLSearchParams(prev)
      if (active) {
        params.set('unclassified', '1')
        params.delete('semantic')
      } else {
        params.delete('unclassified')
      }
      return params
    }, { replace: true })
  }, [setSearchParams])

  useEffect(() => {
    const wantUnclassified = unclassifiedFromUrl || semanticUnclassifiedFromUrl
    if (!wantUnclassified || applied.unclassified) return
    patchBoth({ unclassified: true, semanticFilter: '' })
    void reload()
  }, [unclassifiedFromUrl, semanticUnclassifiedFromUrl, applied.unclassified, patchBoth, reload])

  useEffect(() => {
    if (consumeFromUrl) {
      patchBoth({ consume: consumeFromUrl })
    }
  }, [consumeFromUrl, patchBoth])

  const appliedPeriod = useMemo(
    () => periodFromStrings(applied.start, applied.end),
    [applied.start, applied.end],
  )
  const periodLabel = useMemo(() => {
    if (!applied.start && !applied.end) return 'All time'
    return formatPeriodPreview(appliedPeriod[0], appliedPeriod[1])
  }, [applied.start, applied.end, appliedPeriod])

  const defaultFilters = useMemo(() => defaultTxFilters(), [])

  const hasActiveFilters = useMemo(
    () => txFiltersDiffer(applied, defaultFilters),
    [applied, defaultFilters],
  )

  const activeFilterChips: ActiveFilterChip[] = useMemo(() => {
    const chips: ActiveFilterChip[] = []
    const defPeriod = defaultPeriodStrings()
    if (applied.start !== defPeriod.start || applied.end !== defPeriod.end) {
      chips.push({
        key: 'period',
        label: periodLabel,
        onRemove: () => applyFilterPatch({ ...defaultPeriodStrings() }),
      })
    }
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
        onRemove: () => {
          applyFilterPatch({ unclassified: false })
          syncUnclassifiedUrl(false)
        },
      })
    }
    if (applied.semanticFilter && applied.semanticFilter !== 'unclassified') {
      chips.push({
        key: 'semantic',
        label: reportingClassificationFilterLabel(applied.semanticFilter, semanticsCatalog),
        onRemove: () => applyFilterPatch({ semanticFilter: '' }),
      })
    }
    return chips
  }, [applied, cardTree, treeData, periodLabel, applyFilterPatch, syncUnclassifiedUrl, semanticsCatalog])

  const toggleUnclassifiedFilter = useCallback(() => {
    const next = !applied.unclassified
    applyFilterPatch({ unclassified: next })
    syncUnclassifiedUrl(next)
  }, [applied.unclassified, applyFilterPatch, syncUnclassifiedUrl])

  const clearAllFilters = useCallback(() => {
    resetBoth(defaultFilters)
    setSearchParams({}, { replace: true })
    void reload()
  }, [defaultFilters, resetBoth, setSearchParams, reload])

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
    semanticFilter: applied.semanticFilter && applied.semanticFilter !== 'unclassified'
      ? applied.semanticFilter
      : undefined,
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

  const selectionSummary = useMemo(() => summarizeSelection(selectedRows), [selectedRows])

  const columns = useTransactionColumns({
    editingId,
    pageMaxAmount,
    startEdit,
    reload,
  })

  const activePreset = useMemo(() => {
    for (const preset of TX_FILTER_PRESETS) {
      const merged = { ...defaultTxFilters(), ...preset.patch }
      if (!txFiltersDiffer(applied, merged)) return preset.id
    }
    return ''
  }, [applied])

  const applyPreset = useCallback((presetId: string) => {
    const preset = TX_FILTER_PRESETS.find((p) => p.id === presetId)
    if (!preset) return
    const next = { ...defaultTxFilters(), ...preset.patch }
    patchBoth(next)
    if (presetId === 'unclassified') {
      syncUnclassifiedUrl(true)
    } else {
      syncUnclassifiedUrl(false)
    }
    void reload()
  }, [patchBoth, reload, syncUnclassifiedUrl])

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
      const r = pending.mode === 'ids'
        ? await previewReclassificationByIds(pending.ids, false)
        : await previewReclassificationUnclassified(pending.filters)
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
      const applied = await applyReclassification(
        editRows.map((r) => ({
          transactionId: r.id,
          categoryCode: r.categoryCode,
          categoryName: r.categoryName,
        })),
        'Transactions auto-classify confirm',
      )
      const count = applied.result?.classified ?? 0
      if (count === 0) {
        message.error('No categories were saved — check selection and try again')
        return
      }
      const batchHint = applied.batchId ? ` (batch ${applied.batchId})` : ''
      message.success(`Applied ${count} categor${count === 1 ? 'y' : 'ies'}${batchHint}`)
      setClassifyPreviewOpen(false)
      setClassifyPreview(null)
      setSelectedRowKeys([])
      await reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Apply failed')
    } finally {
      setClassifyBusy(false)
    }
  }, [reload])

  const previewRows = useMemo(
    () => buildClassifyPreviewRows(classifyPreview?.preview ?? [], selectedRows),
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
        <div className="fs-tx-filter-shell">
          <FilterToolbar
            loading={tableLoading || applying}
            onApply={reload}
            dirty={isDirty}
            canReset={hasActiveFilters}
            onReset={clearAllFilters}
            resetLabel="Reset"
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
            <Select
              className="fs-filter-control fs-filter-control--semantic"
              size="small"
              disabled={disabled}
              value={draft.semanticFilter || undefined}
              placeholder="Reporting Classification"
              allowClear
              showSearch
              optionFilterProp="label"
              options={classificationSelectOptions}
              onChange={(v) => setDraft((f) => ({ ...f, semanticFilter: v || '' }))}
            />
            <Input.Search
              className="fs-filter-control fs-filter-control--keyword"
              size="small"
              placeholder="Search…"
              allowClear
              disabled={disabled}
              value={draft.keyword}
              onChange={(e) => setDraft((f) => ({ ...f, keyword: e.target.value }))}
              onSearch={(v) => { setDraft((f) => ({ ...f, keyword: v })); reload() }}
            />
          </FilterToolbar>
          <div className="fs-tx-stats-row">
            <TransactionKpiStrip
              variant="compact"
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
          </div>
          <TransactionActiveFilters
            chips={activeFilterChips}
            onClearAll={clearAllFilters}
            showClearAll={hasActiveFilters}
          />
          <Segmented
            size="small"
            className="fs-tx-filter-presets"
            value={activePreset || undefined}
            options={TX_FILTER_PRESETS.map((p) => ({ label: p.label, value: p.id }))}
            onChange={(v) => applyPreset(String(v))}
          />
        </div>
      )}
    >
      <div
        ref={tablePanelRef}
        className={`fs-table-panel fs-table-panel--editable fs-table-panel--transactions fs-table-panel--tx-overlay${selectedRowKeys.length > 0 ? ' fs-table-panel--has-selection' : ''}`}
      >
        <ProTable<TransactionRow>
          className="fs-data-table fs-data-table--transactions fs-data-table--compact"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          scroll={{ x: 1040, y: tableHeight }}
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
              const semanticHint = record.semanticsSummary || undefined
              return (
                <TransactionEditPanel
                  draft={editDraft}
                  onChange={setEditDraft}
                  treeData={treeData}
                  cardSummary={cardSummary}
                  semanticHint={semanticHint}
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
                semanticFilter: filters.semanticFilter && filters.semanticFilter !== 'unclassified'
                  ? filters.semanticFilter
                  : undefined,
                sortField,
                sortOrder,
              })
              const rows = res.rows.map((r) => ({
                ...r,
                txnKind: rowTxnKind(r),
                editAmount: rowAmount(r),
                consumeCode: r.consumeCode || r.consumeID,
              }))
              const maxAmt = rows.reduce((m, r) => Math.max(m, rowAmount(r)), 0)
              setPageMaxAmount(maxAmt)
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
        <TransactionSelectionBar
          count={selectedRowKeys.length}
          summary={selectionSummary}
          disabled={disabled}
          onClear={clearSelection}
        >
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
