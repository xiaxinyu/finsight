import { Tag, Tooltip } from 'antd'
import { ArrowDownOutlined, ArrowUpOutlined, MinusOutlined } from '@ant-design/icons'
import { formatMoney, formatNumber } from '../utils/format'
import {
  deltaTone,
  deltaToneClass,
  formatDeltaPercent,
  type ForecastKind,
} from '../utils/fsTableCells'

export function DeltaPercentCell({
  value,
  amount,
  expenseContext = true,
}: {
  value: number
  amount?: number | null
  expenseContext?: boolean
}) {
  const tone = deltaTone(value, expenseContext)
  const icon = tone === 'adverse'
    ? <ArrowUpOutlined />
    : tone === 'favorable'
      ? <ArrowDownOutlined />
      : <MinusOutlined />
  return (
    <span className={`fs-table-cell-delta ${deltaToneClass(tone)}`}>
      <span className="fs-table-cell-delta-icon" aria-hidden>{icon}</span>
      {formatDeltaPercent(value, amount)}
    </span>
  )
}

export function DeltaMoneyCell({ value, expenseContext = true }: { value: number; expenseContext?: boolean }) {
  const n = Number(value) || 0
  const tone = deltaTone(n, expenseContext)
  const icon = tone === 'adverse'
    ? <ArrowUpOutlined />
    : tone === 'favorable'
      ? <ArrowDownOutlined />
      : <MinusOutlined />
  return (
    <span className={`fs-table-cell-delta ${deltaToneClass(tone)}`}>
      <span className="fs-table-cell-delta-icon" aria-hidden>{icon}</span>
      {n >= 0 ? '+' : ''}{formatMoney(n)}
    </span>
  )
}

export function MoneyCell({
  value,
  signed = false,
  unit,
}: {
  value: number
  signed?: boolean
  unit?: string
}) {
  const n = Number(value) || 0
  const tone = signed ? deltaTone(n, false) : 'neutral'
  const prefix = unit === 'CNY' ? '¥' : unit === 'USD' ? '$' : ''
  const text = prefix ? `${prefix}${formatNumber(n)}` : formatNumber(n)
  return (
    <span className={`fs-money fs-table-cell-money${signed ? ` ${deltaToneClass(tone)}` : ''}`}>
      {text}
    </span>
  )
}

export function ContributionBar({ value, max = 100 }: { value: number; max?: number }) {
  const pct = Math.max(0, Math.min(100, (Number(value) / max) * 100))
  return (
    <div className="fs-table-contribution">
      <div className="fs-table-contribution-track">
        <div className="fs-table-contribution-fill" style={{ width: `${pct}%` }} />
      </div>
      <span className="fs-table-contribution-label">{Number(value).toFixed(1)}%</span>
    </div>
  )
}

export function RiskTag({ level }: { level?: string | null }) {
  const normalized = String(level || 'low').toLowerCase()
  const color = normalized === 'high' ? 'red' : normalized === 'medium' ? 'orange' : 'green'
  const label = normalized.charAt(0).toUpperCase() + normalized.slice(1)
  return <Tag color={color} className="fs-table-risk-tag">{label}</Tag>
}

export function ForecastTag({ kind }: { kind: ForecastKind }) {
  switch (kind) {
    case 'actual':
      return <Tag className="fs-table-forecast-tag fs-table-forecast-tag--actual">Actual</Tag>
    case 'forecast':
      return <Tag color="blue" className="fs-table-forecast-tag fs-table-forecast-tag--forecast">Forecast</Tag>
    case 'budget':
      return <Tag color="purple" className="fs-table-forecast-tag fs-table-forecast-tag--budget">Budget</Tag>
    case 'band':
      return <Tag className="fs-table-forecast-tag fs-table-forecast-tag--band">Band</Tag>
    default:
      return null
  }
}

export function RowExplanationHint({ text }: { text?: string | null }) {
  if (!text?.trim()) return null
  return (
    <Tooltip title={text}>
      <span className="fs-table-row-hint" aria-label="Why this row matters">i</span>
    </Tooltip>
  )
}
