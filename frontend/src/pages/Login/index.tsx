import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { getJson, verifySession } from '../../api/client'

export function LoginPage() {
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    getJson<{ code?: string; msg?: string }>('/login-error.json')
      .then((d) => { if (d?.msg) setError(d.msg) })
      .catch(() => {})
  }, [])

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true)
    setError('')
    const body = new URLSearchParams()
    body.append('username', values.username)
    body.append('password', values.password)
    try {
      await fetch('/authentication/form', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body.toString(),
        redirect: 'follow',
      })
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
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%)' }}>
      <Card style={{ width: 400, boxShadow: '0 8px 32px rgba(15,23,42,0.08)' }}>
        <Typography.Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>FinSight</Typography.Title>
        <Typography.Paragraph type="secondary" style={{ textAlign: 'center' }}>Personal Finance Intelligence</Typography.Paragraph>
        {error && <Alert type="error" message={error} style={{ marginBottom: 16 }} />}
        <Form layout="vertical" onFinish={onFinish}>
          <Form.Item name="username" label="Username" rules={[{ required: true }]}>
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="Password" rules={[{ required: true }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>Sign in</Button>
        </Form>
      </Card>
    </div>
  )
}
