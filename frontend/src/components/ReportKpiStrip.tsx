type Kpi = {
  key: string
  label: string
  value: string
  hint?: string
  tone?: 'income' | 'expense' | 'neutral' | 'warn'
}

export function ReportKpiStrip({ items }: { items: Kpi[] }) {
  if (!items.length) return null
  return (
    <div className="fs-report-kpi-strip">
      {items.map((k) => (
        <div key={k.key} className={`fs-report-kpi-card${k.tone ? ` fs-report-kpi-card--${k.tone}` : ''}`} title={k.hint}>
          <span className="fs-report-kpi-value">{k.value}</span>
          <span className="fs-report-kpi-label">{k.label}</span>
          {k.hint && <span className="fs-report-kpi-hint">{k.hint}</span>}
        </div>
      ))}
    </div>
  )
}
