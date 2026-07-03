import { useMemo, useState } from 'react'
import { useViewportTableHeight } from '../../hooks/useViewportTableHeight'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Col, Form, Input, InputNumber, Popconfirm, Row, Select, Table, Tabs, Tag, Typography, message } from 'antd'
import { Link } from 'react-router-dom'
import { CalendarOutlined, DeleteOutlined, EditOutlined, FundOutlined } from '@ant-design/icons'
import { billCalendar, budgetVsActual, cashflowMetrics, deleteBill, fetchIncomePayDays, listBills, listGoals, saveBill, saveBudgetLine, saveIncomePayDays } from '../../api/finance'
import { DataPageLayout } from '../../components/DataPageLayout'
import { KpiGrid } from '../../components/KpiGrid'
import { EmptyState } from '../../components/EmptyState'
import { formatMoney } from '../../utils/format'
import { formatTableDate } from '../../utils/cell'
import { useFeatureFlags } from '../../hooks/useFeatureFlags'

function invalidateFinance(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['cashflow'] })
  qc.invalidateQueries({ queryKey: ['budget-vs-actual'] })
  qc.invalidateQueries({ queryKey: ['financial-pulse'] })
  qc.invalidateQueries({ queryKey: ['decision-cards'] })
  qc.invalidateQueries({ queryKey: ['wealth'] })
}

export function PlanningPage() {
  const qc = useQueryClient()
  const { flags } = useFeatureFlags()
  const [billForm] = Form.useForm()
  const [budgetLimitDraft, setBudgetLimitDraft] = useState<number>(5000)
  const [budgetDirty, setBudgetDirty] = useState(false)
  const [editingBillId, setEditingBillId] = useState<string | null>(null)
  const [payDayDraft, setPayDayDraft] = useState<number[]>([5, 20])
  const [payDayDirty, setPayDayDirty] = useState(false)

  const { data: cashflow, isError: cfErr, error: cfError } = useQuery({ queryKey: ['cashflow'], queryFn: cashflowMetrics })
  const { data: bva, isError: bvaErr, error: bvaError } = useQuery({ queryKey: ['budget-vs-actual'], queryFn: () => budgetVsActual() })
  const { data: bills } = useQuery({ queryKey: ['bills'], queryFn: listBills })
  const { data: calendar } = useQuery({ queryKey: ['bill-calendar'], queryFn: billCalendar })
  const { data: goals } = useQuery({ queryKey: ['goals'], queryFn: listGoals })
  const { data: payDaysConfig } = useQuery({ queryKey: ['income-pay-days'], queryFn: fetchIncomePayDays })

  const tableHeight = useViewportTableHeight(320)
  const bvaMeta = bva?.[0]
  const budgetLimitFromData = useMemo(() => Number(bvaMeta?.limitTotal || 0), [bvaMeta?.limitTotal])
  const budgetLimit = !budgetDirty && budgetLimitFromData > 0 ? budgetLimitFromData : budgetLimitDraft
  const incomePayDays = !payDayDirty && payDaysConfig?.incomePayDays?.length
    ? payDaysConfig.incomePayDays
    : payDayDraft

  const safe = Number(cashflow?.safeToSpend || 0)
  const runway = Number(cashflow?.runwayMonths || 0)
  const loadError = cfErr ? cfError : bvaErr ? bvaError : null

  const onSaveBill = async () => {
    const v = await billForm.validateFields()
    await saveBill(editingBillId ? { ...v, id: editingBillId } : v)
    billForm.resetFields()
    setEditingBillId(null)
    qc.invalidateQueries({ queryKey: ['bills'] })
    qc.invalidateQueries({ queryKey: ['bill-calendar'] })
    invalidateFinance(qc)
    message.success(editingBillId ? 'Bill updated' : 'Bill added')
  }

  const onEditBill = (bill: Record<string, unknown>) => {
    setEditingBillId(String(bill.id))
    billForm.setFieldsValue({
      name: bill.name,
      amount: bill.amount,
      dueDay: bill.dueDay,
    })
  }

  const onDeleteBill = async (id: string) => {
    await deleteBill(id)
    if (editingBillId === id) {
      billForm.resetFields()
      setEditingBillId(null)
    }
    qc.invalidateQueries({ queryKey: ['bills'] })
    qc.invalidateQueries({ queryKey: ['bill-calendar'] })
    invalidateFinance(qc)
    message.success('Bill removed')
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

  const onSavePayDays = async () => {
    try {
      const saved = await saveIncomePayDays(incomePayDays)
      setPayDayDraft(saved.incomePayDays)
      setPayDayDirty(false)
      qc.invalidateQueries({ queryKey: ['income-pay-days'] })
      qc.invalidateQueries({ queryKey: ['cash-risk-calendar'] })
      message.success('Income pay days saved')
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Failed to save pay days')
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
      {!flags.planningPersist && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 8 }}
          message="Planning data is session-only"
          description="Budget and bills are stored in memory until you enable finsight.planning.persist on the server. They reset after restart."
        />
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
            <h4 style={{ margin: '0 0 8px' }}>{editingBillId ? 'Edit bill' : 'Add bill'}</h4>
            <Form form={billForm} layout="inline" size="small">
              <Form.Item name="name" rules={[{ required: true }]}><Input placeholder="Name" /></Form.Item>
              <Form.Item name="amount" rules={[{ required: true }]}><InputNumber placeholder="Amount" /></Form.Item>
              <Form.Item name="dueDay" rules={[{ required: true }]}><InputNumber min={1} max={28} placeholder="Due day" /></Form.Item>
              <Button type="primary" onClick={onSaveBill}>{editingBillId ? 'Save' : 'Add'}</Button>
              {editingBillId && (
                <Button onClick={() => { billForm.resetFields(); setEditingBillId(null) }}>Cancel</Button>
              )}
            </Form>
          </div>
        </Col>
      </Row>

      <Row gutter={[12, 12]} style={{ marginTop: 12 }}>
        <Col xs={24} lg={12}>
          <div className="fs-table-panel" style={{ padding: 12 }}>
            <h4 style={{ margin: '0 0 8px' }}>Forecast income pay days</h4>
            <Typography.Paragraph type="secondary" style={{ fontSize: 12, marginBottom: 8 }}>
              Used by Cash risk calendar to spread monthly income (1–28, up to 4 days).
            </Typography.Paragraph>
            <Select
              mode="tags"
              size="small"
              style={{ width: '100%', maxWidth: 320, marginBottom: 8 }}
              placeholder="e.g. 5, 20"
              value={incomePayDays.map(String)}
              onChange={(vals) => {
                const nums = vals
                  .map((v) => Number.parseInt(String(v), 10))
                  .filter((n) => Number.isFinite(n) && n >= 1 && n <= 28)
                setPayDayDirty(true)
                setPayDayDraft([...new Set(nums)].sort((a, b) => a - b))
              }}
              tokenSeparators={[',', ' ']}
            />
            <Button size="small" type="primary" onClick={onSavePayDays}>Save pay days</Button>
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
                {
                  title: '',
                  width: 88,
                  render: (_, row) => (
                    <>
                      <Button type="text" size="small" icon={<EditOutlined />} onClick={() => onEditBill(row as Record<string, unknown>)} />
                      <Popconfirm title="Remove bill?" onConfirm={() => onDeleteBill(String(row.id))}>
                        <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                      </Popconfirm>
                    </>
                  ),
                },
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
