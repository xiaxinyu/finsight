import { Input, Typography } from 'antd'
import type { TreeSelectNode } from '../hooks/useConsumeTree'
import { CategoryPicker } from './CategoryPicker'

type Props = {
  description?: string
  onDescriptionChange?: (value: string) => void
  categoryCode?: string
  onCategoryChange?: (code: string) => void
  memo?: string
  onMemoChange?: (value: string) => void
  treeData: TreeSelectNode[]
}

export function TransactionInlineEditFields({
  description,
  onDescriptionChange,
  categoryCode,
  onCategoryChange,
  memo,
  onMemoChange,
  treeData,
}: Props) {
  return (
    <div className="fs-tx-inline-edit">
      <div className="fs-tx-inline-edit__field">
        <Typography.Text type="secondary" className="fs-tx-inline-edit__label">Description</Typography.Text>
        <Input
          size="small"
          className="fs-tx-inline-edit__desc"
          value={description}
          placeholder="Transaction description"
          onChange={(e) => onDescriptionChange?.(e.target.value)}
        />
      </div>
      <div className="fs-tx-inline-edit__field">
        <Typography.Text type="secondary" className="fs-tx-inline-edit__label">Category</Typography.Text>
        <CategoryPicker
          treeData={treeData}
          size="small"
          className="fs-category-picker--compact"
          value={categoryCode ?? ''}
          placeholder="Search category…"
          onChange={onCategoryChange}
        />
      </div>
      <div className="fs-tx-inline-edit__field">
        <Typography.Text type="secondary" className="fs-tx-inline-edit__label">Memo</Typography.Text>
        <Input
          size="small"
          value={memo}
          placeholder="Optional memo"
          onChange={(e) => onMemoChange?.(e.target.value)}
        />
      </div>
    </div>
  )
}
