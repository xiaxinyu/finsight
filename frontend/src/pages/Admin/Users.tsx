import { useRef } from 'react'
import { Button, message, Popconfirm } from 'antd'
import { PlusOutlined, TeamOutlined } from '@ant-design/icons'
import { ProTable, type ActionType } from '@ant-design/pro-components'
import { createUser, deleteUser, listUsers, updateUser } from '../../api/admin'
import { DataPageLayout } from '../../components/DataPageLayout'
import { EmptyState } from '../../components/EmptyState'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'

export function UsersAdminPage() {
  const actionRef = useRef<ActionType>(null)
  const tableHeight = useViewportTableHeight(180)

  return (
    <DataPageLayout
      title="Users"
      subtitle="Manage application user accounts"
      icon={<TeamOutlined />}
      actions={<Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => message.info('Use row edit to add')}>Add</Button>}
    >
      <div className="fs-table-panel">
        <ProTable
          className="fs-data-table"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          scroll={{ y: tableHeight }}
          search={false}
          locale={{ emptyText: <EmptyState compact title="No users" /> }}
          request={async () => {
            const data = await listUsers()
            return { data: data as Record<string, unknown>[], total: (data as unknown[]).length, success: true }
          }}
          columns={[
            { title: 'Username', dataIndex: 'userName', editable: () => true },
            { title: 'Enabled', dataIndex: 'enabled', valueType: 'switch', editable: () => true },
            {
              title: 'Actions',
              valueType: 'option',
              render: (_, row) => [
                <Popconfirm key="del" title="Delete user?" onConfirm={async () => { await deleteUser(row.id as number); actionRef.current?.reload() }}>
                  <a>Delete</a>
                </Popconfirm>,
              ],
            },
          ]}
          editable={{
            type: 'single',
            onSave: async (_, row) => {
              if (row.id) await updateUser(row.id as number, row as Record<string, unknown>)
              else await createUser(row as Record<string, unknown>)
              message.success('Saved')
              actionRef.current?.reload()
            },
          }}
        />
      </div>
    </DataPageLayout>
  )
}
