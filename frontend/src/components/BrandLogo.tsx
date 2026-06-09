import { finsightColors } from '../styles/finsight-tokens'

type Props = {
  collapsed?: boolean
  variant?: 'light' | 'dark'
}

export function LogoMark({ size = 28 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" fill="none" aria-hidden>
      <rect width="32" height="32" rx="8" fill={finsightColors.primary} />
      <path d="M8 22V10h3.2v4.8L16 10h3.6l-5.2 5.6L20 22h-3.6l-3.4-4.4L11.2 22H8z" fill="#fff" />
      <path d="M22 10v12h-2.4V10H22z" fill="#93c5fd" />
    </svg>
  )
}

export function BrandLogo({ collapsed = false, variant = 'dark' }: Props) {
  const textColor = variant === 'dark' ? '#fff' : finsightColors.text
  const subColor = variant === 'dark' ? '#94a3b8' : finsightColors.textSecondary

  return (
    <div className="fs-brand-logo" style={{ display: 'flex', alignItems: 'center', gap: 10, padding: collapsed ? '12px 8px' : '12px 14px', minHeight: 48 }}>
      <LogoMark size={collapsed ? 24 : 28} />
      {!collapsed && (
        <div style={{ lineHeight: 1.15 }}>
          <div style={{ color: textColor, fontWeight: 700, fontSize: 15, letterSpacing: '-0.02em' }}>FinSight</div>
          <div style={{ color: subColor, fontSize: 10, marginTop: 2 }}>Finance Intelligence</div>
        </div>
      )}
    </div>
  )
}
