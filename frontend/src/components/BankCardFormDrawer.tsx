import { useState, type CSSProperties } from 'react'
import { Button, Drawer, Form, Input, Select, message } from 'antd'
import { createCard, updateCard } from '../api/admin'
import {
  BANK_OPTIONS, CARD_TYPE_OPTIONS, bankAccent, bankInitial, type CardTypeCode,
} from '../utils/bankCardDisplay'
import { adminCardsCopy as copy } from '../pages/Admin/adminLabels'

export type BankCardFormValues = {
  bankCode: string
  cardTypeCode: CardTypeCode
  cardNo: string
  cardName?: string
}

export type BankCardRow = {
  id?: string
  bankCode?: string
  cardTypeCode?: string
  cardNo?: string
  cardName?: string
}

type Props = {
  open: boolean
  card?: BankCardRow | null
  onClose: () => void
  /** Called with new or updated card id after save */
  onSaved: (cardId: string) => void
}

export function BankCardFormDrawer({ open, card, onClose, onSaved }: Props) {
  const isEdit = !!card?.id
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm<BankCardFormValues>()
  const bankWatch = Form.useWatch('bankCode', form)

  const resetForm = () => {
    if (card?.id) {
      form.setFieldsValue({
        bankCode: card.bankCode ?? 'CCB',
        cardTypeCode: (card.cardTypeCode ?? 'debit') as CardTypeCode,
        cardNo: card.cardNo ?? '',
        cardName: card.cardName ?? '',
      })
      return
    }
    form.resetFields()
    form.setFieldsValue({ bankCode: 'CCB', cardTypeCode: 'debit' })
  }

  const save = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      const payload = {
        bankCode: values.bankCode,
        cardTypeCode: values.cardTypeCode,
        cardNo: values.cardNo.trim(),
        cardName: values.cardName?.trim() || null,
      }
      if (isEdit && card?.id) {
        await updateCard(card.id, payload)
        message.success(copy.cardUpdated)
        onSaved(card.id)
      } else {
        const saved = await createCard(payload) as BankCardRow
        message.success(copy.cardAdded)
        onSaved(String(saved.id ?? ''))
        onClose()
      }
    } catch (e) {
      if (e instanceof Error && e.message) message.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  const accent = bankAccent(bankWatch)

  return (
    <Drawer
      title={isEdit ? copy.drawerEditTitle : copy.drawerAddTitle}
      width={440}
      open={open}
      onClose={onClose}
      destroyOnClose
      afterOpenChange={(visible) => visible && resetForm()}
      className="fs-bank-card-drawer"
      footer={(
        <Button type="primary" block loading={loading} onClick={save}>
          {isEdit ? copy.drawerSave : copy.drawerAdd}
        </Button>
      )}
    >
      <div className="fs-bank-card-drawer-preview" style={{ '--bank-accent': accent } as CSSProperties}>
        <div className="fs-bank-card-drawer-preview__chip">{bankInitial(bankWatch)}</div>
        <div>
          <div className="fs-bank-card-drawer-preview__title">
            {BANK_OPTIONS.find((b) => b.value === bankWatch)?.label ?? copy.selectBank}
          </div>
          <div className="fs-bank-card-drawer-preview__hint">{copy.drawerHint}</div>
        </div>
      </div>

      <Form form={form} layout="vertical" className="fs-bank-card-form">
        <Form.Item name="bankCode" label={copy.bankLabel} rules={[{ required: true, message: copy.bankRequired }]}>
          <Select
            showSearch
            optionFilterProp="label"
            options={BANK_OPTIONS.map((b) => ({ value: b.value, label: b.label }))}
          />
        </Form.Item>
        <Form.Item name="cardTypeCode" label={copy.cardTypeLabel} rules={[{ required: true }]}>
          <Select options={[...CARD_TYPE_OPTIONS]} />
        </Form.Item>
        <Form.Item
          name="cardNo"
          label={copy.cardNoLabel}
          rules={[{ required: true, message: copy.cardNoRequired }]}
          extra={copy.cardNoExtra}
        >
          <Input placeholder={copy.cardNoPlaceholder} />
        </Form.Item>
        <Form.Item name="cardName" label={copy.displayNameLabel}>
          <Input placeholder={copy.displayNamePlaceholder} />
        </Form.Item>
      </Form>
    </Drawer>
  )
}
