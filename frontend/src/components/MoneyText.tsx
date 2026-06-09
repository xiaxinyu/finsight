import { Typography } from 'antd'
import { formatNumber } from '../utils/format'

type Props = {
  value?: number | null
  type?: 'income' | 'expense' | 'neutral'
  unit?: boolean
}

export function MoneyText({ value, type = 'neutral' }: Props) {
  const color = type === 'income' ? '#10b981' : type === 'expense' ? '#f59e0b' : undefined
  const text = formatNumber(value)
  return (
    <Typography.Text style={{ color, fontVariantNumeric: 'tabular-nums' }} className="fs-money">
      {text}
    </Typography.Text>
  )
}
