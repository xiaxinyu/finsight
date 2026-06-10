import { useQuery } from '@tanstack/react-query'
import { Alert, Table, Tag } from 'antd'
import { TagsOutlined } from '@ant-design/icons'
import { listRules } from '../../../api/admin'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { EmptyState } from '../../../components/EmptyState'
import { PageSkeleton } from '../../../components/PageSkeleton'
import { useViewportTableHeight } from '../../../hooks/useViewportTableHeight'

export function RulesAdminPage() {
  const { data, isLoading, isError, error } = useQuery({ queryKey: ['rules'], queryFn: listRules })
  const tableHeight = useViewportTableHeight(180)

  return (
    <DataPageLayout
      title="Category Rules"
      subtitle="Read-only view of auto-classification keyword rules (managed in database seeds)"
      icon={<TagsOutlined />}
    >
      {isError && (
        <Alert type="error" showIcon style={{ marginBottom: 8 }}
          message="Failed to load rules" description={error instanceof Error ? error.message : 'Request failed'} />
      )}
      {isLoading ? (
        <PageSkeleton variant="table" />
      ) : (
        <div className="fs-table-panel" style={{ padding: 0 }}>
          <Table
            className="fs-data-table"
            rowKey="id"
            size="small"
            scroll={{ y: tableHeight }}
            dataSource={(data as Record<string, unknown>[]) || []}
            locale={{ emptyText: <EmptyState compact title="No rules" /> }}
            columns={[
              { title: 'Keyword', dataIndex: 'keyword' },
              { title: 'Category', dataIndex: 'consumeName' },
              { title: 'Priority', dataIndex: 'priority', width: 80 },
              {
                title: 'Enabled',
                dataIndex: 'enabled',
                width: 80,
                render: (v) => (v ? <Tag color="success">On</Tag> : <Tag>Off</Tag>),
              },
            ]}
          />
        </div>
      )}
    </DataPageLayout>
  )
}
