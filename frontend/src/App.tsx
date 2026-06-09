import { BrowserRouter, useNavigate } from 'react-router-dom'
import { ConfigProvider, App as AntApp } from 'antd'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useEffect } from 'react'
import { finsightTheme } from './styles/finsight-tokens'
import { AppRoutes } from './routes/AppRoutes'
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
            <AppRoutes />
          </BrowserRouter>
        </AntApp>
      </ConfigProvider>
    </QueryClientProvider>
  )
}
