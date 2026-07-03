import {
  AimOutlined,
  BankOutlined,
  DollarOutlined,
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
  LockOutlined,
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
import { reportConfigs } from './reports'
import {
  REPORT_MENU_LABELS,
  REPORT_NAV_GROUPS,
  menuOpenKeysForReportId,
  type ReportNavGroupKey,
} from './reportNavigation'

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

const REPORT_ICONS: Record<string, ReactNode> = {
  cashflow: <SwapOutlined />,
  'budget-vs-actual': <PieChartOutlined />,
  'bills-calendar': <CalendarOutlined />,
  'income-trends': <WalletOutlined />,
  'trend-changes': <LineChartOutlined />,
  'debt-trends': <CreditCardOutlined />,
  'fixed-vs-variable': <CalendarOutlined />,
  'spending-drift': <SwapOutlined />,
  'fund-flow': <SwapOutlined />,
  'transfer-finance': <BankOutlined />,
  'tax-summary': <FundOutlined />,
  'annual-outlook': <ThunderboltOutlined />,
  'cash-risk': <FundOutlined />,
  subscriptions: <ClusterOutlined />,
  'merchant-concentration': <ShopOutlined />,
  'merchant-drift': <RiseOutlined />,
}

const GROUP_ICONS: Partial<Record<ReportNavGroupKey, ReactNode>> = {
  'reports-monthly': <DollarOutlined />,
  'reports-yoy': <LineChartOutlined />,
  'reports-spending': <PieChartOutlined />,
  'reports-capital': <BankOutlined />,
  'reports-forecast': <ThunderboltOutlined />,
  'reports-merchants': <ShopOutlined />,
}

function reportMenuItem(reportId: string): FsMenuItem {
  const cfg = reportConfigs[reportId]
  const label = REPORT_MENU_LABELS[reportId] ?? cfg?.title ?? reportId
  return {
    key: `/reports/${reportId}`,
    icon: REPORT_ICONS[reportId],
    label,
    path: `/reports/${reportId}`,
  }
}

function buildReportsMenu(): FsMenuItem {
  return {
    key: 'reports',
    icon: <BarChartOutlined />,
    label: 'Reports',
    children: REPORT_NAV_GROUPS.map((group) => ({
      key: group.key,
      icon: GROUP_ICONS[group.key],
      label: group.label,
      children: group.reportIds.map(reportMenuItem),
    })),
  }
}

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
    children: [buildReportsMenu()],
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
          { key: '/ledgers/loans', icon: <DollarOutlined />, label: 'Loans', path: '/ledgers/loans' },
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
      { key: '/profile', icon: <UserOutlined />, label: 'Financial profile', path: '/profile' },
      { key: '/settings/account', icon: <LockOutlined />, label: 'Account & security', path: '/settings/account' },
      {
        key: 'admin',
        icon: <SettingOutlined />,
        label: 'Admin',
        children: [
          { key: '/admin/users', icon: <TeamOutlined />, label: 'Users', path: '/admin/users' },
          { key: '/admin/cards', icon: <CreditCardOutlined />, label: 'Bank cards', path: '/admin/cards' },
          { key: '/admin/rules', icon: <ThunderboltOutlined />, label: 'Rules', path: '/admin/rules' },
          { key: '/admin/categories', icon: <ClusterOutlined />, label: 'Categories', path: '/admin/categories' },
        ],
      },
    ],
  },
]

/** Open keys for the current route so the active item stays visible. */
export function menuOpenKeysForPath(pathname: string): string[] {
  if (pathname.startsWith('/transactions') || pathname.startsWith('/statements')) {
    return ['transactions']
  }

  if (pathname.startsWith('/reports/')) {
    const id = pathname.replace('/reports/', '').split('/')[0]
    return menuOpenKeysForReportId(id)
  }

  if (pathname.startsWith('/ledgers/')) {
    const keys: string[] = ['ledgers']
    const id = pathname.replace('/ledgers/', '')
    if (['endowment', 'accumulation', 'medical', 'unemployment'].includes(id)) {
      keys.push('ledgers-benefits')
    }
    return keys
  }

  if (pathname.startsWith('/admin')) {
    return ['admin']
  }

  return []
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
