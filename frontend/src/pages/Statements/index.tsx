import { useCallback, useMemo, useRef, useState, type MouseEvent } from 'react'
import { Link } from 'react-router-dom'
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloudUploadOutlined,
  DatabaseOutlined,
  EyeOutlined,
  FileTextOutlined,
  HistoryOutlined,
  SearchOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons'
import { ProTable, type ActionType } from '@ant-design/pro-components'
import { Alert, Button, Input, Segmented, Space, Tag, Tooltip, Typography } from 'antd'
import { listStatements, type StatementListRow } from '../../api/statement'
import { StatementSourceDrawer } from '../../components/StatementSourceDrawer'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { useFillTableHeight } from '../../hooks/useFillTableHeight'
import {
  BANK_LABELS,
  formatStatementDisplay,
  formatStatementWhen,
  normalizeStatementStatus,
  type StatementStatusKind,
} from '../../utils/statementDisplay'

type StatusFilter = 'all' | StatementStatusKind

function statusMeta(kind: StatementStatusKind) {
  switch (kind) {
    case 'committed':
      return {
        color: 'success' as const,
        label: 'Committed',
        hint: 'In ledger — visible in Transactions',
        icon: <CheckCircleOutlined />,
      }
    case 'pending':
      return {
        color: 'processing' as const,
        label: 'Preview',
        hint: 'Staged only — commit to add to ledger',
        icon: <ClockCircleOutlined />,
      }
    case 'failed':
      return {
        color: 'error' as const,
        label: 'Failed',
        hint: 'Upload or parse failed',
        icon: <FileTextOutlined />,
      }
    default:
      return {
        color: 'default' as const,
        label: 'Unknown',
        hint: '',
        icon: <FileTextOutlined />,
      }
  }
}

function SummaryPill({
  label,
  value,
  tone,
  active,
  onClick,
}: {
  label: string
  value: number
  tone?: 'pending' | 'committed' | 'default'
  active?: boolean
  onClick?: () => void
}) {
  return (
    <button
      type="button"
      className={`fs-import-summary-pill${tone ? ` fs-import-summary-pill--${tone}` : ''}${active ? ' fs-import-summary-pill--active' : ''}`}
      onClick={onClick}
    >
      <span className="fs-import-summary-pill-value">{value}</span>
      <span className="fs-import-summary-pill-label">{label}</span>
    </button>
  )
}

