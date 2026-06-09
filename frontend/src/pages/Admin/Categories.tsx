import { Tree } from 'antd'
import { PageContainer } from '@ant-design/pro-components'
import { useConsumeAntTree } from '../../hooks/useConsumeTree'

export function CategoriesAdminPage() {
  const { treeData, isLoading } = useConsumeAntTree()

  return (
    <PageContainer title="Categories" loading={isLoading}>
      <Tree showLine defaultExpandAll treeData={treeData} />
    </PageContainer>
  )
}
