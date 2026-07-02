import { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Badge, Button, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Typography, message,
} from 'antd'
import { BankOutlined, LinkOutlined, PlusOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import {
  REPAYMENT_METHOD_LABELS,
  createLoan,
  deleteLoan,
  fetchLoans,
  updateLoan,
  type LoanRow,
  type LoanWritePayload,
  type RepaymentMethod,
} from '../../api/loans'
import { listAccounts } from '../../api/finance'
import { DataPageLayout } from '../../components/DataPageLayout'
import { ContentCard } from '../../components/ContentCard'
import { EmptyState } from '../../components/EmptyState'
import { FsDataTable } from '../../components/FsDataTable'
import { formatMoney } from '../../utils/format'
import { LoanLinksDrawer } from './LoanLinksDrawer'

type LoanFormValues = {
  name?: string
  lenderName?: string
  lenderBankCode?: string
  principalAmount?: number
  outstandingBalance?: number
  interestRatePct?: number
  monthlyPayment?: number
  repaymentMethod?: RepaymentMethod
  maturityDate?: Dayjs
  disbursementCardId?: string
  repaymentCardId?: string
  status?: 'ACTIVE' | 'CLOSED'
  notes?: string
}

function formatRate(v?: number) {
  if (v == null) return '—'
  return `${v.toFixed(2)}%`
}

function formatDate(v?: string) {
  if (!v) return '—'
  return dayjs(v).format('YYYY-MM-DD')
}

function isActive(row: LoanRow) {
  return row.status !== 'CLOSED'
}

export function LoansPage() {
  const queryClient = useQueryClient()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<LoanRow | null>(null)
  const [linksLoan, setLinksLoan] = useState<LoanRow | null>(null)
  const [saveLoading, setSaveLoading] = useState(false)
  const [form] = Form.useForm<LoanFormValues>()
  const principalWatch = Form.useWatch('principalAmount', form)

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['loans'],
    queryFn: fetchLoans,
  })

  const { data: accounts = [] } = useQuery({
    queryKey: ['accounts'],
    queryFn: listAccounts,
  })

  const cardOptions = useMemo(
    () => accounts.map((a) => ({
      value: String(a.id ?? ''),
      label: String(a.name ?? a.id ?? ''),
    })).filter((o) => o.value),
    [accounts],
  )

  const reload = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['loans'] })
  }, [queryClient])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ status: 'ACTIVE' })
    setModalOpen(true)
  }

  const openEdit = (row: LoanRow) => {
    setEditing(row)
    form.setFieldsValue({
      name: row.name,
      lenderName: row.lenderName,
      lenderBankCode: row.lenderBankCode,
      principalAmount: row.principalAmount,
      outstandingBalance: row.outstandingBalance ?? row.principalAmount,
      interestRatePct: row.interestRatePct,
      monthlyPayment: row.monthlyPayment,
      repaymentMethod: row.repaymentMethod,
      maturityDate: row.maturityDate ? dayjs(row.maturityDate) : undefined,
      disbursementCardId: row.disbursementCardId,
      repaymentCardId: row.repaymentCardId,
      status: row.status ?? 'ACTIVE',
      notes: row.notes,
    })
    setModalOpen(true)
  }

  const saveLoan = async () => {
    try {
      const values = await form.validateFields()
      const payload: LoanWritePayload = {
        name: values.name?.trim() || null,
        lenderName: values.lenderName!.trim(),
        lenderBankCode: values.lenderBankCode?.trim() || null,
        principalAmount: values.principalAmount!,
        outstandingBalance: values.outstandingBalance ?? values.principalAmount,
        interestRatePct: values.interestRatePct ?? null,
        monthlyPayment: values.monthlyPayment ?? null,
        repaymentMethod: values.repaymentMethod ?? null,
        maturityDate: values.maturityDate ? values.maturityDate.format('YYYY-MM-DD') : null,
        disbursementCardId: values.disbursementCardId!,
        repaymentCardId: values.repaymentCardId || null,
        status: values.status ?? 'ACTIVE',
        notes: values.notes?.trim() || null,
      }
      setSaveLoading(true)
      if (editing?.id) {
        await updateLoan(editing.id, payload)
        message.success('Loan updated')
      } else {
        await createLoan(payload)
        message.success('Loan added')
      }
      setModalOpen(false)
      reload()
    } catch (e) {
      if (e instanceof Error && e.message) {
        message.error(e.message)
      }
    } finally {
      setSaveLoading(false)
    }
  }

  const summary = data?.summary
  const loans = data?.loans ?? []

  const tableSummary = useMemo(() => {
    const active = loans.filter(isActive)
    return {
      outstandingBalance: active.reduce((s, r) => s + (r.outstandingBalance ?? r.principalAmount ?? 0), 0),
      monthlyPayment: active.reduce((s, r) => s + (r.monthlyPayment ?? 0), 0),
    }
  }, [loans])

  const columns = [
    {
      title: 'Rate',
      dataIndex: 'interestRatePct',
      width: 72,
      align: 'right' as const,
      sortType: 'number' as const,
      render: (v: number) => <span className="fs-loan-rate">{formatRate(v)}</span>,
    },
    {
      title: 'Lender',
      dataIndex: 'lenderName',
      width: 120,
      sortType: 'text' as const,
      render: (v: string, row: LoanRow) => (
        <div>
          <div className="fs-loan-lender">{v}</div>
          {row.name && <Typography.Text type="secondary" className="fs-loan-sub">{row.name}</Typography.Text>}
        </div>
      ),
    },
    {
      title: 'Outstanding',
      dataIndex: 'outstandingBalance',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 120,
      render: (_: unknown, row: LoanRow) => formatMoney(row.outstandingBalance ?? row.principalAmount ?? 0),
    },
    {
      title: 'Monthly',
      dataIndex: 'monthlyPayment',
      align: 'right' as const,
      sortType: 'number' as const,
      width: 100,
      render: (v: number) => (v == null ? '—' : formatMoney(v)),
    },
    {
      title: 'Repayment',
      dataIndex: 'repaymentMethod',
      width: 100,
      render: (v: RepaymentMethod) => (v ? REPAYMENT_METHOD_LABELS[v] : '—'),
    },
    {
      title: 'Maturity',
      dataIndex: 'maturityDate',
      width: 100,
      sortType: 'date' as const,
      render: (v: string) => formatDate(v),
    },
    {
      title: 'Disbursement',
      dataIndex: 'disbursementCardLabel',
      ellipsis: true,
      width: 120,
      render: (v: string) => v || '—',
    },
    {
      title: 'Repayment card',
      dataIndex: 'repaymentCardLabel',
      ellipsis: true,
      width: 120,
      render: (v: string, row: LoanRow) => v || row.disbursementCardLabel || '—',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      width: 80,
      render: (v: string) => (
        v === 'CLOSED'
          ? <Badge status="default" text="Closed" />
          : <Badge status="processing" text="Active" />
      ),
    },
    {
      title: 'Notes',
      dataIndex: 'notes',
      ellipsis: true,
      width: 140,
    },
    {
      title: '',
      key: 'actions',
      width: 160,
      fixed: 'right' as const,
      render: (_: unknown, row: LoanRow) => (
        <Space size={4}>
          <a onClick={() => setLinksLoan(row)}><LinkOutlined /> Links</a>
          <a onClick={() => openEdit(row)}>Edit</a>
          <Popconfirm
            title="Remove this loan?"
            description="Linked transactions will also be unlinked."
            onConfirm={async () => {
              if (!row.id) return
              try {
                await deleteLoan(row.id)
                message.success('Removed')
                reload()
              } catch (e) {
                message.error(e instanceof Error ? e.message : 'Failed to remove')
              }
            }}
          >
            <a>Delete</a>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <DataPageLayout
      title="Loans"
      subtitle="Lender → disbursement card · track rates, balances, and link repayments to ledger transactions"
      icon={<BankOutlined />}
      className="fs-data-page--dense fs-data-page--loans"
      actions={(
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          Add loan
        </Button>
      )}
    >
      {isError && (
        <Typography.Paragraph type="danger">
          {error instanceof Error ? error.message : 'Failed to load loans'}
        </Typography.Paragraph>
      )}

      {summary && (
        <div className="fs-loans-summary">
          <ContentCard className="fs-loans-summary-card" styles={{ body: { padding: '14px 18px' } }}>
            <div className="fs-loans-summary__label">Total outstanding</div>
            <div className="fs-loans-summary__value">{formatMoney(summary.totalOutstanding ?? 0)}</div>
            <div className="fs-loans-summary__hint">Active loans only</div>
          </ContentCard>
          <ContentCard className="fs-loans-summary-card" styles={{ body: { padding: '14px 18px' } }}>
            <div className="fs-loans-summary__label">Monthly payment</div>
            <div className="fs-loans-summary__value">{formatMoney(summary.totalMonthlyPayment ?? 0)}</div>
          </ContentCard>
          <ContentCard className="fs-loans-summary-card fs-loans-summary-card--rate" styles={{ body: { padding: '14px 18px' } }}>
            <div className="fs-loans-summary__label">Weighted avg rate</div>
            <div className="fs-loans-summary__value">{formatRate(summary.weightedAvgRatePct)}</div>
          </ContentCard>
          <ContentCard className="fs-loans-summary-card" styles={{ body: { padding: '14px 18px' } }}>
            <div className="fs-loans-summary__label">Active loans</div>
            <div className="fs-loans-summary__value">{summary.loanCount ?? 0}</div>
          </ContentCard>
        </div>
      )}

      <ContentCard styles={{ body: { padding: 0 } }}>
        <FsDataTable
          rowKey="id"
          loading={isLoading}
          dataSource={loans}
          columns={columns}
          size="small"
          scroll={{ x: 'max-content' }}
          summary={tableSummary}
          summaryLabel="Active total"
          rowClassName={(row) => (row.status === 'CLOSED' ? 'fs-loan-row--closed' : '')}
          locale={{
            emptyText: (
              <EmptyState
                compact
                title="No loans yet"
                description="Add each facility: lender bank, amount, rate, and the card that receives the funds."
              />
            ),
          }}
        />
      </ContentCard>

      <Modal
        title={editing ? `Edit loan — ${editing.lenderName}` : 'Add loan'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={saveLoan}
        confirmLoading={saveLoading}
        width={560}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 8 }}>
          <Form.Item name="lenderName" label="Lender bank" rules={[{ required: true }]}>
            <Input placeholder="e.g. 交通银行" />
          </Form.Item>
          <Form.Item name="name" label="Label (optional)">
            <Input placeholder="e.g. 经营贷 A" />
          </Form.Item>
          <Space style={{ display: 'flex', width: '100%' }} align="start">
            <Form.Item name="principalAmount" label="Principal (¥)" rules={[{ required: true }]} style={{ flex: 1 }}>
              <InputNumber
                min={0}
                style={{ width: '100%' }}
                formatter={(v) => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                onChange={(v) => {
                  if (!editing && v != null) {
                    form.setFieldValue('outstandingBalance', v)
                  }
                }}
              />
            </Form.Item>
            <Form.Item
              name="outstandingBalance"
              label="Outstanding (¥)"
              style={{ flex: 1 }}
              rules={[
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    const principal = getFieldValue('principalAmount')
                    if (value == null || principal == null) return Promise.resolve()
                    if (value < 0) return Promise.reject(new Error('Cannot be negative'))
                    if (value > principal) return Promise.reject(new Error('Cannot exceed principal'))
                    return Promise.resolve()
                  },
                }),
              ]}
            >
              <InputNumber min={0} max={principalWatch ?? undefined} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex', width: '100%' }} align="start">
            <Form.Item name="interestRatePct" label="Annual rate (%)" style={{ flex: 1 }}>
              <InputNumber min={0} max={100} step={0.01} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="monthlyPayment" label="Monthly payment (¥)" style={{ flex: 1 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item
            name="disbursementCardId"
            label="Disbursement card"
            rules={[{ required: true, message: 'Select the card that receives loan proceeds' }]}
            extra="Loan from the bank is credited to this card in your ledger."
          >
            {cardOptions.length === 0 ? (
              <Alert
                type="info"
                showIcon
                message={<>No bank cards yet. Add cards under <Link to="/admin/cards">Admin → Bank Cards</Link> first.</>}
              />
            ) : (
              <Select
                showSearch
                optionFilterProp="label"
                options={cardOptions}
                placeholder="Select bank card"
              />
            )}
          </Form.Item>
          <Form.Item name="repaymentCardId" label="Repayment card (optional)" extra="Card debited for repayments, if different.">
            <Select allowClear showSearch optionFilterProp="label" options={cardOptions} placeholder="Same as disbursement" />
          </Form.Item>
          <Space style={{ display: 'flex', width: '100%' }} align="start">
            <Form.Item name="repaymentMethod" label="Repayment method" style={{ flex: 1 }}>
              <Select
                allowClear
                options={(Object.keys(REPAYMENT_METHOD_LABELS) as RepaymentMethod[]).map((k) => ({
                  value: k,
                  label: REPAYMENT_METHOD_LABELS[k],
                }))}
              />
            </Form.Item>
            <Form.Item name="maturityDate" label="Maturity" style={{ flex: 1 }}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item name="status" label="Status">
            <Select options={[{ value: 'ACTIVE', label: 'Active' }, { value: 'CLOSED', label: 'Closed' }]} />
          </Form.Item>
          <Form.Item name="notes" label="Notes">
            <Input.TextArea rows={2} placeholder="e.g. 2028年3月到期" />
          </Form.Item>
        </Form>
      </Modal>

      <LoanLinksDrawer
        loan={linksLoan}
        open={!!linksLoan}
        onClose={() => setLinksLoan(null)}
      />
    </DataPageLayout>
  )
}
