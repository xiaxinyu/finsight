import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { ConfigProvider, App as AntApp } from 'antd'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useEffect } from 'react'
import { finsightTheme } from './styles/finsight-tokens'
import { AppLayout } from './layouts/AppLayout'
import { LoginPage } from './pages/Login'
import { DashboardPage } from './pages/Dashboard'
import { TransactionsPage } from './pages/Transactions'
import { StatementUploadPage } from './pages/Statements/Upload'
import { StatementListPage } from './pages/Statements/List'
import { ReportPageView } from './pages/Reports/ReportPageView'
import { LedgerPage } from './pages/Ledgers/LedgerPage'
import { UsersAdminPage } from './pages/Admin/Users'
import { CardsAdminPage } from './pages/Admin/Cards'
import { RulesAdminPage } from './pages/Admin/Rules'
import { CategoriesAdminPage } from './pages/Admin/Categories'
import { setUnauthorizedHandler } from './api/client'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})

function AuthBridge() {
  const navigate = useNavigate()
  useEffect(() => {
    setUnauthorizedHandler(() => navigate('/login', { replace: true }))
  }, [navigate])
  return null
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider theme={finsightTheme}>
        <AntApp>
          <BrowserRouter basename="/app">
            <AuthBridge />
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<AppLayout />}>
                <Route index element={<Navigate to="/dashboard" replace />} />
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/transactions" element={<TransactionsPage />} />
                <Route path="/statements/upload" element={<StatementUploadPage />} />
                <Route path="/statements" element={<StatementListPage />} />
                <Route path="/reports/:reportId" element={<ReportPageView />} />
                <Route path="/ledgers/:ledgerId" element={<LedgerPage />} />
                <Route path="/admin/users" element={<UsersAdminPage />} />
                <Route path="/admin/cards" element={<CardsAdminPage />} />
                <Route path="/admin/rules" element={<RulesAdminPage />} />
                <Route path="/admin/categories" element={<CategoriesAdminPage />} />
              </Route>
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </BrowserRouter>
        </AntApp>
      </ConfigProvider>
    </QueryClientProvider>
  )
}
