import { Alert, Progress, Tag } from 'antd'
import type { CSSProperties } from 'react'
import type { LoanRow } from '../../api/loans'
import {
  bankAccent, bankInitial, estimatedInterestInRepayments, formatDate, formatInstallmentProgress, formatRate,
  linkedInterestTotal, linkedPrincipalFromFlow, linkedRepaymentTotal, paidInstallmentCount, principalProgressRepaid,
  repaymentLabel,
} from './loanDisplay'
import { formatMoney } from '../../utils/format'

type Props = {
  loan: LoanRow
}

export function LoanDrawerHero({ loan }: Props) {
  const accent = bankAccent(loan.lenderBankCode, loan.lenderName)
  const principal = loan.principalAmount ?? 0
  const remaining = loan.outstandingBalance ?? principal
  const repaidFlow = linkedRepaymentTotal(loan)
  const interestLinked = linkedInterestTotal(loan)
  const principalFromFlow = linkedPrincipalFromFlow(loan)
  const principalRepaid = principalProgressRepaid(loan)
  const paidPeriods = paidInstallmentCount(loan)
  const termMonths = loan.termMonths
  const installmentPct = termMonths && termMonths > 0
    ? Math.min(100, Math.round((paidPeriods / termMonths) * 100))
    : 0
  const repayLabel = repaymentLabel(loan.repaymentMethod)
  const interestEstimate = estimatedInterestInRepayments(loan)

  const metrics = [
    { label: 'Total principal', value: formatMoney(principal), primary: false },
    { label: 'Remaining', value: formatMoney(remaining), primary: true },
    { label: 'Principal progress', value: formatMoney(principalRepaid), accent: true, hint: 'Total − remaining' },
    { label: 'Linked repayments', value: formatMoney(repaidFlow), hint: repaidFlow > 0 ? 'Flow total (incl. interest)' : undefined },
    ...(interestLinked > 0 ? [{ label: 'Linked interest', value: formatMoney(interestLinked), hint: 'INTEREST links' }] : []),
    ...(principalFromFlow > 0 && interestLinked > 0
      ? [{ label: 'Principal (flow)', value: formatMoney(principalFromFlow), hint: 'Repayment − interest links' }]
      : []),
    { label: 'Installments', value: formatInstallmentProgress(loan), hint: '> ¥100 counts as 1' },
    { label: 'APR', value: formatRate(loan.interestRatePct), rate: true },
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
            {loan.status === 'CLOSED' && <Tag>Closed</Tag>}
            {(loan.linkCount ?? 0) > 0 && (
              <Tag color="blue">{loan.linkCount} linked</Tag>
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

      {interestEstimate > 100 && (
        <Alert
          type="info"
          showIcon
          className="fs-loan-drawer-reconcile-hint"
          message="Repayment flow includes interest"
          description={
            interestLinked > 0
              ? `Repayment links ${formatMoney(repaidFlow)}; ${formatMoney(interestLinked)} tagged as interest; principal progress ${formatMoney(principalRepaid)} from total − remaining.`
              : `Flow total ${formatMoney(repaidFlow)} vs principal progress ${formatMoney(principalRepaid)} — about ${formatMoney(interestEstimate)} may be interest/fees. Mark INTEREST links for a precise split.`
          }
        />
      )}

      {(termMonths != null && termMonths > 0) && (
        <div className="fs-loan-drawer-progress-block">
          <div className="fs-loan-drawer-progress-head">
            <span>Installment progress</span>
            <span>{paidPeriods} / {termMonths} 期 ({installmentPct}%)</span>
          </div>
          <Progress percent={installmentPct} showInfo={false} strokeColor={accent} size="small" />
        </div>
      )}

      {principalRepaid > 0 && principal > 0 && (
        <div className="fs-loan-drawer-progress-block fs-loan-drawer-progress-block--muted">
          <div className="fs-loan-drawer-progress-head">
            <span>Principal payoff</span>
            <span>{Math.min(100, Math.round((principalRepaid / principal) * 100))}%</span>
          </div>
          <Progress
            percent={Math.min(100, Math.round((principalRepaid / principal) * 100))}
            showInfo={false}
            strokeColor="#059669"
            size="small"
          />
        </div>
      )}

      <div className="fs-loan-drawer-meta-row">
        {loan.monthlyPayment != null && <span>Monthly {formatMoney(loan.monthlyPayment)}</span>}
        {loan.disbursementCardLabel && (
          <span>Disbursement · {loan.disbursementCardLabel}</span>
        )}
        {loan.repaymentCardLabel && loan.repaymentCardLabel !== loan.disbursementCardLabel && (
          <span>Repayment · {loan.repaymentCardLabel}</span>
        )}
        {loan.maturityDate && <span>Maturity {formatDate(loan.maturityDate)}</span>}
      </div>
    </div>
  )
}
