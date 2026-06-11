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
  SwapOutlined,
  TeamOutlined,
  ThunderboltOutlined,
  UploadOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'

export type FsMenuItem = {
  key: string
  label: ReactNode
  icon?: ReactNode
  path?: string
  children?: FsMenuItem[]
}

export const menuItems: FsMenuItem[] = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard', path: '/dashboard' },
  { key: '/planning', icon: <FundOutlined />, label: 'Planning', path: '/planning' },
  { key: '/wealth', icon: <BankOutlined />, label: 'Wealth', path: '/wealth' },
  { key: '/goals', icon: <AimOutlined />, label: 'Goals', path: '/goals' },
  {
    key: 'transactions',
    icon: <FileTextOutlined />,
    label: 'Transactions',
    children: [
      { key: '/transactions', icon: <FileTextOutlined />, label: 'Detail', path: '/transactions' },
      { key: '/statements/upload', icon: <UploadOutlined />, label: 'Import', path: '/statements/upload' },
      { key: '/statements', icon: <HistoryOutlined />, label: 'Import History', path: '/statements' },
    ],
  },
  {
    key: 'reports',
    icon: <BarChartOutlined />,
    label: 'Reports',
    children: [
      { key: '/reports/cashflow', icon: <SwapOutlined />, label: 'Cashflow', path: '/reports/cashflow' },
      { key: '/reports/fund-flow', icon: <SwapOutlined />, label: 'Fund Flow', path: '/reports/fund-flow' },
      { key: '/reports/budget-vs-actual', icon: <PieChartOutlined />, label: 'Budget vs Actual', path: '/reports/budget-vs-actual' },
      { key: '/reports/fixed-vs-variable', icon: <CalendarOutlined />, label: 'Fixed vs Variable', path: '/reports/fixed-vs-variable' },
      { key: '/reports/spending-drift', icon: <RiseOutlined />, label: 'Spending Drift', path: '/reports/spending-drift' },
      { key: '/reports/bills-calendar', icon: <LineChartOutlined />, label: 'Bills Calendar', path: '/reports/bills-calendar' },
    ],
  },
  {
    key: 'income',
    icon: <WalletOutlined />,
    label: 'Income',
    children: [
      { key: '/ledgers/salary', icon: <BookOutlined />, label: 'Ledger', path: '/ledgers/salary' },
    ],
  },
  {
    key: 'expense',
    icon: <FundOutlined />,
    label: 'Expense',
    children: [
      { key: '/ledgers/expense', icon: <BookOutlined />, label: 'Ledger', path: '/ledgers/expense' },
      { key: '/ledgers/house-rent', icon: <HomeOutlined />, label: 'Rent Ledger', path: '/ledgers/house-rent' },
    ],
  },
  {
    key: 'benefit',
    icon: <BankOutlined />,
    label: 'Benefits',
    children: [
      { key: '/ledgers/endowment', icon: <BankOutlined />, label: 'Pension', path: '/ledgers/endowment' },
      { key: '/ledgers/accumulation', icon: <WalletOutlined />, label: 'Provident Fund', path: '/ledgers/accumulation' },
      { key: '/ledgers/medical', icon: <MedicineBoxOutlined />, label: 'Medical', path: '/ledgers/medical' },
      { key: '/ledgers/unemployment', icon: <ClusterOutlined />, label: 'Unemployment', path: '/ledgers/unemployment' },
    ],
  },
  {
    key: 'admin',
    icon: <SettingOutlined />,
    label: 'Admin',
    children: [
      { key: '/admin/users', icon: <TeamOutlined />, label: 'Users', path: '/admin/users' },
      { key: '/admin/cards', icon: <CreditCardOutlined />, label: 'Cards', path: '/admin/cards' },
      { key: '/admin/rules', icon: <ThunderboltOutlined />, label: 'Rule Engine', path: '/admin/rules' },
      { key: '/admin/categories', icon: <ClusterOutlined />, label: 'Categories', path: '/admin/categories' },
    ],
  },
]
