import { useRef } from 'react'
import { HistoryOutlined } from '@ant-design/icons'
import { ProTable, type ActionType } from '@ant-design/pro-components'
import { listStatements } from '../../api/statement'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { formatTableDate } from '../../utils/cell'

export function StatementListPage() {
  const actionRef = useRef<ActionType>(null)
  const tableHeight = useViewportTableHeight(180)

  return (
    <DataPageLayout
      title="Import History"
      subtitle="Past statement uploads and commit status"
      icon={<HistoryOutlined />}
    >
      <div className="fs-table-panel">
        <ProTable
          className="fs-data-table"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          search={false}
          scroll={{ y: tableHeight }}
          locale={{ emptyText: <EmptyState compact title="No imports yet" description="Upload a statement to get started." /> }}
          request={async (params) => {
            const res = await listStatements(params.current || 1, params.pageSize || 20)
            return { data: res.rows as Record<string, unknown>[], total: res.total, success: true }
          }}
          columns={[
            { title: 'ID', dataIndex: 'id', width: 120 },
            { title: 'File', dataIndex: 'fileName', ellipsis: true },
            { title: 'Status', dataIndex: 'status', width: 100 },
            { title: 'Created', dataIndex: 'createTime', width: 160, render: (_, r) => formatTableDate(r.createTime) },
          ]}
        />
      </div>
    </DataPageLayout>
  )
}
