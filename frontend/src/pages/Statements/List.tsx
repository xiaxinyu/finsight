import { useRef } from 'react'
import { PageContainer, ProTable, type ActionType } from '@ant-design/pro-components'
import { listStatements } from '../../api/statement'

export function StatementListPage() {
  const actionRef = useRef<ActionType>(null)
  return (
    <PageContainer title="Import History">
      <ProTable
        actionRef={actionRef}
        rowKey="id"
        search={false}
        request={async (params) => {
          const res = await listStatements(params.current || 1, params.pageSize || 20)
          return { data: res.rows as Record<string, unknown>[], total: res.total, success: true }
        }}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 120 },
          { title: 'File', dataIndex: 'fileName', ellipsis: true },
          { title: 'Status', dataIndex: 'status', width: 100 },
          { title: 'Created', dataIndex: 'createTime', width: 160 },
        ]}
      />
    </PageContainer>
  )
}
