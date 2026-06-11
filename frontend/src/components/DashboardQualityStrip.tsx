import { Link } from 'react-router-dom'

type Props = {
  unclassified: number
  duplicateExcess: number
  duplicateGroups: number
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
  duplicateExcess,
  duplicateGroups,
  transfers,
  dataTrustPct,
}: Props) {
  return (
    <div className="fs-dash-quality-strip">
      <QualityPill
        label="Data trust"
        value={`${dataTrustPct}%`}
        tone={dataTrustPct >= 85 ? 'ok' : dataTrustPct >= 60 ? 'default' : 'warn'}
        hint="Based on classified rows and duplicate-free ledger"
      />
      <QualityPill
        label="Unclassified"
        value={unclassified}
        tone={unclassified > 0 ? 'warn' : 'ok'}
        to="/transactions?unclassified=1"
      />
      <QualityPill
        label="Duplicate rows"
        value={duplicateExcess}
        tone={duplicateExcess > 0 ? 'warn' : 'ok'}
        hint={duplicateGroups > 0 ? `${duplicateGroups} fingerprint groups` : undefined}
        to="/transactions"
      />
      <QualityPill label="Transfer pairs" value={transfers} />
    </div>
  )
}
