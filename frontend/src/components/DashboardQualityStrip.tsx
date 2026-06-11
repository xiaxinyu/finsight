import { Link } from 'react-router-dom'

type Props = {
  unclassified: number
  transfers: number
  dataTrustPct: number
}

function QualityPill({
  label,
  value,
  tone,
  hint,
  to,
}: {
  label: string
  value: string | number
  tone?: 'warn' | 'ok' | 'default'
  hint?: string
  to?: string
}) {
  const body = (
    <div className={`fs-dash-quality-pill fs-dash-quality-pill--${tone || 'default'}`} title={hint}>
      <span className="fs-dash-quality-value">{value}</span>
      <span className="fs-dash-quality-label">{label}</span>
    </div>
  )
  if (to) {
    return <Link to={to} className="fs-dash-quality-link">{body}</Link>
  }
  return body
}

export function DashboardQualityStrip({
  unclassified,
  transfers,
  dataTrustPct,
}: Props) {
  return (
    <div className="fs-dash-quality-strip">
      <QualityPill
        label="Data trust"
        value={`${dataTrustPct}%`}
        tone={dataTrustPct >= 85 ? 'ok' : dataTrustPct >= 60 ? 'default' : 'warn'}
        hint="Based on category coverage"
      />
      <QualityPill
        label="Unclassified"
        value={unclassified}
        tone={unclassified > 0 ? 'warn' : 'ok'}
        to="/transactions?unclassified=1"
      />
      <QualityPill label="Transfer pairs" value={transfers} />
    </div>
  )
}
