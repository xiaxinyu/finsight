import { useCallback, useMemo, useState } from 'react'
import { ThunderboltOutlined } from '@ant-design/icons'
import { Alert, Button, Modal, Space, Table, Tag, Tooltip, Typography } from 'antd'
import { CategoryPicker } from './CategoryPicker'
import type { TreeSelectNode } from '../hooks/useConsumeTree'
import type { ReclassifyPreviewRow, ReclassifyResult } from '../api/transaction'
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
    enabled: Boolean(p.categoryCode),
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

  const [rows, setRows] = useState<ClassifyEditRow[]>(() => toEditRows(previewRows))

  const applyCount = useMemo(
    () => rows.filter((r) => r.enabled && r.categoryCode).length,
    [rows],
  )
  const editedCount = useMemo(() => rows.filter((r) => r.userEdited).length, [rows])

  const updateCategory = useCallback((id: string, code: string | undefined) => {
    const value = code || ''
    const name = value ? findTreeTitle(treeData, value) : ''
    setRows((prev) => prev.map((r) => {
      if (r.id !== id) return r
      return {
        ...r,
        categoryCode: value,
        categoryName: name || r.categoryName,
        enabled: value ? true : r.enabled,
        userEdited: value !== (originalCodes[id] ?? ''),
      }
    }))
  }, [originalCodes, treeData])

  const selectedKeys = useMemo(() => rows.filter((r) => r.enabled).map((r) => r.id), [rows])

  return (
    <Modal
      className="fs-classify-modal"
      title={(
        <div className="fs-classify-modal__title">
          <span className="fs-classify-modal__icon" aria-hidden>
            <ThunderboltOutlined />
          </span>
          <div>
            <div className="fs-classify-modal__heading">Review auto-classify</div>
            <div className="fs-classify-modal__sub">Adjust categories before saving — only checked rows are updated.</div>
          </div>
        </div>
      )}
      open={open}
      width={1080}
      centered
      destroyOnClose
      maskClosable={!busy}
      onCancel={() => { if (!busy) onCancel() }}
      footer={(
        <div className="fs-classify-modal__footer">
          <Typography.Text type="secondary" className="fs-classify-modal__footer-meta">
            {applyCount} row{applyCount === 1 ? '' : 's'} ready
            {editedCount > 0 && <> · {editedCount} edited</>}
          </Typography.Text>
          <Space>
            <Button onClick={onCancel} disabled={busy}>Cancel</Button>
            <Button
              type="primary"
              loading={busy}
              disabled={applyCount === 0}
              onClick={() => void onConfirm(rows.filter((r) => r.enabled && r.categoryCode))}
            >
              Apply {applyCount} categor{applyCount === 1 ? 'y' : 'ies'}
            </Button>
          </Space>
        </div>
      )}
    >
      <Alert
        type="info"
        showIcon
        className="fs-classify-modal__alert"
        message="Rule matches are exact; blue tags are recommendations. Change any category via the dropdown."
      />

      {preview && (
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

      <Table<ClassifyEditRow>
        className="fs-classify-table"
        size="small"
        rowKey="id"
        dataSource={rows}
        pagination={rows.length > 8 ? { pageSize: 8, size: 'small', showTotal: (t) => `${t} rows` } : false}
        scroll={{ y: 360 }}
        rowSelection={{
          selectedRowKeys: selectedKeys,
          onChange: (keys) => {
            const set = new Set(keys.map(String))
            setRows((prev) => prev.map((r) => ({ ...r, enabled: set.has(r.id) })))
          },
        }}
        locale={{ emptyText: 'No rows to classify — try different filters or add rules.' }}
        columns={[
          {
            title: 'Date',
            dataIndex: 'transactionDate',
            width: 96,
            render: (v) => <span className="fs-mono fs-classify-date">{formatTableDate(v)}</span>,
          },
          {
            title: 'Transaction',
            dataIndex: 'transactionDesc',
            ellipsis: true,
            render: (v) => (
              <span className="fs-classify-desc" title={cellText(v)}>{cellText(v) || '—'}</span>
            ),
          },
          {
            title: 'Before',
            width: 120,
            render: (_, r) => (
              <span className="fs-classify-before">{beforeLabel(r)}</span>
            ),
          },
          {
            title: 'Suggested',
            width: 300,
            render: (_, r) => {
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
          {
            title: 'Confidence',
            dataIndex: 'confidence',
            width: 88,
            render: (v, r) => {
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
            width: 168,
            ellipsis: true,
            render: (v, r) => {
              const tip = cellText(v) || (r.suggestedKeywords?.length
                ? `Suggested keywords: ${r.suggestedKeywords.join(', ')}`
                : '')
              return (
                <Tooltip title={tip}>
                  <span className="fs-classify-why">{cellText(v) || (r.suggestedKeywords?.length ? `Keywords: ${r.suggestedKeywords.slice(0, 2).join(', ')}` : '—')}</span>
                </Tooltip>
              )
            },
          },
        ]}
      />
    </Modal>
  )
}
