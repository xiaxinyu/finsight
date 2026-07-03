import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Alert, Button, DatePicker, Drawer, Form, Input, InputNumber, Select, Tabs, message,
} from 'antd'
import { PlusOutlined } from '@ant-design/icons'
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
import { LoanDrawerHero } from './LoanDrawerHero'
import { loanFormCopy } from './loanLabels'
import { BankCardFormDrawer } from '../../components/BankCardFormDrawer'

type LoanFormValues = {
  name?: string
  lenderName?: string
  lenderBankCode?: string
  principalAmount?: number
  outstandingBalance?: number
  interestRatePct?: number
  monthlyPayment?: number
  termMonths?: number
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
  onCardsChanged?: () => void
}

export function LoanDetailDrawer({
  loan, open, initialTab = 'detail', cardOptions, onClose, onSaved, onCardsChanged,
}: Props) {
  const isCreate = !loan?.id
  const [tab, setTab] = useState<'detail' | 'links'>(initialTab)
  const [saveLoading, setSaveLoading] = useState(false)
  const [cardDrawerOpen, setCardDrawerOpen] = useState(false)
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
      termMonths: openLoan.termMonths,
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
        termMonths: values.termMonths ?? null,
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
        message.success(loanFormCopy.saved)
      } else {
        await createLoan(payload)
        message.success(loanFormCopy.created)
        onClose()
      }
      onSaved()
    } catch (e) {
      if (e instanceof Error && e.message) message.error(e.message)
    } finally {
      setSaveLoading(false)
    }
  }

  const detailForm = (
    <Form form={form} layout="vertical" className="fs-loan-form">
      <Form.Item name="lenderName" label={loanFormCopy.lender} rules={[{ required: true, message: loanFormCopy.lenderRequired }]}>
        <Input placeholder={loanFormCopy.lenderPlaceholder} />
      </Form.Item>
      <Form.Item name="name" label={loanFormCopy.alias}>
        <Input placeholder={loanFormCopy.aliasPlaceholder} />
      </Form.Item>
      <div className="fs-loan-form-row">
        <Form.Item name="principalAmount" label={loanFormCopy.principal} rules={[{ required: true }]} className="fs-loan-form-col">
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
          label={loanFormCopy.remaining}
          className="fs-loan-form-col"
          rules={[
            ({ getFieldValue }) => ({
              validator(_, value) {
                const principal = getFieldValue('principalAmount')
                if (value == null || principal == null) return Promise.resolve()
                if (value < 0) return Promise.reject(new Error(loanFormCopy.remainingNegative))
                if (value > principal) return Promise.reject(new Error(loanFormCopy.remainingExceeds))
                return Promise.resolve()
              },
            }),
          ]}
        >
          <InputNumber min={0} max={principalWatch ?? undefined} style={{ width: '100%' }} />
        </Form.Item>
      </div>
      <div className="fs-loan-form-row">
        <Form.Item name="interestRatePct" label={loanFormCopy.apr} className="fs-loan-form-col">
          <InputNumber min={0} max={100} step={0.01} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="monthlyPayment" label={loanFormCopy.monthly} className="fs-loan-form-col">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
      </div>
      <div className="fs-loan-form-row">
        <Form.Item name="termMonths" label={loanFormCopy.termMonths} className="fs-loan-form-col"
          extra={loanFormCopy.termExtra}
        >
          <InputNumber min={1} max={600} style={{ width: '100%' }} placeholder={loanFormCopy.termPlaceholder} />
        </Form.Item>
        <Form.Item name="maturityDate" label={loanFormCopy.maturity} className="fs-loan-form-col">
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
      </div>
      <Form.Item
        name="disbursementCardId"
        label={loanFormCopy.disbursementCard}
        rules={[{ required: true, message: loanFormCopy.disbursementRequired }]}
        extra={loanFormCopy.disbursementExtra}
      >
        {cardOptions.length === 0 ? (
          <div className="fs-loan-card-empty">
            <Alert
              type="info"
              showIcon
              message={loanFormCopy.noCards}
              description={loanFormCopy.noCardsHint}
            />
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setCardDrawerOpen(true)}
              style={{ marginTop: 12 }}
            >
              {loanFormCopy.addCard}
            </Button>
          </div>
        ) : (
          <Select
            showSearch
            optionFilterProp="label"
            options={cardOptions}
            placeholder={loanFormCopy.selectCard}
            dropdownRender={(menu) => (
              <>
                {menu}
                <div className="fs-loan-card-select-footer">
                  <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => setCardDrawerOpen(true)}>
                    {loanFormCopy.addNewCard}
                  </Button>
                  <Link to="/admin/cards">{loanFormCopy.manageCards}</Link>
                </div>
              </>
            )}
          />
        )}
      </Form.Item>
      <Form.Item name="repaymentCardId" label={loanFormCopy.repaymentCard} extra={loanFormCopy.repaymentExtra}>
        {cardOptions.length === 0 ? (
          <Select disabled placeholder={loanFormCopy.noCards} />
        ) : (
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            options={cardOptions}
            placeholder={loanFormCopy.sameAsDisbursement}
          />
        )}
      </Form.Item>
      <div className="fs-loan-form-row">
        <Form.Item name="repaymentMethod" label={loanFormCopy.repaymentMethod} className="fs-loan-form-col">
          <Select
            allowClear
            options={(Object.keys(REPAYMENT_METHOD_LABELS) as RepaymentMethod[]).map((k) => ({
              value: k,
              label: REPAYMENT_METHOD_LABELS[k],
            }))}
          />
        </Form.Item>
        <Form.Item name="status" label={loanFormCopy.status} className="fs-loan-form-col">
          <Select options={[{ value: 'ACTIVE', label: loanFormCopy.active }, { value: 'CLOSED', label: loanFormCopy.closed }]} />
        </Form.Item>
      </div>
      <Form.Item name="notes" label={loanFormCopy.notes}>
        <Input.TextArea rows={2} placeholder={loanFormCopy.notesPlaceholder} />
      </Form.Item>
      <Button type="primary" block loading={saveLoading} onClick={saveLoan}>
        {isCreate ? loanFormCopy.saveCreate : loanFormCopy.saveEdit}
      </Button>
    </Form>
  )

  return (
    <Drawer
      title={isCreate ? loanFormCopy.createTitle : undefined}
      width={Math.min(920, typeof window !== 'undefined' ? Math.floor(window.innerWidth * 0.92) : 920)}
      open={open}
      onClose={onClose}
      destroyOnClose
      afterOpenChange={handleAfterOpen}
      className="fs-loan-detail-drawer"
      styles={{ body: { paddingTop: 8 } }}
    >
      {!isCreate && openLoan && <LoanDrawerHero loan={openLoan} />}

      {isCreate ? (
        detailForm
      ) : (
        <Tabs
          activeKey={tab}
          onChange={(k) => setTab(k as 'detail' | 'links')}
          className="fs-loan-detail-tabs"
          items={[
            { key: 'detail', label: loanFormCopy.tabDetail, children: detailForm },
            {
              key: 'links',
              label: (
                <span>
                  {loanFormCopy.tabLinks}
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
      <BankCardFormDrawer
        open={cardDrawerOpen}
        onClose={() => setCardDrawerOpen(false)}
        onSaved={(cardId) => {
          onCardsChanged?.()
          if (cardId) form.setFieldValue('disbursementCardId', cardId)
          setCardDrawerOpen(false)
        }}
      />
    </Drawer>
  )
}
