import { useState, type CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import {
  Alert, Button, DatePicker, Drawer, Form, Input, InputNumber, Select, Tabs, message,
} from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import {
  REPAYMENT_METHOD_LABELS,
  createLoan,
  updateLoan,
  type LoanRow,
  type LoanWritePayload,
  type RepaymentMethod,
} from '../../api/loans'
import { LoanTxnLinkPanel } from './LoanTxnLinkPanel'
import { bankAccent, bankInitial, formatRate } from './loanDisplay'
import { formatMoney } from '../../utils/format'

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

type Props = {
  loan: LoanRow | null
  open: boolean
  initialTab?: 'detail' | 'links'
  cardOptions: { value: string; label: string }[]
  onClose: () => void
  onSaved: () => void
}

export function LoanDetailDrawer({
  loan, open, initialTab = 'detail', cardOptions, onClose, onSaved,
}: Props) {
  const isCreate = !loan?.id
  const [tab, setTab] = useState<'detail' | 'links'>(initialTab)
  const [saveLoading, setSaveLoading] = useState(false)
  const [form] = Form.useForm<LoanFormValues>()
  const principalWatch = Form.useWatch('principalAmount', form)

  const openLoan = loan

  const resetForm = () => {
    if (!openLoan) {
      form.resetFields()
      form.setFieldsValue({ status: 'ACTIVE' })
      return
    }
    form.setFieldsValue({
      name: openLoan.name,
      lenderName: openLoan.lenderName,
      lenderBankCode: openLoan.lenderBankCode,
      principalAmount: openLoan.principalAmount,
      outstandingBalance: openLoan.outstandingBalance ?? openLoan.principalAmount,
      interestRatePct: openLoan.interestRatePct,
      monthlyPayment: openLoan.monthlyPayment,
      repaymentMethod: openLoan.repaymentMethod,
      maturityDate: openLoan.maturityDate ? dayjs(openLoan.maturityDate) : undefined,
      disbursementCardId: openLoan.disbursementCardId,
      repaymentCardId: openLoan.repaymentCardId,
      status: openLoan.status ?? 'ACTIVE',
      notes: openLoan.notes,
    })
  }

  const handleAfterOpen = (visible: boolean) => {
    if (visible) {
      setTab(isCreate ? 'detail' : initialTab)
      resetForm()
    }
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
      if (openLoan?.id) {
        await updateLoan(openLoan.id, payload)
        message.success('已保存')
      } else {
        await createLoan(payload)
        message.success('贷款已添加')
        onClose()
      }
      onSaved()
    } catch (e) {
      if (e instanceof Error && e.message) message.error(e.message)
    } finally {
      setSaveLoading(false)
    }
  }

  const accent = bankAccent(openLoan?.lenderBankCode, openLoan?.lenderName)

  const detailForm = (
    <Form form={form} layout="vertical" className="fs-loan-form">
      <Form.Item name="lenderName" label="贷款银行" rules={[{ required: true, message: '请输入银行名称' }]}>
        <Input placeholder="如：交通银行" />
      </Form.Item>
      <Form.Item name="name" label="备注名称（可选）">
        <Input placeholder="如：经营贷 A" />
      </Form.Item>
      <div className="fs-loan-form-row">
        <Form.Item name="principalAmount" label="本金 (¥)" rules={[{ required: true }]} className="fs-loan-form-col">
          <InputNumber
            min={0}
            style={{ width: '100%' }}
            formatter={(v) => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
            onChange={(v) => {
              if (isCreate && v != null) form.setFieldValue('outstandingBalance', v)
            }}
          />
        </Form.Item>
        <Form.Item
          name="outstandingBalance"
          label="剩余本金 (¥)"
          className="fs-loan-form-col"
          rules={[
            ({ getFieldValue }) => ({
              validator(_, value) {
                const principal = getFieldValue('principalAmount')
                if (value == null || principal == null) return Promise.resolve()
                if (value < 0) return Promise.reject(new Error('不能为负'))
                if (value > principal) return Promise.reject(new Error('不能超过本金'))
                return Promise.resolve()
              },
            }),
          ]}
        >
          <InputNumber min={0} max={principalWatch ?? undefined} style={{ width: '100%' }} />
        </Form.Item>
      </div>
      <div className="fs-loan-form-row">
        <Form.Item name="interestRatePct" label="年利率 (%)" className="fs-loan-form-col">
          <InputNumber min={0} max={100} step={0.01} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="monthlyPayment" label="月供 (¥)" className="fs-loan-form-col">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
      </div>
      <Form.Item
        name="disbursementCardId"
        label="放款到账卡"
        rules={[{ required: true, message: '请选择放款卡' }]}
        extra="银行贷款发放后进入此卡的流水"
      >
        {cardOptions.length === 0 ? (
          <Alert
            type="info"
            showIcon
            message={<>请先在 <Link to="/admin/cards">Admin → Bank Cards</Link> 添加银行卡</>}
          />
        ) : (
          <Select showSearch optionFilterProp="label" options={cardOptions} placeholder="选择银行卡" />
        )}
      </Form.Item>
      <Form.Item name="repaymentCardId" label="还款扣款卡（可选）" extra="与放款卡不同时填写">
        <Select allowClear showSearch optionFilterProp="label" options={cardOptions} placeholder="默认同放款卡" />
      </Form.Item>
      <div className="fs-loan-form-row">
        <Form.Item name="repaymentMethod" label="还款方式" className="fs-loan-form-col">
          <Select
            allowClear
            options={(Object.keys(REPAYMENT_METHOD_LABELS) as RepaymentMethod[]).map((k) => ({
              value: k,
              label: REPAYMENT_METHOD_LABELS[k],
            }))}
          />
        </Form.Item>
        <Form.Item name="maturityDate" label="到期日" className="fs-loan-form-col">
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
      </div>
      <Form.Item name="status" label="状态">
        <Select options={[{ value: 'ACTIVE', label: '在贷' }, { value: 'CLOSED', label: '已结清' }]} />
      </Form.Item>
      <Form.Item name="notes" label="备注">
        <Input.TextArea rows={2} placeholder="到期说明、放款卡核对等" />
      </Form.Item>
      <Button type="primary" block loading={saveLoading} onClick={saveLoan}>
        {isCreate ? '添加贷款' : '保存修改'}
      </Button>
    </Form>
  )

  return (
    <Drawer
      title={isCreate ? '添加贷款' : undefined}
      width={520}
      open={open}
      onClose={onClose}
      destroyOnClose
      afterOpenChange={handleAfterOpen}
      className="fs-loan-detail-drawer"
    >
      {!isCreate && openLoan && (
        <div className="fs-loan-drawer-hero" style={{ '--loan-accent': accent } as CSSProperties}>
          <div className="fs-loan-drawer-hero-badge">{bankInitial(openLoan.lenderName)}</div>
          <div className="fs-loan-drawer-hero-body">
            <div className="fs-loan-drawer-hero-title">{openLoan.lenderName}</div>
            <div className="fs-loan-drawer-hero-stats">
              <span className="fs-loan-drawer-hero-rate">{formatRate(openLoan.interestRatePct)}</span>
              <span>{formatMoney(openLoan.outstandingBalance ?? openLoan.principalAmount ?? 0)} 剩余</span>
              {openLoan.monthlyPayment != null && (
                <span>月供 {formatMoney(openLoan.monthlyPayment)}</span>
              )}
            </div>
          </div>
        </div>
      )}

      {isCreate ? (
        detailForm
      ) : (
        <Tabs
          activeKey={tab}
          onChange={(k) => setTab(k as 'detail' | 'links')}
          className="fs-loan-detail-tabs"
          items={[
            { key: 'detail', label: '详情', children: detailForm },
            {
              key: 'links',
              label: (
                <span>
                  关联交易
                  {(openLoan?.linkCount ?? 0) > 0 && (
                    <span className="fs-loan-tab-badge">{openLoan?.linkCount}</span>
                  )}
                </span>
              ),
              children: openLoan ? (
                <LoanTxnLinkPanel loan={openLoan} onLinksChanged={onSaved} />
              ) : null,
            },
          ]}
        />
      )}
    </Drawer>
  )
}
