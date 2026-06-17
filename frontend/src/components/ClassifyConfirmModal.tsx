import { useCallback, useMemo, useState } from 'react'
import { ArrowRightOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { Alert, Button, Modal, Space, Table, Tag, Tooltip, Typography } from 'antd'
import { CategoryPicker } from './CategoryPicker'
import type { TreeSelectNode } from '../hooks/useConsumeTree'
import type { ReclassifyPreviewRow, ReclassifyResult } from '../api/transaction'
import { isBulkManualMode } from '../utils/classifyPreview'
import { cellText, formatTableDate } from '../utils/cell'

export type ClassifyEditRow = ReclassifyPreviewRow & {
  enabled: boolean
  userEdited: boolean
  categoryCode: string
  categoryName: string
}

type Props = {
  open: boolean
  busy: boolean
  preview: ReclassifyResult | null
  rows: ReclassifyPreviewRow[]
  treeData: TreeSelectNode[]
  onCancel: () => void
  onConfirm: (rows: ClassifyEditRow[]) => Promise<void>
}

function findTreeTitle(nodes: TreeSelectNode[], value: string): string {
  for (const n of nodes) {
    if (n.value === value) return n.title
    if (n.children) {
      const t = findTreeTitle(n.children, value)
      if (t) return t
    }
  }
  return ''
}

function sourceMeta(source?: ReclassifyPreviewRow['source'], action?: string) {
  if (action === 'MANUAL') return { label: 'Manual', color: 'default' as const }
  if (action === 'SUGGEST' || (source && source !== 'RULE')) {
    if (source === 'SIMILAR') return { label: 'Similar', color: 'geekblue' as const }
    if (source === 'HEURISTIC') return { label: 'Heuristic', color: 'cyan' as const }
    if (source === 'WEAK_RULE') return { label: 'Weak match', color: 'purple' as const }
    if (source === 'KEYWORDS') return { label: 'Keywords', color: 'gold' as const }
    return { label: 'Suggested', color: 'blue' as const }
  }
  if (source === 'RULE') return { label: 'Rule', color: 'default' as const }
  return null
}

function formatConfidence(value?: number): string {
  const n = Number(value)
  if (!n || Number.isNaN(n)) return '—'
  return `${Math.round(n * 100)}%`
}

function beforeLabel(row: ClassifyEditRow): string {
  const name = row.beforeCategoryName || row.beforeCategoryCode
  return name ? String(name) : 'Unclassified'
}

function toEditRows(rows: ReclassifyPreviewRow[]): ClassifyEditRow[] {
  return rows.map((p) => ({
    ...p,
    enabled: true,
    userEdited: false,
    categoryCode: p.categoryCode || '',
    categoryName: p.categoryName || '',
  }))
}

export function ClassifyConfirmModal({
  open,
  busy,
  preview,
  rows: previewRows,
  treeData,
  onCancel,
  onConfirm,
}: Props) {
  const sessionKey = useMemo(
    () => (open ? `open:${previewRows.map((r) => r.id).join(',')}` : 'closed'),
    [open, previewRows],
  )
  return (
    <ClassifyConfirmModalInner
      key={sessionKey}
      open={open}
      busy={busy}
      preview={preview}
      rows={previewRows}
      treeData={treeData}
      onCancel={onCancel}
      onConfirm={onConfirm}
    />
  )
}

function ClassifyConfirmModalInner({
  open,
  busy,
  preview,
  rows: previewRows,
  treeData,
  onCancel,
  onConfirm,
}: Props) {
  const originalCodes = useMemo(() => {
    const orig: Record<string, string> = {}
    for (const r of previewRows) orig[r.id] = r.categoryCode || ''
    return orig
  }, [previewRows])

  const bulkMode = useMemo(() => isBulkManualMode(previewRows, preview), [previewRows, preview])

  const [rows, setRows] = useState<ClassifyEditRow[]>(() => toEditRows(previewRows))
  const [bulkCategory, setBulkCategory] = useState<string | undefined>()
  const [showRowPickers, setShowRowPickers] = useState(false)

  const checkedCount = useMemo(() => rows.filter((r) => r.enabled).length, [rows])
  const applyCount = useMemo(
    () => rows.filter((r) => r.enabled && r.categoryCode).length,
    [rows],
  )
  const bulkCategoryName = bulkCategory ? findTreeTitle(treeData, bulkCategory) : ''

  const syncBulkCategory = useCallback((code: string | undefined) => {
    setBulkCategory(code)
    if (!code) {
      setRows((prev) => prev.map((r) => ({
        ...r,
        categoryCode: '',
        categoryName: '',
        userEdited: false,
      })))
      return
    }
    const name = findTreeTitle(treeData, code)
    setRows((prev) => prev.map((r) => {
      if (!r.enabled) return r
      return {
        ...r,
        categoryCode: code,
        categoryName: name || r.categoryName,
        userEdited: code !== (originalCodes[r.id] ?? ''),
      }
    }))
  }, [originalCodes, treeData])

  const updateCategory = useCallback((id: string, code: string | undefined) => {
    const value = code || ''
    const name = value ? findTreeTitle(treeData, value) : ''
    setRows((prev) => prev.map((r) => {
      if (r.id !== id) return r
      return {
        ...r,
        categoryCode: value,
        categoryName: name || r.categoryName,
        enabled: true,
        userEdited: value !== (originalCodes[id] ?? ''),
      }
    }))
  }, [originalCodes, treeData])

  const selectedKeys = useMemo(() => rows.filter((r) => r.enabled).map((r) => r.id), [rows])

  const selectAll = () => setRows((prev) => prev.map((r) => ({ ...r, enabled: true })))
  const selectNone = () => setRows((prev) => prev.map((r) => ({ ...r, enabled: false })))

  const readyRows = useMemo(
    () => rows.filter((r) => r.enabled && r.categoryCode),
    [rows],
  )

  const footerLabel = bulkMode
    ? `Apply to ${readyRows.length} transaction${readyRows.length === 1 ? '' : 's'}`
    : `Apply ${applyCount} categor${applyCount === 1 ? 'y' : 'ies'}`

  const useRowPickers = !bulkMode || showRowPickers

  return (
    <Modal
      className={`fs-classify-modal${bulkMode ? ' fs-classify-modal--bulk' : ''}`}
      title={(
        <div className="fs-classify-modal__title">
          <span className="fs-classify-modal__icon" aria-hidden>
            <ThunderboltOutlined />
          </span>
          <div>
            <div className="fs-classify-modal__heading">
              {bulkMode ? 'Assign category' : 'Review auto-classify'}
            </div>
            <div className="fs-classify-modal__sub">
              {bulkMode
                ? `No rule matches — pick one category for ${checkedCount} selected transaction${checkedCount === 1 ? '' : 's'}.`
                : 'Adjust categories before saving — only checked rows are updated.'}
            </div>
          </div>
        </div>
      )}
      open={open}
      width={bulkMode ? 840 : 1080}
      centered
      destroyOnClose
      maskClosable={!busy}
      onCancel={() => { if (!busy) onCancel() }}
      footer={(
        <div className="fs-classify-modal__footer">
          <Typography.Text type="secondary" className="fs-classify-modal__footer-meta">
            {applyCount > 0
              ? bulkMode && bulkCategoryName
                ? `${applyCount} of ${checkedCount} → ${bulkCategoryName}`
                : `${applyCount} of ${checkedCount} checked ready to save`
              : `${checkedCount} checked · pick a category above`}
          </Typography.Text>
          <Space>
            <Button onClick={onCancel} disabled={busy}>Cancel</Button>
            <Button
              type="primary"
              loading={busy}
              disabled={applyCount === 0}
              onClick={() => void onConfirm(readyRows)}
            >
              {footerLabel}
            </Button>
          </Space>
        </div>
      )}
    >
      {bulkMode ? (
        <div className="fs-classify-modal__bulk-bar">
          <div className="fs-classify-modal__bulk-bar-main">
            <Typography.Text strong className="fs-classify-modal__bulk-label">
              Category
            </Typography.Text>
            <CategoryPicker
              treeData={treeData}
              size="middle"
              className="fs-classify-picker fs-classify-picker--bulk-bar"
              placeholder="Search or pick a category…"
              value={bulkCategory}
              onChange={(v) => syncBulkCategory(v || undefined)}
            />
          </div>
          <Space size={4} className="fs-classify-modal__bulk-actions">
            <Button type="link" size="small" onClick={selectAll}>Select all</Button>
            <Button type="link" size="small" onClick={selectNone}>Clear</Button>
            {!showRowPickers && (
              <Button type="link" size="small" onClick={() => setShowRowPickers(true)}>
                Customize per row
              </Button>
            )}
          </Space>
        </div>
      ) : (
        <>
          <Alert
            type="info"
            showIcon
            className="fs-classify-modal__alert"
            message="Rule matches are exact; blue tags are recommendations. Use the bulk picker or change any row."
          />
          <div className="fs-classify-modal__bulk">
            <CategoryPicker
              treeData={treeData}
              size="middle"
              className="fs-classify-picker fs-classify-picker--bulk"
              placeholder="Bulk category for checked rows"
              value={bulkCategory}
              onChange={(v) => syncBulkCategory(v || undefined)}
            />
            <Space size={4}>
              <Button type="link" size="small" onClick={selectAll}>All</Button>
              <Button type="link" size="small" onClick={selectNone}>None</Button>
            </Space>
          </div>
        </>
      )}

      {preview && !bulkMode && (
        <div className="fs-classify-modal__stats">
          <div className="fs-classify-stat">
            <span className="fs-classify-stat__n">{preview.classified}</span>
            <span className="fs-classify-stat__l">matched</span>
          </div>
          {(preview.suggested ?? 0) > 0 && (
            <div className="fs-classify-stat fs-classify-stat--suggest">
              <span className="fs-classify-stat__n">{preview.suggested}</span>
              <span className="fs-classify-stat__l">recommended</span>
            </div>
          )}
          {preview.skipped > 0 && (
            <div className="fs-classify-stat fs-classify-stat--muted">
              <span className="fs-classify-stat__n">{preview.skipped}</span>
              <span className="fs-classify-stat__l">skipped</span>
            </div>
          )}
          {preview.noMatch > 0 && (
            <div className="fs-classify-stat fs-classify-stat--warn">
              <span className="fs-classify-stat__n">{preview.noMatch}</span>
              <span className="fs-classify-stat__l">no match</span>
            </div>
          )}
        </div>
      )}

      {bulkMode && preview && preview.skipped > 0 && (
        <div className="fs-classify-modal__stats fs-classify-modal__stats--compact">
          <Tag bordered={false} className="fs-classify-stat-pill">{preview.skipped} need manual category</Tag>
        </div>
      )}

      <Table<ClassifyEditRow>
        className="fs-classify-table"
        size="small"
        rowKey="id"
        dataSource={rows}
        pagination={rows.length > 25 ? { pageSize: 25, size: 'small', showTotal: (t) => `${t} rows` } : false}
        scroll={{ y: bulkMode ? Math.min(280, Math.max(88, rows.length * 44 + 48)) : 360 }}
        rowSelection={{
          selectedRowKeys: selectedKeys,
          onChange: (keys) => {
            const set = new Set(keys.map(String))
            setRows((prev) => prev.map((r) => {
              const enabled = set.has(r.id)
              if (!enabled) return { ...r, enabled: false }
              if (bulkCategory && !r.categoryCode) {
                const name = findTreeTitle(treeData, bulkCategory)
                return {
                  ...r,
                  enabled: true,
                  categoryCode: bulkCategory,
                  categoryName: name,
                  userEdited: bulkCategory !== (originalCodes[r.id] ?? ''),
                }
              }
              return { ...r, enabled: true }
            }))
          },
        }}
        locale={{ emptyText: 'No rows to classify — select transactions and try again.' }}
        columns={[
          {
            title: 'Date',
            dataIndex: 'transactionDate',
            width: 92,
            render: (v) => <span className="fs-mono fs-classify-date">{formatTableDate(v)}</span>,
          },
          {
            title: 'Transaction',
            dataIndex: 'transactionDesc',
            ellipsis: true,
            render: (v, r) => (
              <Tooltip title={cellText(r.reason) || undefined}>
                <span className="fs-classify-desc" title={cellText(v)}>{cellText(v) || '—'}</span>
              </Tooltip>
            ),
          },
          ...(bulkMode && !useRowPickers ? [{
            title: 'Change',
            width: 300,
            render: (_: unknown, r: ClassifyEditRow) => (
              <div className="fs-classify-change-cell">
                <Tag bordered={false} className="fs-classify-before-tag">{beforeLabel(r)}</Tag>
                <ArrowRightOutlined className="fs-classify-change-arrow" />
                {r.categoryCode ? (
                  <Tag color="processing" bordered={false} className="fs-classify-new-tag">
                    {r.categoryName || r.categoryCode}
                  </Tag>
                ) : (
                  <Typography.Text type="secondary" className="fs-classify-new-empty">Pick above</Typography.Text>
                )}
              </div>
            ),
          }] : [
            {
              title: 'Before',
              width: 110,
              render: (_: unknown, r: ClassifyEditRow) => (
                <span className="fs-classify-before">{beforeLabel(r)}</span>
              ),
            },
            {
              title: useRowPickers ? 'Category' : 'Suggested',
              width: useRowPickers ? 280 : 300,
              render: (_: unknown, r: ClassifyEditRow) => {
                if (!useRowPickers) {
                  return r.categoryName ? (
                    <Tag color="blue" bordered={false}>{r.categoryName}</Tag>
                  ) : '—'
                }
                const meta = sourceMeta(r.source, r.action)
                return (
                  <div className="fs-classify-category-cell">
                    <div className="fs-classify-category-meta">
                      {meta && <Tag bordered={false} color={meta.color} className="fs-classify-tag">{meta.label}</Tag>}
                      {r.userEdited && <Tag bordered={false} color="orange" className="fs-classify-tag">Edited</Tag>}
                    </div>
                    <CategoryPicker
                      treeData={treeData}
                      size="middle"
                      className="fs-classify-picker"
                      placeholder={r.suggestedKeywords?.length ? `Search: ${r.suggestedKeywords.slice(0, 2).join(', ')}` : 'Search or pick category'}
                      value={r.categoryCode}
                      onChange={(v) => updateCategory(r.id, v || undefined)}
                    />
                  </div>
                )
              },
            },
          ]),
          ...(!bulkMode ? [
            {
              title: 'Confidence',
              dataIndex: 'confidence',
              width: 80,
              render: (v: number | undefined, r: ClassifyEditRow) => {
                const text = formatConfidence(v)
                const low = Number(v) > 0 && Number(v) < 0.6
                return (
                  <Tooltip title={cellText(r.reason) || undefined}>
                    <Tag bordered={false} color={low ? 'orange' : 'green'} className="fs-classify-tag">{text}</Tag>
                  </Tooltip>
                )
              },
            },
            {
              title: 'Why',
              dataIndex: 'reason',
              width: 140,
              ellipsis: true,
              render: (v: string | undefined, r: ClassifyEditRow) => {
                const tip = cellText(v) || (r.suggestedKeywords?.length
                  ? `Suggested keywords: ${r.suggestedKeywords.join(', ')}`
                  : '')
                return (
                  <Tooltip title={tip}>
                    <span className="fs-classify-why">{cellText(v) || '—'}</span>
                  </Tooltip>
                )
              },
            },
          ] : []),
        ]}
      />
    </Modal>
  )
}
