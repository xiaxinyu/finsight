import { Form, Input, Button, Alert, Typography, message } from 'antd'
import { LockOutlined, SafetyOutlined } from '@ant-design/icons'
import { changePassword } from '../../api/auth'
import { DataPageLayout } from '../../components/DataPageLayout'
import { useAuth } from '../../hooks/useAuth'

export function AccountSecurityPage() {
  const { username, displayName } = useAuth()
  const [form] = Form.useForm()

  const onFinish = async (values: { currentPassword: string; newPassword: string; confirm: string }) => {
    await changePassword(values.currentPassword, values.newPassword)
    message.success('Password updated')
    form.resetFields()
  }

  return (
    <DataPageLayout
      title="Account & security"
      subtitle="Manage your login credentials"
      icon={<SafetyOutlined />}
    >
      <Typography.Paragraph type="secondary" style={{ maxWidth: 520, marginBottom: 16 }}>
        Signed in as <strong>{displayName || username}</strong> ({username}).
        Password must be at least 8 characters and include both letters and digits.
      </Typography.Paragraph>

      <Alert
        type="info"
        showIcon
        style={{ maxWidth: 520, marginBottom: 20 }}
        message="Ledger data is private to your account. Administrators manage users and system settings but cannot browse other users' financial records."
      />

      <Form
        form={form}
        layout="vertical"
        style={{ maxWidth: 420 }}
        onFinish={onFinish}
      >
        <Form.Item
          name="currentPassword"
          label="Current password"
          rules={[{ required: true }]}
        >
          <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label="New password"
          rules={[
            { required: true },
            { min: 8 },
            { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: 'Include a letter and a digit' },
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          name="confirm"
          label="Confirm new password"
          dependencies={['newPassword']}
          rules={[
            { required: true },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) {
                  return Promise.resolve()
                }
                return Promise.reject(new Error('Passwords do not match'))
              },
            }),
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit">Update password</Button>
      </Form>
    </DataPageLayout>
  )
}
