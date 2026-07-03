import { Progress, Tag } from 'antd'
import type { CSSProperties } from 'react'
import type { LoanRow } from '../../api/loans'
import {
  bankAccent, bankInitial, formatDate, formatInstallmentProgress, formatRate,
  linkedRepaymentTotal, paidInstallmentCount, payoffPct, repaymentLabel,
} from './loanDisplay'
import { formatMoney } from '../../utils/format'

type Props = {
  loan: LoanRow
}

export function LoanDrawerHero({ loan }: Props) {
  const accent = bankAccent(loan.lenderBankCode, loan.lenderName)
  const principal = loan.principalAmount ?? 0
  const remaining = loan.outstandingBalance ?? principal
  const repaidAmount = linkedRepaymentTotal(loan)
  const paidPeriods = paidInstallmentCount(loan)
  const termMonths = loan.termMonths
  const installmentPct = termMonths && termMonths > 0
    ? Math.min(100, Math.round((paidPeriods / termMonths) * 100))
    : 0
  const repayLabel = repaymentLabel(loan.repaymentMethod)

  const metrics = [
    { label: '总贷款', value: formatMoney(principal), primary: false },
    { label: '剩余本金', value: formatMoney(remaining), primary: true },
    { label: '已还金额', value: formatMoney(repaidAmount), accent: true },
    { label: '已还期数', value: formatInstallmentProgress(loan), hint: '单笔 > ¥100 计 1 期' },
    { label: '年利率', value: formatRate(loan.interestRatePct), rate: true },
    { label: '月供', value: loan.monthlyPayment != null ? formatMoney(loan.monthlyPayment) : '—' },
  ]

  return (
    <div className="fs-loan-drawer-hero" style={{ '--loan-accent': accent } as CSSProperties}>
      <div className="fs-loan-drawer-hero-head">
        <div className="fs-loan-drawer-hero-badge">{bankInitial(loan.lenderName)}</div>
        <div className="fs-loan-drawer-hero-head-text">
          <div className="fs-loan-drawer-hero-title">{loan.lenderName}</div>
          {loan.name && <div className="fs-loan-drawer-hero-subtitle">{loan.name}</div>}
          <div className="fs-loan-drawer-hero-tags">
            {repayLabel && <Tag className="fs-loan-drawer-hero-tag">{repayLabel}</Tag>}
            {loan.status === 'CLOSED' && <Tag>已结清</Tag>}
            {(loan.linkCount ?? 0) > 0 && (
              <Tag color="blue">{loan.linkCount} 笔关联</Tag>
            )}
          </div>
        </div>
      </div>

      <div className="fs-loan-drawer-metrics">
        {metrics.map((m) => (
          <div
            key={m.label}
            className={`fs-loan-drawer-metric${m.primary ? ' fs-loan-drawer-metric--primary' : ''}${m.accent ? ' fs-loan-drawer-metric--accent' : ''}${m.rate ? ' fs-loan-drawer-metric--rate' : ''}`}
          >
            <span className="fs-loan-drawer-metric-label">{m.label}</span>
            <span className="fs-loan-drawer-metric-value">{m.value}</span>
            {m.hint && <span className="fs-loan-drawer-metric-hint">{m.hint}</span>}
          </div>
        ))}
      </div>

      {(termMonths != null && termMonths > 0) && (
        <div className="fs-loan-drawer-progress-block">
          <div className="fs-loan-drawer-progress-head">
            <span>还款进度</span>
            <span>{paidPeriods} / {termMonths} 期 ({installmentPct}%)</span>
          </div>
          <Progress percent={installmentPct} showInfo={false} strokeColor={accent} size="small" />
        </div>
      )}

      {payoffPct(loan) > 0 && (
        <div className="fs-loan-drawer-progress-block fs-loan-drawer-progress-block--muted">
          <div className="fs-loan-drawer-progress-head">
            <span>已还金额 / 总贷款</span>
            <span>{payoffPct(loan)}%</span>
          </div>
          <Progress percent={payoffPct(loan)} showInfo={false} strokeColor="#059669" size="small" />
        </div>
      )}

      <div className="fs-loan-drawer-meta-row">
        {loan.disbursementCardLabel && (
          <span>放款卡 · {loan.disbursementCardLabel}</span>
        )}
        {loan.repaymentCardLabel && loan.repaymentCardLabel !== loan.disbursementCardLabel && (
          <span>还款卡 · {loan.repaymentCardLabel}</span>
        )}
        {loan.maturityDate && <span>到期 {formatDate(loan.maturityDate)}</span>}
      </div>
    </div>
  )
}
