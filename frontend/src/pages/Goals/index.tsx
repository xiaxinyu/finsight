import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Col, Form, Input, InputNumber, Progress, Row } from 'antd'
import { AimOutlined } from '@ant-design/icons'
import { listGoals, saveGoal, simulateScenario } from '../../api/finance'
import { DataPageLayout } from '../../components/DataPageLayout'
import { ContentCard } from '../../components/ContentCard'
import { formatMoney } from '../../utils/format'

export function GoalsPage() {
  const qc = useQueryClient()
  const [form] = Form.useForm()
  const [scenario, setScenario] = useState<Record<string, unknown> | null>(null)

  const { data: goals } = useQuery({ queryKey: ['goals'], queryFn: listGoals })

  const onSave = async () => {
    const v = await form.validateFields()
    await saveGoal(v)
    form.resetFields()
    qc.invalidateQueries({ queryKey: ['goals'] })
  }

  const onSimulate = async () => {
    const res = await simulateScenario({
      lumpSumExpense: Number(form.getFieldValue('lumpSum') || 0),
      incomeChangePct: Number(form.getFieldValue('incomePct') || 0),
      newMonthlyBill: Number(form.getFieldValue('newBill') || 0),
    })
    setScenario(res)
  }

  return (
    <DataPageLayout title="Goals" subtitle="Savings targets and what-if scenarios" icon={<AimOutlined />}>
      <Row gutter={[12, 12]}>
        {(goals || []).map((g) => {
          const target = Number(g.targetAmount || 0)
          const current = Number(g.currentAmount || 0)
          const pct = target > 0 ? Math.min(100, (current / target) * 100) : 0
          return (
            <Col key={String(g.id)} xs={24} md={12} lg={8}>
              <ContentCard title={String(g.name)} size="small">
                <Progress percent={Math.round(pct)} />
                <div style={{ fontSize: 12, marginTop: 8 }}>
                  {formatMoney(current)} / {formatMoney(target)}
                </div>
              </ContentCard>
            </Col>
          )
        })}
      </Row>

      <Row gutter={[12, 12]} style={{ marginTop: 12 }}>
        <Col xs={24} lg={12}>
          <ContentCard title="New goal" size="small">
            <Form form={form} layout="vertical" size="small">
              <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="goalType" label="Type" initialValue="savings"><Input /></Form.Item>
              <Form.Item name="targetAmount" label="Target" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} /></Form.Item>
              <Form.Item name="currentAmount" label="Current" initialValue={0}><InputNumber style={{ width: '100%' }} /></Form.Item>
              <Form.Item name="monthlyContribution" label="Monthly contribution"><InputNumber style={{ width: '100%' }} /></Form.Item>
              <Button type="primary" onClick={onSave}>Save goal</Button>
            </Form>
          </ContentCard>
        </Col>
        <Col xs={24} lg={12}>
          <ContentCard title="Scenario simulator" size="small">
            <Form form={form} layout="vertical" size="small">
              <Form.Item name="lumpSum" label="One-time expense"><InputNumber style={{ width: '100%' }} /></Form.Item>
              <Form.Item name="incomePct" label="Income change %"><InputNumber style={{ width: '100%' }} /></Form.Item>
              <Form.Item name="newBill" label="New monthly bill"><InputNumber style={{ width: '100%' }} /></Form.Item>
              <Button onClick={onSimulate}>Simulate</Button>
            </Form>
            {scenario && (
              <pre style={{ fontSize: 11, marginTop: 12, background: '#f8fafc', padding: 8, borderRadius: 8 }}>
                {JSON.stringify(scenario, null, 2)}
              </pre>
            )}
          </ContentCard>
        </Col>
      </Row>
    </DataPageLayout>
  )
}
