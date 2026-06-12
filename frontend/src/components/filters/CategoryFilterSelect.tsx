import { CategoryPicker } from '../CategoryPicker'

type Props = {
  value: string
  onChange: (categoryId: string) => void
  disabled?: boolean
  txnType?: string
}

export function CategoryFilterSelect({ value, onChange, disabled }: Props) {
  return (
    <CategoryPicker
      className="fs-filter-control fs-filter-control--category"
      size="small"
      largePopup={false}
      value={value}
      placeholder="Category"
      disabled={disabled}
      onChange={onChange}
    />
  )
}
