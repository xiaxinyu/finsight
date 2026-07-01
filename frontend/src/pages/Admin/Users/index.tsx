import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Avatar, Badge, Button, Dropdown, Form, Input, Modal, Select, Space, Tag, Typography, message,
} from 'antd'
import {
  DeleteOutlined, EditOutlined, KeyOutlined, MoreOutlined, PlusOutlined, SearchOutlined, TeamOutlined, UserOutlined,
} from '@ant-design/icons'
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import {
  createUser,
  deleteUser,
  listRoles,
  listUsers,
  resetUserPassword,
  updateUser,
  type RoleRow,
  type UserAdminRow,
} from '../../../api/admin'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { EmptyState } from '../../../components/EmptyState'
import { useFillTableHeight } from '../../../hooks/useFillTableHeight'
import { useAuth } from '../../../hooks/useAuth'
import { avatarColor, ROLE_TAG_COLORS, userInitials } from '../../../utils/userDisplay'

type UserFormValues = {
  username?: string
  displayName?: string
  password?: string
  enabled?: number
  roleIds?: number[]
}

type UserStats = {
  total: number
  active: number
  admins: number
}

function formatTs(v?: string) {
  if (!v) return '—'
  try {
    return new Date(v).toLocaleString(undefined, {
      year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
    })
  } catch {
    return v
  }
}

function computeStats(rows: UserAdminRow[]): UserStats {
  return {
    total: rows.length,
    active: rows.filter((r) => r.enabled === 1).length,
    admins: rows.filter((r) => r.roles?.includes('ADMIN')).length,
  }
}

