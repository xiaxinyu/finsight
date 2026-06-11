import { useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Alert, Button, Drawer, Input, InputNumber, Segmented, Space, Table, Tag, Tooltip, Typography,
} from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import {
  fetchStatementSourceLines,
  type StatementSourceLineKind,
  type StatementSourceLineRow,
  type StatementSourceView,
} from '../api/statement'
import { useViewportTableHeight } from '../hooks/useViewportTableHeight'
import { formatStatementDisplay } from '../utils/statementDisplay'
import { cellText } from '../utils/cell'

type KindFilter = 'all' | StatementSourceLineKind

function kindMeta(kind: StatementSourceLineKind) {
  switch (kind) {
    case 'linked':
      return { color: 'success' as const, label: 'Linked' }
    case 'skipped':
      return { color: 'error' as const, label: 'Skipped' }
    case 'header':
      return { color: 'blue' as const, label: 'Header' }
    case 'ignored':
      return { color: 'default' as const, label: 'Ignored' }
    default:
      return { color: 'default' as const, label: 'Noise' }
  }
}

type Props = {
  open: boolean
  statementId: string
  fileName?: string
  bankCode?: string
  onClose: () => void
}

export function StatementSourceDrawer({ open, statementId, fileName, bankCode, onClose }: Props) {
  const tableWrapRef = useRef<HTMLDivElement>(null)
  const tableHeight = useViewportTableHeight(220)
  const [search, setSearch] = useState('')
  const [kindFilter, setKindFilter] = useState<KindFilter>('all')
  const [jumpLine, setJumpLine] = useState<number | null>(null)

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['statement-source', statementId],
    queryFn: () => fetchStatementSourceLines(statementId),
    enabled: open && Boolean(statementId),
    staleTime: 30_000,
  })

  const view = (data || { rows: [] }) as StatementSourceView
  const headers = view.columnHeaders || []
  const display = formatStatementDisplay(fileName || view.fileName || '', bankCode || view.bankCode, statementId)

  const filteredRows = useMemo(() => {
    const rows = view.rows || []
    const q = search.trim().toLowerCase()
    return rows.filter((r) => {
      if (kindFilter !== 'all' && r.kind !== kindFilter) return false
      if (jumpLine != null && r.fileLineNumber !== jumpLine && r.lineNumber !== jumpLine) return false
      if (!q) return true
      const hay = [
        String(r.fileLineNumber),
        String(r.lineNumber),
        r.originalLine,
        r.reason,
        r.hint,
        ...(r.columns || []),
      ].join(' ').toLowerCase()
      return hay.includes(q)
    })
  }, [view.rows, search, kindFilter, jumpLine])

  const columns = useMemo(() => {
    const base = [
      {
        title: 'L#',
        width: 56,
        fixed: 'left' as const,
        render: (_: unknown, r: StatementSourceLineRow) => (
          <Tooltip title={`File line ${r.fileLineNumber}`}>
            <span className="fs-mono fs-source-line-no">{r.fileLineNumber}</span>
          </Tooltip>
        ),
      },
      {
        title: 'Kind',
        width: 88,
        fixed: 'left' as const,
        render: (_: unknown, r: StatementSourceLineRow) => {
          const meta = kindMeta(r.kind)
          return <Tag className="fs-tag" color={meta.color}>{meta.label}</Tag>
        },
      },
    ]
    const dynamic = headers.map((h, idx) => ({
      title: h || `Col ${idx + 1}`,
      width: Math.min(160, Math.max(88, (h || '').length * 10)),
      ellipsis: true,
      render: (_: unknown, r: StatementSourceLineRow) => {
        const val = cellText(r.columns?.[idx])
        const strong = r.kind === 'header'
        return (
          <span
            className={strong ? 'fs-source-cell fs-source-cell--header' : 'fs-source-cell'}
            title={val}
          >
            {val || '—'}
          </span>
        )
      },
    }))
    return [...base, ...dynamic]
  }, [headers])

  return (
    <Drawer
      title="Source data"
      width="min(96vw, 1120px)"
      open={open}
      onClose={onClose}
      destroyOnClose
      className="fs-source-drawer"
      extra={(
        <Button size="small" onClick={() => refetch()} loading={isLoading}>Refresh</Button>
      )}
    >
      <div className="fs-source-drawer-head">
        <div>
          <Typography.Text strong>{display.title}</Typography.Text>
          <Typography.Text type="secondary" className="fs-source-drawer-sub">
            {display.subtitle}
          </Typography.Text>
        </div>
        <div className="fs-source-stats">
          <span className="fs-source-stat"><b>{view.lines ?? 0}</b> lines</span>
          <span className="fs-source-stat fs-source-stat--ok"><b>{view.linked ?? 0}</b> linked</span>
          <span className="fs-source-stat fs-source-stat--warn"><b>{view.skipped ?? 0}</b> skipped</span>
          <span className="fs-source-stat"><b>{view.ignored ?? 0}</b> ignored</span>
          <span className="fs-source-stat"><b>{view.transactions ?? 0}</b> txns</span>
        </div>
      </div>

      {isError && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 8 }}
          message="Failed to load source lines"
          description={error instanceof Error ? error.message : 'Request failed'}
        />
      )}

      <div className="fs-source-toolbar">
        <Input
          allowClear
          size="small"
          prefix={<SearchOutlined className="fs-input-icon" />}
          placeholder="Search line text, amount, date…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="fs-source-search"
        />
        <Space size={4}>
          <span className="fs-source-jump-label">Jump to line</span>
          <InputNumber
            size="small"
            min={1}
            placeholder="#"
            value={jumpLine}
            onChange={(v) => setJumpLine(typeof v === 'number' ? v : null)}
            className="fs-source-jump-input"
          />
          {jumpLine != null && (
            <Button size="small" type="link" onClick={() => setJumpLine(null)}>Clear</Button>
          )}
        </Space>
        <Segmented
          size="small"
          value={kindFilter}
          onChange={(v) => setKindFilter(v as KindFilter)}
          options={[
            { label: 'All', value: 'all' },
            { label: 'Linked', value: 'linked' },
            { label: 'Skipped', value: 'skipped' },
            { label: 'Ignored', value: 'ignored' },
            { label: 'Header', value: 'header' },
          ]}
        />
      </div>

      <div ref={tableWrapRef} className="fs-source-table-wrap">
        <Table<StatementSourceLineRow>
          size="small"
          className="fs-data-table fs-source-table"
          rowKey={(r) => `${r.fileLineNumber}-${r.lineNumber}`}
          loading={isLoading}
          dataSource={filteredRows}
          columns={columns}
          pagination={{ pageSize: 50, size: 'small', showTotal: (t) => `${t} rows` }}
          scroll={{ x: 'max-content', y: tableHeight }}
          rowClassName={(r) => `fs-source-row fs-source-row--${r.kind}`}
          expandable={{
            expandedRowRender: (r) => (
              <div className="fs-source-expand">
                {r.reason && (
                  <div className="fs-source-expand-block">
                    <span className="fs-source-expand-label">Reason</span>
                    <span>{r.reason}</span>
                  </div>
                )}
                {r.hint && (
                  <div className="fs-source-expand-block">
                    <span className="fs-source-expand-label">Diagnostics</span>
                    <pre className="fs-source-expand-pre">{r.hint}</pre>
                  </div>
                )}
                <div className="fs-source-expand-block">
                  <span className="fs-source-expand-label">Original line (file L{r.fileLineNumber})</span>
                  <pre className="fs-source-expand-pre">{r.originalLine || '—'}</pre>
                </div>
              </div>
            ),
            rowExpandable: (r) => r.kind === 'skipped' || Boolean(r.hint) || Boolean(r.originalLine),
          }}
          locale={{
            emptyText: view.rows?.length
              ? 'No rows match the current filter'
              : 'No source content stored for this import',
          }}
        />
      </div>
    </Drawer>
  )
}
