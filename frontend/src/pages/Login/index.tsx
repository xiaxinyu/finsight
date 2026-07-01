import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { BarChartOutlined, LockOutlined, SafetyOutlined, UserOutlined } from '@ant-design/icons'
import { getJson, postLoginForm, verifySession } from '../../api/client'
import { fetchCsrfToken } from '../../api/auth'
import { BrandLogo } from '../../components/BrandLogo'

export function LoginPage() {
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    fetchCsrfToken().catch(() => {})
    getJson<{ code?: string; msg?: string }>('/login-error.json')
      .then((d) => { if (d?.msg) setError(d.msg) })
      .catch(() => {})
  }, [])

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true)
    setError('')
    try {
      await fetchCsrfToken().catch(() => null)
      await postLoginForm(values.username, values.password)
      if (await verifySession()) {
        navigate('/dashboard', { replace: true })
        return
      }
      const err = await getJson<{ msg?: string }>('/login-error.json').catch(() => null)
      setError(err?.msg || 'Invalid username or password')
    } catch {
      setError('Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fs-login-page">
      <div className="fs-login-brand">
        <BrandLogo variant="dark" />
        <Typography.Title level={2} style={{ color: '#fff', marginTop: 32, marginBottom: 8 }}>
          Personal Finance Intelligence
        </Typography.Title>
        <Typography.Paragraph style={{ color: '#bfdbfe', marginBottom: 32 }}>
          Local-first insights for income, spending, and long-term trends.
        </Typography.Paragraph>
        <div className="fs-login-value-prop">
          <SafetyOutlined className="fs-login-value-icon" />
          <span>Your data stays on your infrastructure — session-based, secure access.</span>
        </div>
        <div className="fs-login-value-prop">
          <BarChartOutlined className="fs-login-value-icon" />
          <span>Professional reports, ledgers, and category breakdowns at a glance.</span>
        </div>
        <div className="fs-login-value-prop">
          <UserOutlined className="fs-login-value-icon" />
          <span>Import statements, classify transactions, and track savings over time.</span>
        </div>
      </div>
      <div className="fs-login-form-wrap">
        <Card className="fs-login-card">
          <Typography.Title level={3} style={{ textAlign: 'center', marginBottom: 4 }}>Welcome back</Typography.Title>
          <Typography.Paragraph type="secondary" style={{ textAlign: 'center', marginBottom: 20 }}>Sign in to FinSight</Typography.Paragraph>
          {error && <Alert type="error" message={error} style={{ marginBottom: 16 }} />}
          <Form layout="vertical" onFinish={onFinish}>
            <Form.Item name="username" label="Username" rules={[{ required: true }]}>
              <Input prefix={<UserOutlined />} autoComplete="username" placeholder="Username" />
            </Form.Item>
            <Form.Item name="password" label="Password" rules={[{ required: true }]}>
              <Input.Password prefix={<LockOutlined />} autoComplete="current-password" placeholder="Password" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading} size="large">Sign in</Button>
          </Form>
        </Card>
      </div>
    </div>
  )
}
