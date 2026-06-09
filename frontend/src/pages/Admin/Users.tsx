import { useRef } from 'react'
import { Button, message, Popconfirm } from 'antd'
import { PageContainer, ProTable, type ActionType } from '@ant-design/pro-components'
import { createUser, deleteUser, listUsers, updateUser } from '../../api/admin'

export function UsersAdminPage() {
  const actionRef = useRef<ActionType>(null)
  return (
    <PageContainer title="Users">
      <ProTable
        actionRef={actionRef}
        rowKey="id"
        toolBarRender={() => [<Button key="add" type="primary" onClick={() => message.info('Use row edit to add')}>Add</Button>]}
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
    </PageContainer>
  )
}
