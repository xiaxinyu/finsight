import { Button, DatePicker, Input, InputNumber, Select, Space } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import type { TreeSelectNode } from '../hooks/useConsumeTree'
import { CategoryPicker } from './CategoryPicker'

export type TransactionEditDraft = {
  transactionDate?: string | Date
  transactionDesc: string
  txnKind: string
  editAmount: number
  consumeCode: string
  demoArea: string
}

type Props = {
  draft: TransactionEditDraft
  onChange: (draft: TransactionEditDraft) => void
  treeData: TreeSelectNode[]
  cardSummary?: string
  semanticHint?: string
  saving?: boolean
  onSave: () => void
  onCancel: () => void
}

function parseDate(value?: string | Date): Dayjs | null {
  if (!value) return null
  const d = dayjs(value)
  return d.isValid() ? d : null
}

export function TransactionEditPanel({
  draft,
  onChange,
  treeData,
  cardSummary,
  semanticHint,
  saving,
  onSave,
  onCancel,
}: Props) {
  const patch = (partial: Partial<TransactionEditDraft>) => onChange({ ...draft, ...partial })

  return (
    <div className="fs-tx-edit-panel">
      <div className="fs-tx-edit-panel__header">
        <div className="fs-tx-edit-panel__intro">
          <span className="fs-tx-edit-panel__title">Edit transaction</span>
          <span className="fs-tx-edit-panel__hint">
            {semanticHint || 'Review and update details before saving'}
          </span>
        </div>
        {cardSummary ? <span className="fs-tx-edit-panel__card">{cardSummary}</span> : null}
      </div>

      <div className="fs-tx-edit-panel__grid">
        <label className="fs-tx-edit-panel__field fs-tx-edit-panel__field--date">
          <span className="fs-tx-edit-panel__label">Date</span>
          <DatePicker
            size="middle"
            className="fs-tx-edit-panel__control"
            format="MM/DD/YYYY"
            value={parseDate(draft.transactionDate)}
            onChange={(d) => patch({ transactionDate: d ? d.toDate() : undefined })}
          />
        </label>

        <label className="fs-tx-edit-panel__field fs-tx-edit-panel__field--type">
          <span className="fs-tx-edit-panel__label">Type</span>
          <Select
            size="middle"
            className="fs-tx-edit-panel__control"
            value={draft.txnKind}
            options={[
              { value: 'expense', label: 'Expense' },
              { value: 'income', label: 'Income' },
              { value: 'transfer', label: 'Transfer' },
            ]}
            onChange={(v) => patch({ txnKind: v })}
          />
        </label>

        <label className="fs-tx-edit-panel__field fs-tx-edit-panel__field--amount">
          <span className="fs-tx-edit-panel__label">Amount (CNY)</span>
          <InputNumber
            size="middle"
            className="fs-tx-edit-panel__control"
            min={0}
            precision={2}
            value={draft.editAmount}
            onChange={(v) => patch({ editAmount: Number(v ?? 0) })}
          />
        </label>

        <label className="fs-tx-edit-panel__field fs-tx-edit-panel__field--memo">
          <span className="fs-tx-edit-panel__label">Memo</span>
          <Input
            size="middle"
            className="fs-tx-edit-panel__control"
            value={draft.demoArea}
            placeholder="Optional note"
            onChange={(e) => patch({ demoArea: e.target.value })}
          />
        </label>

        <label className="fs-tx-edit-panel__field fs-tx-edit-panel__field--desc">
          <span className="fs-tx-edit-panel__label">Description</span>
          <Input
            size="middle"
            className="fs-tx-edit-panel__control"
            value={draft.transactionDesc}
            placeholder="What was this transaction?"
            onChange={(e) => patch({ transactionDesc: e.target.value })}
          />
        </label>

        <label className="fs-tx-edit-panel__field fs-tx-edit-panel__field--category">
          <span className="fs-tx-edit-panel__label">Category</span>
          <CategoryPicker
            treeData={treeData}
            size="middle"
            value={draft.consumeCode}
            placeholder="Search or browse categories"
            onChange={(code) => patch({ consumeCode: code })}
          />
        </label>
      </div>

      <div className="fs-tx-edit-panel__footer">
        <Space size={10}>
          <Button size="middle" onClick={onCancel} disabled={saving}>Cancel</Button>
          <Button type="primary" size="middle" loading={saving} onClick={onSave}>
            Save changes
          </Button>
        </Space>
      </div>
    </div>
  )
}
