import { useRef } from 'react'
import { Button, Input, message, Popconfirm, Switch } from 'antd'
import { PlusOutlined, TeamOutlined } from '@ant-design/icons'
import { ProTable, type ActionType } from '@ant-design/pro-components'
import { createUser, deleteUser, listUsers, updateUser } from '../../../api/admin'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { EmptyState } from '../../../components/EmptyState'
import { useFillTableHeight } from '../../../hooks/useFillTableHeight'

export function UsersAdminPage() {
  const actionRef = useRef<ActionType>(null)
  const tablePanelRef = useRef<HTMLDivElement>(null)
  const tableHeight = useFillTableHeight(tablePanelRef)

  return (
    <DataPageLayout
      title="Users"
      subtitle="Application accounts and access"
      icon={<TeamOutlined />}
      className="fs-data-page--dense fs-data-page--fill"
      actions={(
        <Button
          type="primary"
          size="small"
          icon={<PlusOutlined />}
          onClick={() => actionRef.current?.addEditRecord?.({ userName: '', enabled: 1, password: '' }, { position: 'top' })}
        >
          Add user
        </Button>
      )}
    >
      <div ref={tablePanelRef} className="fs-table-panel fs-table-panel--editable">
        <ProTable
          className="fs-data-table"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          scroll={{ x: 'max-content', y: tableHeight }}
          search={false}
          options={{ density: true, reload: true }}
          locale={{ emptyText: <EmptyState compact title="No users" /> }}
          request={async () => {
            const data = await listUsers()
            return { data: data as Record<string, unknown>[], total: (data as unknown[]).length, success: true }
          }}
          columns={[
            { title: 'Username', dataIndex: 'userName', width: 200, editable: () => true },
            {
              title: 'Password',
              dataIndex: 'password',
              width: 160,
              editable: () => true,
              hideInTable: true,
              renderFormItem: () => <Input.Password size="small" placeholder="Leave blank to keep unchanged" />,
            },
            {
              title: 'Enabled',
              dataIndex: 'enabled',
              width: 90,
              editable: () => true,
              render: (_, r) => (r.enabled === 1 || r.enabled === true ? 'Yes' : 'No'),
              renderFormItem: (_, { value, onChange }) => (
                <Switch size="small" checked={value === 1 || value === true} onChange={(c) => onChange?.(c ? 1 : 0)} />
              ),
            },
            {
              title: 'Actions',
              valueType: 'option',
              width: 120,
              render: (_, row, __, action) => [
                <a key="edit" onClick={() => action?.startEditable?.(row.id as number)}>Edit</a>,
                <Popconfirm
                  key="del"
                  title="Delete user?"
                  onConfirm={async () => {
                    await deleteUser(row.id as number)
                    message.success('Deleted')
                    actionRef.current?.reload()
                  }}
                >
                  <a>Delete</a>
                </Popconfirm>,
              ],
            },
          ]}
          editable={{
            type: 'single',
            onSave: async (_, row) => {
              const payload = { ...row }
              if (!payload.password) delete payload.password
              if (row.id) await updateUser(row.id as number, payload as Record<string, unknown>)
              else await createUser(payload as Record<string, unknown>)
              message.success('Saved')
              actionRef.current?.reload()
            },
          }}
          pagination={{ defaultPageSize: 20, showSizeChanger: true, size: 'small', showTotal: (t) => `${t} users` }}
        />
      </div>
    </DataPageLayout>
  )
}
