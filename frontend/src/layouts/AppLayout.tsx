import { useEffect, useMemo, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { verifySession } from '../api/client'
import { Layout, Menu, Typography, Button, Breadcrumb, Popconfirm, theme, type MenuProps } from 'antd'
import { LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons'
import {
  menuItems,
  menuOpenKeysForPath,
  reconcileMenuOpenKeys,
  type FsMenuItem,
} from '../config/menuConfig'
import { useFeatureFlags } from '../hooks/useFeatureFlags'
import { useAuth } from '../hooks/useAuth'
import { filterMenuByFeatures } from '../utils/featureFlags'
import { filterMenuByRole } from '../utils/menuRoleFilter'
import { BrandLogo } from '../components/BrandLogo'
import { resolveRouteMeta } from '../config/routes'

const { Header, Sider, Content } = Layout

function findSelectedKeys(pathname: string): string[] {
  if (pathname.startsWith('/reports/')) return [pathname]
  if (pathname.startsWith('/ledgers/')) return [pathname]
  if (pathname.startsWith('/admin/')) return [pathname]
  if (pathname.startsWith('/statements')) return [pathname === '/statements/upload' ? '/statements/upload' : '/statements']
  return [pathname]
}

function renderMenuItems(items: FsMenuItem[]): MenuProps['items'] {
  return items.map((item) => {
    if (item.type === 'group') {
      return {
        type: 'group',
        key: item.key,
        label: item.label,
        children: renderMenuItems(item.children ?? []),
      }
    }
    if (item.children?.length) {
      return { key: item.key, icon: item.icon, label: item.label, children: renderMenuItems(item.children) }
    }
    if (item.path) {
      return { key: item.key, icon: item.icon, label: <Link to={item.path}>{item.label}</Link> }
    }
    return { key: item.key, icon: item.icon, label: item.label }
  })
}

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const { token } = theme.useToken()
  const routeMeta = resolveRouteMeta(location.pathname)
  const { flags } = useFeatureFlags()
  const { isAdmin } = useAuth()
  const visibleMenu = useMemo(
    () => filterMenuByRole(filterMenuByFeatures(menuItems, flags), isAdmin),
    [flags, isAdmin],
  )
  const [openKeys, setOpenKeys] = useState<string[]>(() => menuOpenKeysForPath(location.pathname))

  useEffect(() => {
    verifySession().then((ok) => {
      if (!ok) navigate('/login', { replace: true })
    })
  }, [navigate])

  useEffect(() => {
    setOpenKeys((prev) => {
      const routeKeys = menuOpenKeysForPath(location.pathname)
      return [...new Set([...routeKeys, ...prev.filter((k) => routeKeys.some((r) => k.startsWith(r) || r.startsWith(k)))])]
    })
  }, [location.pathname])

  const breadcrumbItems = routeMeta.breadcrumb.map((label, i) => ({
    key: String(i),
    title: i === routeMeta.breadcrumb.length - 1
      ? <Typography.Text strong style={{ fontSize: 13 }}>{label}</Typography.Text>
      : <span style={{ fontSize: 13 }}>{label}</span>,
  }))

  const onOpenChange: MenuProps['onOpenChange'] = (keys) => {
    setOpenKeys((prev) => reconcileMenuOpenKeys(prev, keys))
  }

  return (
    <Layout style={{ height: '100vh', overflow: 'hidden' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        trigger={null}
        width={220}
        theme="dark"
        className="fs-app-sider"
      >
        <BrandLogo collapsed={collapsed} variant="dark" />
        <div className="fs-sider-menu-scroll">
          <Menu
            className="fs-sider-menu"
            theme="dark"
            mode="inline"
            selectedKeys={findSelectedKeys(location.pathname)}
            openKeys={collapsed ? [] : openKeys}
            onOpenChange={onOpenChange}
            items={renderMenuItems(visibleMenu)}
            style={{ borderRight: 0 }}
          />
        </div>
      </Sider>
      <Layout style={{ minHeight: 0 }}>
        <Header
          className="fs-app-header"
          style={{
            background: token.colorBgContainer,
            borderBottom: `1px solid ${token.colorBorder}`,
          }}
        >
          <div className="fs-app-header-left">
            <Button
              type="text"
              size="small"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
            />
            <Breadcrumb items={breadcrumbItems} />
          </div>
          <Typography.Text type="secondary" className="fs-app-tagline">Personal Finance Intelligence</Typography.Text>
          <div className="fs-app-header-right">
            <Popconfirm title="Sign out of FinSight?" okText="Sign out" cancelText="Cancel" onConfirm={() => { window.location.href = '/logout' }}>
              <Button type="text" size="small" icon={<LogoutOutlined />}>Sign out</Button>
            </Popconfirm>
          </div>
        </Header>
        <Content className="fs-app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
