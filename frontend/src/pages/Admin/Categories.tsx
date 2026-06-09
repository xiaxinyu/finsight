import { useQuery } from '@tanstack/react-query'
import { Tree } from 'antd'
import { PageContainer } from '@ant-design/pro-components'
import { consumeTree } from '../../api/transaction'

export function CategoriesAdminPage() {
  const { data, isLoading } = useQuery({ queryKey: ['admin-categories'], queryFn: () => consumeTree() })

  function toTree(nodes: { id: string; text: string; children?: typeof nodes }[]): { key: string; title: string; children?: ReturnType<typeof toTree> }[] {
    return (nodes || []).map((n) => ({ key: n.id, title: n.text, children: n.children ? toTree(n.children) : undefined }))
  }

  return (
    <PageContainer title="Categories" loading={isLoading}>
      <Tree showLine defaultExpandAll treeData={toTree(data || [])} />
    </PageContainer>
  )
}
