import { useEffect, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { verifySession } from '../api/client'
import { Layout, Menu, Typography, Button, Breadcrumb, Popconfirm, theme, type MenuProps } from 'antd'
import { LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons'
import { menuItems, type FsMenuItem } from '../config/menuConfig'
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

function findOpenKeys(pathname: string): string[] {
  if (pathname.startsWith('/reports')) {
    if (pathname.includes('income-curve')) return ['income']
    if (pathname.includes('expense-curve')) return ['expense']
    return ['reports']
  }
  if (pathname.startsWith('/ledgers')) {
    if (pathname.includes('salary') || pathname.includes('income-curve')) return ['income']
    if (pathname.includes('expense') || pathname.includes('house-rent') || pathname.includes('expense-curve')) return ['expense']
    return ['benefit']
  }
  if (pathname.startsWith('/admin')) return ['admin']
  if (pathname.startsWith('/transactions') || pathname.startsWith('/statements')) return ['transactions']
  if (pathname === '/planning' || pathname === '/wealth' || pathname === '/goals') return []
  return []
}

function renderMenuItems(items: FsMenuItem[]): MenuProps['items'] {
  return items.map((item) => {
    if (item.children) {
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
  const [openKeys, setOpenKeys] = useState<string[]>([])
  const location = useLocation()
  const navigate = useNavigate()
  const { token } = theme.useToken()
  const routeMeta = resolveRouteMeta(location.pathname)

  useEffect(() => {
    verifySession().then((ok) => {
      if (!ok) navigate('/login', { replace: true })
    })
  }, [navigate])

  useEffect(() => {
    setOpenKeys(findOpenKeys(location.pathname))
  }, [location.pathname])

  const breadcrumbItems = routeMeta.breadcrumb.map((label, i) => ({
    key: String(i),
    title: i === routeMeta.breadcrumb.length - 1
      ? <Typography.Text strong style={{ fontSize: 13 }}>{label}</Typography.Text>
      : <span style={{ fontSize: 13 }}>{label}</span>,
  }))

  return (
    <Layout style={{ height: '100vh', overflow: 'hidden' }}>
      <Sider collapsible collapsed={collapsed} trigger={null} width={200} theme="dark">
        <BrandLogo collapsed={collapsed} variant="dark" />
        <Menu
          className="fs-sider-menu"
          theme="dark"
          mode="inline"
          selectedKeys={findSelectedKeys(location.pathname)}
          openKeys={openKeys}
          onOpenChange={setOpenKeys}
          items={renderMenuItems(menuItems)}
          style={{ borderRight: 0 }}
        />
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