export function StatementListPage() {
  const actionRef = useRef<ActionType>(null)
  const tablePanelRef = useRef<HTMLDivElement>(null)
  const tableHeight = useFillTableHeight(tablePanelRef)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all')
  const [search, setSearch] = useState('')
  const [summary, setSummary] = useState({ total: 0, pending: 0, committed: 0 })
  const [sourceDrawer, setSourceDrawer] = useState<StatementListRow | null>(null)

  const openSource = useCallback((row: StatementListRow, e?: MouseEvent) => {
    e?.stopPropagation()
    setSourceDrawer(row)
  }, [])

  return (
    <DataPageLayout
      title="Import History"
      subtitle="Track uploads, preview staged rows, and commit to your ledger"
      icon={<HistoryOutlined />}
      className="fs-data-page--dense fs-data-page--fill"
      actions={(
        <Link to="/statements/upload">
          <Button type="primary" icon={<CloudUploadOutlined />}>New import</Button>
        </Link>
      )}
    >
      {loadError && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 8 }}
          message="Failed to load imports"
          description={loadError}
        />
      )}

      <div className="fs-import-history-summary">
        <SummaryPill
          label="All imports"
          value={summary.total}
          active={statusFilter === 'all'}
          onClick={() => setStatusFilter('all')}
        />
        <SummaryPill
          label="Awaiting commit"
          value={summary.pending}
          tone="pending"
          active={statusFilter === 'pending'}
          onClick={() => setStatusFilter('pending')}
        />
        <SummaryPill
          label="Committed"
          value={summary.committed}
          tone="committed"
          active={statusFilter === 'committed'}
          onClick={() => setStatusFilter('committed')}
        />
      </div>

      <div className="fs-import-history-toolbar">
        <Input
          allowClear
          size="small"
          prefix={<SearchOutlined className="fs-input-icon" />}
          placeholder="Search by file name, bank, or ref…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="fs-import-history-search"
        />
        <Segmented
          size="small"
          value={statusFilter}
          onChange={(v) => setStatusFilter(v as StatusFilter)}
          options={[
            { label: 'All', value: 'all' },
            { label: 'Preview', value: 'pending' },
            { label: 'Committed', value: 'committed' },
          ]}
        />
      </div>

      <div ref={tablePanelRef} className="fs-table-panel fs-import-history-panel">
        <ProTable<StatementListRow>
          className="fs-data-table fs-import-history-table"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          search={false}
          options={{ density: true, reload: true, setting: false }}
          scroll={{ x: 880, y: tableHeight }}
          onRow={(r) => ({
            onClick: () => openSource(r),
            style: { cursor: 'pointer' },
          })}
          rowClassName={(r) => {
            const kind = normalizeStatementStatus(r.status)
            const active = sourceDrawer?.id === r.id
            return `fs-table-row fs-import-row--clickable${kind === 'pending' ? ' fs-import-row--pending' : ''}${active ? ' fs-import-row--active' : ''}`
          }}
          locale={{
            emptyText: (
              <EmptyState
                compact
                title={search || statusFilter !== 'all' ? 'No matching imports' : 'No imports yet'}
                description={
                  search || statusFilter !== 'all'
                    ? 'Try clearing filters or upload a new statement.'
                    : 'Upload a bank statement to preview and commit transactions.'
                }
                action={(
                  <Link to="/statements/upload">
                    <Button type="primary" size="small" icon={<CloudUploadOutlined />}>Upload statement</Button>
                  </Link>
                )}
              />
            ),
          }}
          request={async (params) => {
            try {
              setLoadError(null)
              const res = await listStatements(1, 200)
              const all = (res.rows || []) as StatementListRow[]

              let pending = 0
              let committed = 0
              for (const row of all) {
                const kind = normalizeStatementStatus(row.status)
                if (kind === 'pending') pending += 1
                else if (kind === 'committed') committed += 1
              }
              setSummary({ total: res.total || all.length, pending, committed })

              const q = search.trim().toLowerCase()
              let filtered = all
              if (statusFilter !== 'all') {
                filtered = filtered.filter((r) => normalizeStatementStatus(r.status) === statusFilter)
              }
              if (q) {
                filtered = filtered.filter((r) => {
                  const bank = (r.sourceBankCode || r.source || '').toLowerCase()
                  const bankLabel = (BANK_LABELS[(r.sourceBankCode || r.source || '').toUpperCase()] || '').toLowerCase()
                  const file = String(r.fileName || '').toLowerCase()
                  const id = String(r.id || '').toLowerCase()
                  return file.includes(q) || bank.includes(q) || bankLabel.includes(q) || id.includes(q)
                })
              }

              const page = params.current || 1
              const pageSize = params.pageSize || 20
              const start = (page - 1) * pageSize
              const pageRows = filtered.slice(start, start + pageSize)

              return { data: pageRows, total: filtered.length, success: true }
            } catch (e) {
              const msg = e instanceof Error ? e.message : 'Request failed'
              setLoadError(msg)
              return { data: [], total: 0, success: false }
            }
          }}
          params={{ statusFilter, search }}
          columns={useMemo(() => [
            {
              title: 'Statement',
              dataIndex: 'fileName',
              width: 360,
              ellipsis: true,
              render: (_, r) => {
                const display = formatStatementDisplay(
                  r.fileName || '',
                  r.sourceBankCode || r.source,
                  r.id,
                  r.createdAt ?? r.createTime ?? r.createtime,
                )
                const bank = (r.sourceBankCode || r.source || '').toUpperCase()
                return (
                  <div className="fs-statement-cell">
                    <span className="fs-statement-cell-icon" aria-hidden>
                      <FileTextOutlined />
                    </span>
                    <div className="fs-statement-cell-body">
                      <Typography.Text strong className="fs-statement-cell-title" ellipsis={{ tooltip: display.title }}>
                        {display.title}
                      </Typography.Text>
                      <Typography.Text type="secondary" className="fs-statement-cell-meta" ellipsis={{ tooltip: display.subtitle }}>
                        {display.subtitle || String(r.fileName || '—')}
                      </Typography.Text>
                      <Space size={6} className="fs-statement-cell-actions">
                        {bank && <Tag className="fs-tag fs-statement-bank-tag">{bank}</Tag>}
                        <Button
                          type="link"
                          size="small"
                          className="fs-statement-source-link"
                          icon={<DatabaseOutlined />}
                          onClick={(e) => openSource(r, e)}
                        >
                          View source
                        </Button>
                      </Space>
                    </div>
                  </div>
                )
              },
            },
            {
              title: 'Rows',
              dataIndex: 'itemCount',
              width: 72,
              align: 'center',
              render: (_, r) => {
                const n = r.itemCount ?? r.rowCount
                return (
                  <Tooltip title="Parsed transaction rows staged or committed">
                    <span className="fs-mono fs-statement-rows">{n != null ? String(n) : '—'}</span>
                  </Tooltip>
                )
              },
            },
            {
              title: 'Source',
              key: 'source',
              width: 108,
              render: (_, r) => (
                <Button
                  type="primary"
                  ghost
                  size="small"
                  icon={<DatabaseOutlined />}
                  className="fs-statement-source-btn"
                  onClick={(e) => openSource(r, e)}
                >
                  Source
                </Button>
              ),
            },
            {
              title: 'Status',
              dataIndex: 'status',
              width: 200,
              render: (_, r) => {
                const meta = statusMeta(normalizeStatementStatus(r.status))
                return (
                  <div className="fs-statement-status">
                    <Tag className="fs-tag" color={meta.color} icon={meta.icon}>{meta.label}</Tag>
                    {meta.hint && (
                      <span className="fs-statement-status-hint">{meta.hint}</span>
                    )}
                  </div>
                )
              },
            },
            {
              title: 'Uploaded',
              dataIndex: 'createdAt',
              width: 148,
              render: (_, r) => {
                const when = formatStatementWhen(r.createdAt ?? r.createTime ?? r.createtime)
                return (
                  <div className="fs-statement-when">
                    <span className="fs-mono">{when.date}</span>
                    {when.relative && (
                      <span className="fs-statement-when-rel">{when.relative}</span>
                    )}
                  </div>
                )
              },
            },
            {
              title: '',
              key: 'actions',
              width: 168,
              render: (_, r) => {
                const kind = normalizeStatementStatus(r.status)
                const id = String(r.id || '')
                if (!id) return null
                return (
                  <Space size={4} wrap className="fs-statement-actions" onClick={(e) => e.stopPropagation()}>
                    {kind === 'pending' && (
                      <Link to={`/statements/upload?resume=${encodeURIComponent(id)}`}>
                        <Button type="primary" size="small" icon={<EyeOutlined />}>
                          Resume
                        </Button>
                      </Link>
                    )}
                    {kind === 'committed' && (
                      <Link to="/transactions">
                        <Button size="small" icon={<UnorderedListOutlined />}>Txns</Button>
                      </Link>
                    )}
                  </Space>
                )
              },
            },
          ], [openSource, sourceDrawer?.id])}
          pagination={{
            defaultPageSize: 20,
            showSizeChanger: true,
            size: 'small',
            showTotal: (t) => `${t} import${t === 1 ? '' : 's'}`,
          }}
        />
      </div>

      <StatementSourceDrawer
        open={Boolean(sourceDrawer)}
        statementId={String(sourceDrawer?.id || '')}
        fileName={sourceDrawer?.fileName}
        bankCode={sourceDrawer?.sourceBankCode || sourceDrawer?.source}
        onClose={() => setSourceDrawer(null)}
      />
    </DataPageLayout>
  )
}
