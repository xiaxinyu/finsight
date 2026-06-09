import { Tree } from 'antd'
import { ClusterOutlined } from '@ant-design/icons'
import { useConsumeAntTree } from '../../hooks/useConsumeTree'
import { DataPageLayout } from '../../components/DataPageLayout'
import { PageSkeleton } from '../../components/PageSkeleton'

export function CategoriesAdminPage() {
  const { treeData, isLoading } = useConsumeAntTree()

  return (
    <DataPageLayout
      title="Categories"
      subtitle="Expense and income category hierarchy"
      icon={<ClusterOutlined />}
    >
      <div className="fs-table-panel" style={{ padding: 12 }}>
        {isLoading ? (
          <PageSkeleton variant="table" />
        ) : (
          <Tree showLine defaultExpandAll treeData={treeData} />
        )}
      </div>
    </DataPageLayout>
  )
}
