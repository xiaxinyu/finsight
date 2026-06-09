import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { Layout, Menu, Typography, Button, theme, type MenuProps } from 'antd'
import { LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons'
import { menuItems, type FsMenuItem } from '../routes/menuConfig'

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
  const location = useLocation()
  const { token } = theme.useToken()

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} trigger={null} width={240} theme="dark">
        <div style={{ height: 56, display: 'flex', alignItems: 'center', padding: '0 16px', color: '#fff', fontWeight: 800, fontSize: 18 }}>
          {collapsed ? 'FS' : 'FinSight'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={findSelectedKeys(location.pathname)}
          defaultOpenKeys={findOpenKeys(location.pathname)}
          items={renderMenuItems(menuItems)}
          style={{ borderRight: 0 }}
        />
      </Sider>
      <Layout>
        <Header style={{ background: token.colorBgContainer, padding: '0 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: `1px solid ${token.colorBorderSecondary}` }}>
          <Button type="text" icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />} onClick={() => setCollapsed(!collapsed)} />
          <Typography.Text type="secondary">Personal Finance Intelligence</Typography.Text>
          <Button type="text" icon={<LogoutOutlined />} href="/logout">Logout</Button>
        </Header>
        <Content style={{ margin: 16, padding: 20, background: token.colorBgContainer, borderRadius: token.borderRadiusLG, minHeight: 280 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
