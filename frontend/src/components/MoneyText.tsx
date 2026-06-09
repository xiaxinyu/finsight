import { Typography } from 'antd'
import { formatNumber } from '../utils/format'
import { finsightColors } from '../styles/finsight-tokens'

type Props = {
  value?: number | null
  type?: 'income' | 'expense' | 'neutral'
  unit?: boolean
}

export function MoneyText({ value, type = 'neutral' }: Props) {
  const color = type === 'income' ? finsightColors.income : type === 'expense' ? finsightColors.expense : undefined
  const text = formatNumber(value)
  return (
    <Typography.Text style={{ color, fontVariantNumeric: 'tabular-nums' }} className="fs-money">
      {text}
    </Typography.Text>
  )
}

/** Infer income/expense from txn type or amount sign. */
export function moneyTypeFromRow(txnType?: string, amount?: number | null): 'income' | 'expense' | 'neutral' {
  if (txnType === 'income') return 'income'
  if (txnType === 'expense') return 'expense'
  const n = Number(amount)
  if (n > 0) return 'income'
  if (n < 0) return 'expense'
  return 'neutral'
}
