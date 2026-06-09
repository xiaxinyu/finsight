import {
  AreaChartOutlined,
  BankOutlined,
  BarChartOutlined,
  BookOutlined,
  CalendarOutlined,
  ClusterOutlined,
  CreditCardOutlined,
  DashboardOutlined,
  FallOutlined,
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
  TagsOutlined,
  TeamOutlined,
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
      { key: '/reports/transaction-trend', icon: <RiseOutlined />, label: 'Transaction Trend', path: '/reports/transaction-trend' },
      { key: '/reports/category-breakdown', icon: <PieChartOutlined />, label: 'Category Breakdown', path: '/reports/category-breakdown' },
      { key: '/reports/category-comparison', icon: <SwapOutlined />, label: 'Category Comparison', path: '/reports/category-comparison' },
      { key: '/reports/weekly-summary', icon: <CalendarOutlined />, label: 'Weekly Summary', path: '/reports/weekly-summary' },
      { key: '/reports/monthly-comparison', icon: <AreaChartOutlined />, label: 'Monthly Comparison', path: '/reports/monthly-comparison' },
      { key: '/reports/income-vs-expense', icon: <FundOutlined />, label: 'Income vs Expense', path: '/reports/income-vs-expense' },
    ],
  },
  {
    key: 'income',
    icon: <WalletOutlined />,
    label: 'Income',
    children: [
      { key: '/ledgers/salary', icon: <BookOutlined />, label: 'Ledger', path: '/ledgers/salary' },
      { key: '/reports/income-curve', icon: <LineChartOutlined />, label: 'Income Curve', path: '/reports/income-curve' },
    ],
  },
  {
    key: 'expense',
    icon: <FundOutlined />,
    label: 'Expense',
    children: [
      { key: '/ledgers/expense', icon: <BookOutlined />, label: 'Ledger', path: '/ledgers/expense' },
      { key: '/reports/expense-curve', icon: <FallOutlined />, label: 'Expense Curve', path: '/reports/expense-curve' },
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
      { key: '/admin/rules', icon: <TagsOutlined />, label: 'Category Rules', path: '/admin/rules' },
      { key: '/admin/categories', icon: <ClusterOutlined />, label: 'Categories', path: '/admin/categories' },
    ],
  },
]
