import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Col, Form, Input, InputNumber, Progress, Row } from 'antd'
import { AimOutlined } from '@ant-design/icons'
import { goalProgress, listGoals, saveGoal, simulateScenario } from '../../api/finance'
import { DataPageLayout } from '../../components/DataPageLayout'
import { ContentCard } from '../../components/ContentCard'
import { EmptyState } from '../../components/EmptyState'
import { KpiGrid } from '../../components/KpiGrid'
import { formatMoney } from '../../utils/format'

function GoalCard({ goal }: { goal: Record<string, unknown> }) {
  const { data: progress } = useQuery({
    queryKey: ['goal-progress', goal.id],
    queryFn: () => goalProgress(String(goal.id)),
    enabled: !!goal.id,
  })
  const target = Number(goal.targetAmount || 0)
  const current = Number(goal.currentAmount || 0)
  const pct = Number(progress?.percent ?? (target > 0 ? Math.min(100, (current / target) * 100) : 0))
  const months = progress?.monthsToTarget as number | undefined

  return (
    <ContentCard title={String(goal.name)} size="small">
      <Progress percent={Math.round(pct)} />
      <div style={{ fontSize: 12, marginTop: 8 }}>
        {formatMoney(current)} / {formatMoney(target)}
        {months != null && <div style={{ marginTop: 4, opacity: 0.7 }}>~{months} months at current contribution</div>}
      </div>
    </ContentCard>
  )
}

export function GoalsPage() {
  const qc = useQueryClient()
  const [goalForm] = Form.useForm()
  const [scenarioForm] = Form.useForm()
  const [scenario, setScenario] = useState<{
    baseline: Record<string, number>
    scenario: Record<string, number>
    delta: Record<string, number>
  } | null>(null)

  const { data: goals, isError, error } = useQuery({ queryKey: ['goals'], queryFn: listGoals })

  const onSave = async () => {
    const v = await goalForm.validateFields()
    await saveGoal(v)
    goalForm.resetFields()
    qc.invalidateQueries({ queryKey: ['goals'] })
  }

  const onSimulate = async () => {
    const v = await scenarioForm.validateFields()
    const res = await simulateScenario({
      lumpSumExpense: Number(v.lumpSum || 0),
      incomeChangePct: Number(v.incomePct || 0),
      newMonthlyBill: Number(v.newBill || 0),
    })
    setScenario(res)
  }

  return (
    <DataPageLayout title="Goals" subtitle="Savings targets and what-if scenarios" icon={<AimOutlined />}>
      {isError && (
        <Alert type="error" showIcon style={{ marginBottom: 8 }}
          message="Failed to load goals"
          description={error instanceof Error ? error.message : 'Please sign in again.'} />
      )}
      {!goals?.length && !isError && (
        <div style={{ marginBottom: 12 }}>
          <EmptyState title="No goals yet" description="Create a savings target below to track progress." />
        </div>
      )}
      <Row gutter={[12, 12]}>
        {(goals || []).map((g) => (
          <Col key={String(g.id)} xs={24} md={12} lg={8}>
            <GoalCard goal={g} />
          </Col>
        ))}
      </Row>

      <Row gutter={[12, 12]} style={{ marginTop: 12 }}>
        <Col xs={24} lg={12}>
          <ContentCard title="New goal" size="small">
            <Form form={goalForm} layout="vertical" size="small">
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
            <Form form={scenarioForm} layout="vertical" size="small">
              <Form.Item name="lumpSum" label="One-time expense"><InputNumber style={{ width: '100%' }} /></Form.Item>
              <Form.Item name="incomePct" label="Income change %"><InputNumber style={{ width: '100%' }} /></Form.Item>
              <Form.Item name="newBill" label="New monthly bill"><InputNumber style={{ width: '100%' }} /></Form.Item>
              <Button onClick={onSimulate}>Simulate</Button>
            </Form>
            {scenario && (
              <div style={{ marginTop: 12 }}>
                <KpiGrid items={[
                  { key: 'br', label: 'Baseline runway', value: `${scenario.baseline.runwayMonths?.toFixed(1)} mo` },
                  { key: 'sr', label: 'Scenario runway', value: `${scenario.scenario.runwayMonths?.toFixed(1)} mo` },
                  { key: 'd', label: 'Runway delta', value: `${scenario.delta.runwayMonths >= 0 ? '+' : ''}${scenario.delta.runwayMonths?.toFixed(1)} mo` },
                  { key: 'nw', label: 'Net worth delta', value: formatMoney(scenario.delta.netWorth || 0) },
                ]} />
              </div>
            )}
          </ContentCard>
        </Col>
      </Row>
    </DataPageLayout>
  )
}
