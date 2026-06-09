import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Col, Form, Input, InputNumber, Row, Table, Tag } from 'antd'
import { CalendarOutlined, FundOutlined } from '@ant-design/icons'
import { billCalendar, budgetVsActual, cashflowMetrics, listBills, saveBill, saveBudgetLine } from '../../api/finance'
import { DataPageLayout } from '../../components/DataPageLayout'
import { KpiGrid } from '../../components/KpiGrid'
import { formatMoney } from '../../utils/format'
import { formatTableDate } from '../../utils/cell'

export function PlanningPage() {
  const qc = useQueryClient()
  const [billForm] = Form.useForm()
  const [budgetLimit, setBudgetLimit] = useState<number>(5000)

  const { data: cashflow } = useQuery({ queryKey: ['cashflow'], queryFn: cashflowMetrics })
  const { data: bva } = useQuery({ queryKey: ['budget-vs-actual'], queryFn: budgetVsActual })
  const { data: bills } = useQuery({ queryKey: ['bills'], queryFn: listBills })
  const { data: calendar } = useQuery({ queryKey: ['bill-calendar'], queryFn: billCalendar })

  const bvaMeta = bva?.[0]
  const safe = Number(cashflow?.safeToSpend || 0)
  const runway = Number(cashflow?.runwayMonths || 0)

  const onSaveBill = async () => {
    const v = await billForm.validateFields()
    await saveBill(v)
    billForm.resetFields()
    qc.invalidateQueries({ queryKey: ['bills'] })
    qc.invalidateQueries({ queryKey: ['bill-calendar'] })
    qc.invalidateQueries({ queryKey: ['cashflow'] })
  }

  const onSaveBudget = async () => {
    await saveBudgetLine({ bucketKey: 'all', limitAmount: budgetLimit })
    qc.invalidateQueries({ queryKey: ['budget-vs-actual'] })
  }

  return (
    <DataPageLayout
      title="Planning"
      subtitle="Budget, bills, and safe-to-spend"
      icon={<FundOutlined />}
    >
      <KpiGrid items={[
        { key: 'safe', label: 'Safe to spend', value: formatMoney(safe), icon: <FundOutlined /> },
        { key: 'runway', label: 'Runway (months)', value: runway.toFixed(1), icon: <CalendarOutlined /> },
        { key: 'actual', label: 'Spent MTD', value: formatMoney(Number(bvaMeta?.actualTotal || 0)) },
        { key: 'limit', label: 'Budget limit', value: formatMoney(Number(bvaMeta?.limitTotal || 0)) },
      ]} />

      <Row gutter={[12, 12]}>
        <Col xs={24} lg={12}>
          <div className="fs-table-panel" style={{ padding: 12 }}>
            <h4 style={{ margin: '0 0 8px' }}>Monthly budget</h4>
            <InputNumber
              size="small"
              style={{ width: 160, marginRight: 8 }}
              value={budgetLimit}
              onChange={(v) => setBudgetLimit(Number(v || 0))}
            />
            <Button size="small" type="primary" onClick={onSaveBudget}>Save limit</Button>
            {bvaMeta && Number(bvaMeta.limitTotal) > 0 && Number(bvaMeta.actualTotal) / Number(bvaMeta.limitTotal) > 0.8 && (
              <Tag color="orange" style={{ marginLeft: 8 }}>Over 80% used</Tag>
            )}
          </div>
        </Col>
        <Col xs={24} lg={12}>
          <div className="fs-table-panel" style={{ padding: 12 }}>
            <h4 style={{ margin: '0 0 8px' }}>Add bill</h4>
            <Form form={billForm} layout="inline" size="small">
              <Form.Item name="name" rules={[{ required: true }]}><Input placeholder="Name" /></Form.Item>
              <Form.Item name="amount" rules={[{ required: true }]}><InputNumber placeholder="Amount" /></Form.Item>
              <Form.Item name="dueDay" rules={[{ required: true }]}><InputNumber min={1} max={28} placeholder="Due day" /></Form.Item>
              <Button type="primary" onClick={onSaveBill}>Add</Button>
            </Form>
          </div>
        </Col>
      </Row>

      <Row gutter={[12, 12]} style={{ marginTop: 12 }}>
        <Col xs={24} lg={12}>
          <div className="fs-table-panel" style={{ padding: 0 }}>
            <Table
              className="fs-data-table"
              size="small"
              rowKey="id"
              pagination={false}
              dataSource={bills || []}
              columns={[
                { title: 'Bill', dataIndex: 'name' },
                { title: 'Amount', dataIndex: 'amount', align: 'right', render: (v) => formatMoney(Number(v)) },
                { title: 'Due day', dataIndex: 'dueDay', width: 80 },
              ]}
            />
          </div>
        </Col>
        <Col xs={24} lg={12}>
          <div className="fs-table-panel" style={{ padding: 0 }}>
            <Table
              className="fs-data-table"
              size="small"
              rowKey={(_, i) => String(i)}
              pagination={false}
              dataSource={calendar || []}
              columns={[
                { title: 'Date', dataIndex: 'date', render: (v) => formatTableDate(v) },
                { title: 'Bill', dataIndex: 'name' },
                { title: 'Amount', dataIndex: 'amount', align: 'right', render: (v) => formatMoney(Number(v)) },
              ]}
            />
          </div>
        </Col>
      </Row>
    </DataPageLayout>
  )
}
