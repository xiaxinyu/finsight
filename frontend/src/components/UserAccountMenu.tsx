import { Link } from 'react-router-dom'
import { Avatar, Dropdown, Space, Tag, Typography, type MenuProps } from 'antd'
import { LockOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons'
import { useAuth } from '../hooks/useAuth'
import { avatarColor, userInitials, ROLE_TAG_COLORS } from '../utils/userDisplay'

export function UserAccountMenu() {
  const { username, displayName, roles, isAdmin } = useAuth()
  const label = displayName || username || 'User'
  const initials = userInitials(displayName, username)
  const color = avatarColor(username)

  const menuItems: MenuProps['items'] = [
    {
      key: 'profile-header',
      type: 'group',
      label: (
        <div className="fs-account-menu-header">
          <Avatar size={40} style={{ backgroundColor: color, flexShrink: 0 }}>
            {initials}
          </Avatar>
          <div className="fs-account-menu-meta">
            <Typography.Text strong className="fs-account-menu-name">{label}</Typography.Text>
            {username && (
              <Typography.Text type="secondary" className="fs-account-menu-handle">
                @{username}
              </Typography.Text>
            )}
            {(roles?.length || isAdmin) && (
              <Space size={[0, 4]} wrap className="fs-account-menu-roles">
                {(roles ?? (isAdmin ? ['ADMIN'] : [])).map((code) => (
                  <Tag key={code} color={ROLE_TAG_COLORS[code] ?? 'default'} bordered={false}>
                    {code}
                  </Tag>
                ))}
              </Space>
            )}
          </div>
        </div>
      ),
    },
    { type: 'divider' },
    {
      key: 'account',
      icon: <LockOutlined />,
      label: <Link to="/settings/account">Account & security</Link>,
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Sign out',
      danger: true,
      onClick: () => { window.location.href = '/logout' },
    },
  ]

  return (
    <Dropdown
      menu={{ items: menuItems }}
      trigger={['click']}
      placement="bottomRight"
      overlayClassName="fs-account-menu-dropdown"
    >
      <button type="button" className="fs-account-menu-trigger" aria-label="Account menu">
        <Avatar size={28} style={{ backgroundColor: color }}>
          {initials}
        </Avatar>
        <span className="fs-account-menu-trigger-text">
          <span className="fs-account-menu-trigger-name">{label}</span>
          {username && <span className="fs-account-menu-trigger-user">@{username}</span>}
        </span>
        <UserOutlined className="fs-account-menu-chevron" />
      </button>
    </Dropdown>
  )
}
