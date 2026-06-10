import { Alert, Tree } from 'antd'
import { ClusterOutlined } from '@ant-design/icons'
import { useConsumeAntTree } from '../../../hooks/useConsumeTree'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { EmptyState } from '../../../components/EmptyState'
import { PageSkeleton } from '../../../components/PageSkeleton'

export function CategoriesAdminPage() {
  const { treeData, isLoading, isError, error } = useConsumeAntTree()

  return (
    <DataPageLayout
      title="Categories"
      subtitle="Expense and income category hierarchy"
      icon={<ClusterOutlined />}
    >
      {isError && (
        <Alert type="error" showIcon style={{ marginBottom: 8 }}
          message="Failed to load categories" description={error instanceof Error ? error.message : 'Request failed'} />
      )}
      <div className="fs-table-panel" style={{ padding: 12 }}>
        {isLoading ? (
          <PageSkeleton variant="table" />
        ) : !treeData?.length ? (
          <EmptyState compact title="No categories" description="Category tree is empty." />
        ) : (
          <Tree showLine defaultExpandAll treeData={treeData} />
        )}
      </div>
    </DataPageLayout>
  )
}
