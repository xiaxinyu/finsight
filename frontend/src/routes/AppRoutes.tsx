import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from '../layouts/AppLayout'
import { LoginPage } from '../pages/Login'
import { DashboardPage } from '../pages/Dashboard'
import { TransactionsPage } from '../pages/Transactions'
import { StatementUploadPage } from '../pages/Statements/Upload'
import { StatementListPage } from '../pages/Statements'
import { ReportRoute } from './ReportRoute'
import { LedgersPage } from '../pages/Ledgers'
import { PlanningPage } from '../pages/Planning'
import { WealthPage } from '../pages/Wealth'
import { GoalsPage } from '../pages/Goals'
import { LoansPage } from '../pages/Loans'
import { AccountSecurityPage } from '../pages/Account'
import { ProfilePage } from '../pages/Profile'
import { UsersAdminPage } from '../pages/Admin/Users'
import { CardsAdminPage } from '../pages/Admin/Cards'
import { RulesAdminPage } from '../pages/Admin/Rules'
import { CategoriesAdminPage } from '../pages/Admin/Categories'
import { AdminRoute } from './AdminRoute'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/planning" element={<PlanningPage />} />
        <Route path="/wealth" element={<WealthPage />} />
        <Route path="/goals" element={<GoalsPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/settings/account" element={<AccountSecurityPage />} />
        <Route path="/transactions" element={<TransactionsPage />} />
        <Route path="/statements/upload" element={<StatementUploadPage />} />
        <Route path="/statements" element={<StatementListPage />} />
        <Route path="/reports/:reportId" element={<ReportRoute />} />
        <Route path="/ledgers/loans" element={<LoansPage />} />
        <Route path="/ledgers/:ledgerId" element={<LedgersPage />} />
        <Route path="/loans" element={<Navigate to="/ledgers/loans" replace />} />
        <Route element={<AdminRoute />}>
          <Route path="/admin/users" element={<UsersAdminPage />} />
          <Route path="/admin/cards" element={<CardsAdminPage />} />
          <Route path="/admin/rules" element={<RulesAdminPage />} />
          <Route path="/admin/categories" element={<CategoriesAdminPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
