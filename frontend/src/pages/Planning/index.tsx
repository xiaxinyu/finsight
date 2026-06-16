import { useMemo, useState } from 'react'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Col, Form, Input, InputNumber, Row, Table, Tabs, Tag, message } from 'antd'
import { Link } from 'react-router-dom'
import { CalendarOutlined, FundOutlined } from '@ant-design/icons'
import { billCalendar, budgetVsActual, cashflowMetrics, listBills, listGoals, saveBill, saveBudgetLine } from '../../api/finance'
import { DataPageLayout } from '../../components/DataPageLayout'
import { KpiGrid } from '../../components/KpiGrid'
import { EmptyState } from '../../components/EmptyState'
import { formatMoney } from '../../utils/format'
import { formatTableDate } from '../../utils/cell'

function invalidateFinance(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['cashflow'] })
  qc.invalidateQueries({ queryKey: ['budget-vs-actual'] })
  qc.invalidateQueries({ queryKey: ['financial-pulse'] })
  qc.invalidateQueries({ queryKey: ['decision-cards'] })
  qc.invalidateQueries({ queryKey: ['wealth'] })
}

export function PlanningPage() {
  const qc = useQueryClient()
  const [billForm] = Form.useForm()
  const [budgetLimitDraft, setBudgetLimitDraft] = useState<number>(5000)
  const [budgetDirty, setBudgetDirty] = useState(false)

  const { data: cashflow, isError: cfErr, error: cfError } = useQuery({ queryKey: ['cashflow'], queryFn: cashflowMetrics })
  const { data: bva, isError: bvaErr, error: bvaError } = useQuery({ queryKey: ['budget-vs-actual'], queryFn: () => budgetVsActual() })
  const { data: bills } = useQuery({ queryKey: ['bills'], queryFn: listBills })
  const { data: calendar } = useQuery({ queryKey: ['bill-calendar'], queryFn: billCalendar })
  const { data: goals } = useQuery({ queryKey: ['goals'], queryFn: listGoals })

  const tableHeight = useViewportTableHeight(320)
  const bvaMeta = bva?.[0]
  const budgetLimitFromData = useMemo(() => Number(bvaMeta?.limitTotal || 0), [bvaMeta?.limitTotal])
  const budgetLimit = !budgetDirty && budgetLimitFromData > 0 ? budgetLimitFromData : budgetLimitDraft

  const safe = Number(cashflow?.safeToSpend || 0)
  const runway = Number(cashflow?.runwayMonths || 0)
  const loadError = cfErr ? cfError : bvaErr ? bvaError : null

  const onSaveBill = async () => {
    const v = await billForm.validateFields()
    await saveBill(v)
    billForm.resetFields()
    qc.invalidateQueries({ queryKey: ['bills'] })
    qc.invalidateQueries({ queryKey: ['bill-calendar'] })
    invalidateFinance(qc)
  }

  const onSaveBudget = async () => {
    try {
      await saveBudgetLine({ bucketKey: 'all', limitAmount: budgetLimit })
      setBudgetDirty(false)
      invalidateFinance(qc)
      message.success('Budget limit saved')
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Failed to save budget')
    }
  }

  return (
    <DataPageLayout
      title="Planning"
      subtitle="Budget, bills, and safe-to-spend"
      icon={<FundOutlined />}
    >
      {loadError && (
        <Alert type="error" showIcon style={{ marginBottom: 8 }}
          message="Failed to load planning data"
          description={loadError instanceof Error ? loadError.message : 'Please sign in again.'} />
      )}
      <Tabs
        size="small"
        items={[
          {
            key: 'overview',
            label: 'Overview',
            children: (
              <>
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
              onChange={(v) => {
                setBudgetDirty(true)
                setBudgetLimitDraft(Number(v || 0))
              }}
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
              scroll={{ y: tableHeight }}
              locale={{ emptyText: <EmptyState compact title="No bills" description="Add recurring bills above." /> }}
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
              scroll={{ y: tableHeight }}
              locale={{ emptyText: <EmptyState compact title="No upcoming bills" description="Bills with due days in the next 30 days appear here." /> }}
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
              </>
            ),
          },
          {
            key: 'timeline',
            label: 'Timeline',
            children: (
              <Row gutter={[12, 12]}>
                <Col xs={24} lg={14}>
                  <div className="fs-table-panel" style={{ padding: 0 }}>
                    <Table
                      className="fs-data-table"
                      size="small"
                      rowKey={(_, i) => `cal-${i}`}
                      pagination={false}
                      scroll={{ y: tableHeight }}
                      dataSource={calendar || []}
                      columns={[
                        { title: 'Date', dataIndex: 'date', render: (v) => formatTableDate(v) },
                        { title: 'Bill', dataIndex: 'name' },
                        { title: 'Amount', dataIndex: 'amount', align: 'right', render: (v) => formatMoney(Number(v)) },
                      ]}
                    />
                  </div>
                </Col>
                <Col xs={24} lg={10}>
                  <div className="fs-table-panel" style={{ padding: 12 }}>
                    <h4 style={{ marginTop: 0 }}>Goals on timeline</h4>
                    {(goals || []).length === 0 ? (
                      <>
                        <EmptyState compact title="No goals" description="Set a savings goal on the Goals page." />
                        <Link to="/goals" style={{ display: 'block', marginTop: 8 }}>Open Goals →</Link>
                      </>
                    ) : (
                      <ul className="fs-planning-timeline">
                        {(goals || []).map((g) => (
                          <li key={String(g.id)}>
                            <strong>{String(g.name)}</strong>
                            <span>{formatMoney(Number(g.monthlyContribution || 0))}/mo</span>
                          </li>
                        ))}
                      </ul>
                    )}
                    <div style={{ marginTop: 12 }}>
                      <Link to="/reports/annual-outlook">Open annual outlook →</Link>
                    </div>
                  </div>
                </Col>
              </Row>
            ),
          },
        ]}
      />
    </DataPageLayout>
  )
}
