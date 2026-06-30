import {
  AimOutlined,
  BankOutlined,
  BarChartOutlined,
  BookOutlined,
  CalendarOutlined,
  ClusterOutlined,
  CreditCardOutlined,
  DashboardOutlined,
  FileTextOutlined,
  FundOutlined,
  HistoryOutlined,
  HomeOutlined,
  LineChartOutlined,
  MedicineBoxOutlined,
  PieChartOutlined,
  RiseOutlined,
  SettingOutlined,
  ShopOutlined,
  SwapOutlined,
  TeamOutlined,
  ThunderboltOutlined,
  UploadOutlined,
  UserOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'

export type FsMenuItem = {
  key: string
  label: ReactNode
  icon?: ReactNode
  path?: string
  children?: FsMenuItem[]
  /** Ant Design menu section label — not clickable. */
  type?: 'group'
}

/** Root submenus that participate in sidebar accordion (one open at a time). */
export const menuAccordionRoots = ['transactions', 'reports', 'ledgers', 'admin'] as const

export const menuItems: FsMenuItem[] = [
  {
    type: 'group',
    key: 'grp-overview',
    label: 'Overview',
    children: [
      { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard', path: '/dashboard' },
      { key: '/planning', icon: <FundOutlined />, label: 'Planning', path: '/planning' },
      { key: '/wealth', icon: <BankOutlined />, label: 'Wealth', path: '/wealth' },
      { key: '/goals', icon: <AimOutlined />, label: 'Goals', path: '/goals' },
    ],
  },
  {
    type: 'group',
    key: 'grp-activity',
    label: 'Activity',
    children: [
      {
        key: 'transactions',
        icon: <FileTextOutlined />,
        label: 'Transactions',
        children: [
          { key: '/transactions', label: 'All transactions', path: '/transactions' },
          { key: '/statements/upload', icon: <UploadOutlined />, label: 'Import', path: '/statements/upload' },
          { key: '/statements', icon: <HistoryOutlined />, label: 'Import history', path: '/statements' },
        ],
      },
    ],
  },
  {
    type: 'group',
    key: 'grp-insights',
    label: 'Insights',
    children: [
      {
        key: 'reports',
        icon: <BarChartOutlined />,
        label: 'Reports',
        children: [
          {
            key: 'reports-cashflow',
            label: 'Cashflow & budget',
            children: [
              { key: '/reports/cashflow', icon: <SwapOutlined />, label: 'Cashflow', path: '/reports/cashflow' },
              { key: '/reports/budget-vs-actual', icon: <PieChartOutlined />, label: 'Budget vs Actual', path: '/reports/budget-vs-actual' },
              { key: '/reports/fund-flow', icon: <SwapOutlined />, label: 'Fund flow', path: '/reports/fund-flow' },
              { key: '/reports/transfer-finance', icon: <BankOutlined />, label: 'Transfer & Finance', path: '/reports/transfer-finance' },
              { key: '/reports/tax-summary', icon: <FundOutlined />, label: 'Tax summary', path: '/reports/tax-summary' },
            ],
          },
          {
            key: 'reports-spending',
            label: 'Spending',
            children: [
              { key: '/reports/fixed-vs-variable', icon: <CalendarOutlined />, label: 'Fixed vs Variable', path: '/reports/fixed-vs-variable' },
              { key: '/reports/spending-drift', icon: <RiseOutlined />, label: 'Spending drift', path: '/reports/spending-drift' },
              { key: '/reports/trend-changes', icon: <RiseOutlined />, label: 'Trend changes', path: '/reports/trend-changes' },
            ],
          },
          {
            key: 'reports-outlook',
            label: 'Cash & outlook',
            children: [
              { key: '/reports/bills-calendar', icon: <LineChartOutlined />, label: 'Bills calendar', path: '/reports/bills-calendar' },
              { key: '/reports/annual-outlook', icon: <ThunderboltOutlined />, label: 'Annual outlook', path: '/reports/annual-outlook' },
              { key: '/reports/cash-risk', icon: <FundOutlined />, label: 'Cash risk', path: '/reports/cash-risk' },
            ],
          },
          {
            key: 'reports-merchants',
            label: 'Merchants',
            children: [
              { key: '/reports/subscriptions', icon: <ClusterOutlined />, label: 'Subscriptions', path: '/reports/subscriptions' },
              { key: '/reports/merchant-concentration', icon: <ShopOutlined />, label: 'Concentration', path: '/reports/merchant-concentration' },
              { key: '/reports/merchant-drift', icon: <RiseOutlined />, label: 'Drift', path: '/reports/merchant-drift' },
            ],
          },
        ],
      },
    ],
  },
  {
    type: 'group',
    key: 'grp-ledgers',
    label: 'Ledgers',
    children: [
      {
        key: 'ledgers',
        icon: <BookOutlined />,
        label: 'Special ledgers',
        children: [
          { key: '/ledgers/salary', icon: <WalletOutlined />, label: 'Income', path: '/ledgers/salary' },
          { key: '/ledgers/expense', icon: <FundOutlined />, label: 'Expense', path: '/ledgers/expense' },
          { key: '/ledgers/house-rent', icon: <HomeOutlined />, label: 'Rent', path: '/ledgers/house-rent' },
          {
            key: 'ledgers-benefits',
            icon: <BankOutlined />,
            label: 'Benefits',
            children: [
              { key: '/ledgers/endowment', label: 'Pension', path: '/ledgers/endowment' },
              { key: '/ledgers/accumulation', label: 'Provident fund', path: '/ledgers/accumulation' },
              { key: '/ledgers/medical', icon: <MedicineBoxOutlined />, label: 'Medical', path: '/ledgers/medical' },
              { key: '/ledgers/unemployment', label: 'Unemployment', path: '/ledgers/unemployment' },
            ],
          },
        ],
      },
    ],
  },
  {
    type: 'group',
    key: 'grp-settings',
    label: 'Settings',
    children: [
      { key: '/profile', icon: <UserOutlined />, label: 'Profile', path: '/profile' },
      {
        key: 'admin',
        icon: <SettingOutlined />,
        label: 'Admin',
        children: [
          { key: '/admin/users', icon: <TeamOutlined />, label: 'Users', path: '/admin/users' },
          { key: '/admin/cards', icon: <CreditCardOutlined />, label: 'Cards', path: '/admin/cards' },
          { key: '/admin/rules', icon: <ThunderboltOutlined />, label: 'Rule engine', path: '/admin/rules' },
          { key: '/admin/categories', icon: <ClusterOutlined />, label: 'Categories', path: '/admin/categories' },
        ],
      },
    ],
  },
]

/** Open keys for the current route so the active item stays visible. */
export function menuOpenKeysForPath(pathname: string): string[] {
  const keys: string[] = []

  if (pathname.startsWith('/transactions') || pathname.startsWith('/statements')) {
    keys.push('transactions')
    return keys
  }

  if (pathname.startsWith('/reports/')) {
    keys.push('reports')
    const id = pathname.replace('/reports/', '')
    if (['cashflow', 'budget-vs-actual', 'fund-flow', 'transfer-finance', 'tax-summary'].includes(id)) keys.push('reports-cashflow')
    else if (['fixed-vs-variable', 'spending-drift', 'trend-changes'].includes(id)) keys.push('reports-spending')
    else if (['bills-calendar', 'annual-outlook', 'cash-risk'].includes(id)) keys.push('reports-outlook')
    else if (['subscriptions', 'merchant-concentration', 'merchant-drift'].includes(id)) keys.push('reports-merchants')
    return keys
  }

  if (pathname.startsWith('/ledgers/')) {
    keys.push('ledgers')
    const id = pathname.replace('/ledgers/', '')
    if (['endowment', 'accumulation', 'medical', 'unemployment'].includes(id)) {
      keys.push('ledgers-benefits')
    }
    return keys
  }

  if (pathname.startsWith('/admin')) {
    keys.push('admin')
  }

  return keys
}

/** Keep one root accordion section open; preserve nested keys under that root. */
export function reconcileMenuOpenKeys(prev: string[], next: string[]): string[] {
  const added = next.filter((k) => !prev.includes(k))
  const rootAdded = added.find((k) => (menuAccordionRoots as readonly string[]).includes(k))
  if (!rootAdded) return next

  return next.filter((k) => {
    if ((menuAccordionRoots as readonly string[]).includes(k)) return k === rootAdded
    if (rootAdded === 'reports') return k.startsWith('reports')
    if (rootAdded === 'ledgers') return k.startsWith('ledgers')
    return false
  })
}
