import { useQuery } from '@tanstack/react-query'
import { Table } from 'antd'
import { PageContainer } from '@ant-design/pro-components'
import { listRules } from '../../api/admin'

export function RulesAdminPage() {
  const { data, isLoading } = useQuery({ queryKey: ['rules'], queryFn: listRules })
  return (
    <PageContainer title="Category Rules" loading={isLoading}>
      <Table rowKey="id" size="small" dataSource={(data as Record<string, unknown>[]) || []} columns={[
        { title: 'Keyword', dataIndex: 'keyword' },
        { title: 'Category', dataIndex: 'consumeName' },
        { title: 'Priority', dataIndex: 'priority', width: 80 },
        { title: 'Enabled', dataIndex: 'enabled', width: 80 },
      ]} />
    </PageContainer>
  )
}
