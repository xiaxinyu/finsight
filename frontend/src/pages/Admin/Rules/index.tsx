import { useQuery } from '@tanstack/react-query'
import { Table } from 'antd'
import { TagsOutlined } from '@ant-design/icons'
import { listRules } from '../../api/admin'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { PageSkeleton } from '../../components/PageSkeleton'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'

export function RulesAdminPage() {
  const { data, isLoading } = useQuery({ queryKey: ['rules'], queryFn: listRules })
  const tableHeight = useViewportTableHeight(180)

  return (
    <DataPageLayout
      title="Category Rules"
      subtitle="Auto-classification keyword rules"
      icon={<TagsOutlined />}
    >
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
              { title: 'Enabled', dataIndex: 'enabled', width: 80 },
            ]}
          />
        </div>
      )}
    </DataPageLayout>
  )
}