export function UsersAdminPage() {
  const actionRef = useRef<ActionType>(null)
  const tablePanelRef = useRef<HTMLDivElement>(null)
  const tableHeight = useFillTableHeight(tablePanelRef)
  const { username: currentUser } = useAuth()
  const [search, setSearch] = useState('')
  const [searchDraft, setSearchDraft] = useState('')
  const searchQueryRef = useRef('')
  const [stats, setStats] = useState<UserStats>({ total: 0, active: 0, admins: 0 })
  const [roles, setRoles] = useState<RoleRow[]>([])
  const [modalOpen, setModalOpen] = useState(false)
  const [resetOpen, setResetOpen] = useState(false)
  const [editing, setEditing] = useState<UserAdminRow | null>(null)
  const [resetTarget, setResetTarget] = useState<UserAdminRow | null>(null)
  const [form] = Form.useForm<UserFormValues>()
  const [resetForm] = Form.useForm<{ password: string }>()

  useEffect(() => {
    listRoles().then(setRoles).catch(() => {})
  }, [])

  const roleOptions = useMemo(
    () => roles.map((r) => ({ label: `${r.name ?? r.code} (${r.code})`, value: r.id })),
    [roles],
  )

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ enabled: 1, roleIds: roles.filter((r) => r.code === 'USER').map((r) => r.id) })
    setModalOpen(true)
  }

  const openEdit = (row: UserAdminRow) => {
    setEditing(row)
    const roleIds = roles.filter((r) => row.roles?.includes(r.code)).map((r) => r.id)
    form.setFieldsValue({
      username: row.username,
      displayName: row.displayName,
      enabled: row.enabled ?? 1,
      roleIds,
    })
    setModalOpen(true)
  }

  const openReset = (row: UserAdminRow) => {
    setResetTarget(row)
    resetForm.resetFields()
    setResetOpen(true)
  }

  const reload = useCallback(() => {
    actionRef.current?.reload()
  }, [])

  const applySearch = (value?: string) => {
    const q = (value ?? searchDraft).trim()
    searchQueryRef.current = q
    setSearch(q)
    setSearchDraft(q)
    reload()
  }

  const saveUser = async () => {
    const values = await form.validateFields()
    const payload: Record<string, unknown> = {
      username: values.username?.trim(),
      displayName: values.displayName?.trim() || null,
      enabled: values.enabled ?? 1,
      roleIds: values.roleIds ?? [],
    }
    if (!editing?.id) {
      payload.password = values.password
      await createUser(payload)
      message.success('User created')
    } else {
      await updateUser(editing.id, payload)
      message.success('User updated')
    }
    setModalOpen(false)
    reload()
  }

  const submitReset = async () => {
    const values = await resetForm.validateFields()
    if (!resetTarget?.id) return
    await resetUserPassword(resetTarget.id, values.password)
    message.success('Password reset')
    setResetOpen(false)
  }

  const columns: ProColumns<UserAdminRow>[] = [
    {
      title: 'User',
      dataIndex: 'username',
      width: 240,
      fixed: 'left',
      render: (_, row) => {
        const isSelf = row.username === currentUser
        return (
          <div className="fs-user-cell">
            <Avatar size={36} style={{ backgroundColor: avatarColor(row.username) }}>
              {userInitials(row.displayName, row.username)}
            </Avatar>
            <div className="fs-user-cell-body">
              <div className="fs-user-cell-name">
                <span>{row.displayName || row.username}</span>
                {isSelf && <Tag color="processing" className="fs-user-you-tag">You</Tag>}
              </div>
              <Typography.Text type="secondary" className="fs-user-cell-handle">
                @{row.username}
              </Typography.Text>
            </div>
          </div>
        )
      },
    },
    {
      title: 'Roles',
      dataIndex: 'roles',
      width: 200,
      render: (_, r) => (
        <Space size={[0, 4]} wrap>
          {(r.roles ?? []).map((code) => (
            <Tag key={code} color={ROLE_TAG_COLORS[code] ?? 'default'}>{code}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'enabled',
      width: 110,
      render: (_, r) => (
        r.enabled === 1
          ? <Badge status="success" text="Active" />
          : <Badge status="default" text="Disabled" />
      ),
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      width: 160,
      render: (_, r) => (
        <Typography.Text type="secondary" className="fs-user-ts">{formatTs(r.createdAt)}</Typography.Text>
      ),
    },
    {
      title: 'Updated',
      dataIndex: 'updatedAt',
      width: 160,
      render: (_, r) => (
        <Typography.Text type="secondary" className="fs-user-ts">{formatTs(r.updatedAt)}</Typography.Text>
      ),
    },
    {
      title: '',
      valueType: 'option',
      width: 72,
      fixed: 'right',
      render: (_, row) => {
        const isSelf = row.username === currentUser
        const items = [
          { key: 'edit', icon: <EditOutlined />, label: 'Edit profile & roles', onClick: () => openEdit(row) },
          { key: 'reset', icon: <KeyOutlined />, label: 'Reset password', onClick: () => openReset(row) },
          { type: 'divider' as const },
          {
            key: 'del',
            icon: <DeleteOutlined />,
            label: 'Delete account',
            danger: true,
            disabled: isSelf,
            onClick: () => {
              if (isSelf || !row.id) return
              Modal.confirm({
                title: `Delete ${row.username}?`,
                content: 'This removes the account and role assignments.',
                okText: 'Delete',
                okButtonProps: { danger: true },
                onOk: async () => {
                  await deleteUser(row.id!)
                  message.success('Deleted')
                  reload()
                },
              })
            },
          },
        ]
        return (
          <Dropdown menu={{ items }} trigger={['click']} placement="bottomRight">
            <Button type="text" size="small" icon={<MoreOutlined />} aria-label="User actions" />
          </Dropdown>
        )
      },
    },
  ]

  return (
    <DataPageLayout
      title="Users"
      subtitle="Accounts, roles, and access control"
      icon={<TeamOutlined />}
      className="fs-data-page--dense fs-data-page--fill fs-data-page--users"
      actions={(
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          Add user
        </Button>
      )}
      toolbar={(
        <div className="fs-users-toolbar">
          <div className="fs-users-stat">
            <UserOutlined className="fs-users-stat-icon" />
            <div>
              <div className="fs-users-stat-value">{stats.total}</div>
              <div className="fs-users-stat-label">Total</div>
            </div>
          </div>
          <div className="fs-users-stat fs-users-stat--active">
            <div className="fs-users-stat-dot" />
            <div>
              <div className="fs-users-stat-value">{stats.active}</div>
              <div className="fs-users-stat-label">Active</div>
            </div>
          </div>
          <div className="fs-users-stat fs-users-stat--admin">
            <div>
              <div className="fs-users-stat-value">{stats.admins}</div>
              <div className="fs-users-stat-label">Admins</div>
            </div>
          </div>
          <Input.Search
            allowClear
            className="fs-users-search"
            placeholder="Search username or display name"
            value={searchDraft}
            onChange={(e) => setSearchDraft(e.target.value)}
            onSearch={applySearch}
            onClear={() => {
              searchQueryRef.current = ''
              setSearchDraft('')
              setSearch('')
              reload()
            }}
            enterButton={<SearchOutlined />}
          />
        </div>
      )}
    >
      <div ref={tablePanelRef} className="fs-table-panel fs-users-table-panel">
        <ProTable<UserAdminRow>
          className="fs-data-table fs-users-table"
          actionRef={actionRef}
          rowKey="id"
          size="middle"
          scroll={{ x: 'max-content', y: tableHeight }}
          search={false}
          options={{ density: true, reload: true, setting: false }}
          locale={{ emptyText: <EmptyState compact title="No users" description="Try a different search or add a user." /> }}
          request={async () => {
            const data = await listUsers(searchQueryRef.current || search)
            setStats(computeStats(data))
            return { data, total: data.length, success: true }
          }}
          columns={columns}
          rowClassName={(row) => (row.username === currentUser ? 'fs-users-row-self' : '')}
          pagination={{ defaultPageSize: 20, showSizeChanger: true, showTotal: (t) => `${t} users` }}
        />
      </div>

      <Modal
        title={editing ? `Edit ${editing.username}` : 'Add user'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={saveUser}
        destroyOnClose
        width={480}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 8 }}>
          <Form.Item
            name="username"
            label="Username"
            rules={[
              { required: true },
              { pattern: /^[A-Za-z0-9._-]{2,64}$/, message: 'Letters, digits, . _ - only (2–64 chars)' },
            ]}
          >
            <Input disabled={!!editing?.id} autoComplete="off" />
          </Form.Item>
          <Form.Item name="displayName" label="Display name">
            <Input autoComplete="off" placeholder="Optional friendly name" />
          </Form.Item>
          {!editing?.id && (
            <Form.Item
              name="password"
              label="Initial password"
              rules={[
                { required: true },
                { min: 8, message: 'At least 8 characters' },
                { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: 'Must include a letter and a digit' },
              ]}
            >
              <Input.Password autoComplete="new-password" />
            </Form.Item>
          )}
          <Form.Item name="roleIds" label="Roles" rules={[{ required: true, message: 'Select at least one role' }]}>
            <Select mode="multiple" options={roleOptions} placeholder="ADMIN, USER, …" />
          </Form.Item>
          <Form.Item name="enabled" label="Account status" initialValue={1}>
            <Select options={[{ label: 'Active', value: 1 }, { label: 'Disabled', value: 0 }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={resetTarget ? `Reset password — ${resetTarget.username}` : 'Reset password'}
        open={resetOpen}
        onCancel={() => setResetOpen(false)}
        onOk={submitReset}
        destroyOnClose
        okText="Reset"
        okButtonProps={{ icon: <KeyOutlined /> }}
      >
        <Form form={resetForm} layout="vertical">
          <Form.Item
            name="password"
            label="New password"
            rules={[
              { required: true },
              { min: 8 },
              { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: 'Must include a letter and a digit' },
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
        </Form>
      </Modal>
    </DataPageLayout>
  )
}
