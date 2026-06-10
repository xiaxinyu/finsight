import { useRef } from 'react'
import { FileTextOutlined, HistoryOutlined } from '@ant-design/icons'
import { ProTable, type ActionType } from '@ant-design/pro-components'
import { Alert, Tag, Tooltip } from 'antd'
import { useState } from 'react'
import { listStatements } from '../../api/statement'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { formatTableDate } from '../../utils/cell'

function statusTag(status: unknown) {
  const s = String(status || 'unknown').toLowerCase()
  if (s.includes('commit') || s === 'done' || s === 'success') {
    return <Tag className="fs-tag" color="success">Committed</Tag>
  }
  if (s.includes('preview') || s.includes('pending') || s === 'uploaded') {
    return <Tag className="fs-tag" color="processing">Preview</Tag>
  }
  if (s.includes('fail') || s.includes('error')) {
    return <Tag className="fs-tag" color="error">Failed</Tag>
  }
  return <Tag className="fs-tag">{status ? String(status) : '—'}</Tag>
}

function shortId(id: unknown) {
  const s = String(id || '')
  if (s.length <= 10) return s
  return (
    <Tooltip title={s}>
      <span className="fs-mono fs-id-short">{s.slice(0, 8)}…</span>
    </Tooltip>
  )
}

export function StatementListPage() {
  const actionRef = useRef<ActionType>(null)
  const tableHeight = useViewportTableHeight(180)
  const [loadError, setLoadError] = useState<string | null>(null)

  return (
    <DataPageLayout
      title="Import History"
      subtitle="Past statement uploads and commit status"
      icon={<HistoryOutlined />}
    >
      {loadError && (
        <Alert type="error" showIcon style={{ marginBottom: 8 }} message="Failed to load imports" description={loadError} />
      )}
      <div className="fs-table-panel">
        <ProTable
          className="fs-data-table"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          search={false}
          options={{ density: true, reload: true }}
          scroll={{ x: 'max-content', y: tableHeight }}
          rowClassName={() => 'fs-table-row'}
          locale={{ emptyText: <EmptyState compact title="No imports yet" description="Upload a statement to get started." /> }}
          request={async (params) => {
            try {
              setLoadError(null)
              const res = await listStatements(params.current || 1, params.pageSize || 20)
              return { data: res.rows as Record<string, unknown>[], total: res.total, success: true }
            } catch (e) {
              const msg = e instanceof Error ? e.message : 'Request failed'
              setLoadError(msg)
              return { data: [], total: 0, success: false }
            }
          }}
          columns={[
            {
              title: 'Ref',
              dataIndex: 'id',
              width: 88,
              render: (_, r) => shortId(r.id),
            },
            {
              title: 'File',
              dataIndex: 'fileName',
              ellipsis: true,
              render: (_, r) => (
                <span className="fs-file-cell">
                  <FileTextOutlined className="fs-file-icon" />
                  <span title={String(r.fileName || '')}>{String(r.fileName || '—')}</span>
                </span>
              ),
            },
            {
              title: 'Status',
              dataIndex: 'status',
              width: 110,
              render: (_, r) => statusTag(r.status),
            },
            {
              title: 'Imported',
              dataIndex: 'createTime',
              width: 120,
              render: (_, r) => <span className="fs-mono">{formatTableDate(r.createTime)}</span>,
            },
          ]}
          pagination={{ defaultPageSize: 20, showSizeChanger: true, size: 'small', showTotal: (t) => `${t} imports` }}
        />
      </div>
    </DataPageLayout>
  )
}
