import { useCallback, useMemo, useState, type CSSProperties } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Badge, Button, Dropdown, Progress, Spin, Tag, Tooltip, Typography, message, type MenuProps,
} from 'antd'
import {
  BankOutlined, DeleteOutlined, EditOutlined, LinkOutlined, MoreOutlined, PlusOutlined,
} from '@ant-design/icons'
import { deleteLoan, fetchLoans, type LoanRow } from '../../api/loans'
import { listBankCards } from '../../api/transaction'
import { displayCardTitle } from '../../utils/bankCardDisplay'
import { DataPageLayout } from '../../components/DataPageLayout'
import { ContentCard } from '../../components/ContentCard'
import { EmptyState } from '../../components/EmptyState'
import { formatMoney } from '../../utils/format'
import { LoanDetailDrawer } from './LoanDetailDrawer'
import {
  bankAccent, bankInitial, formatDate, formatInstallmentProgress, formatRate, isActiveLoan,
  linkedRepaymentTotal, payoffPct, principalProgressRepaid, repaymentLabel,
} from './loanDisplay'
import { loanCopy } from './loanLabels'

type DrawerState = {
  loan: LoanRow | null
  tab: 'detail' | 'links'
}

export function LoansPage() {
  const queryClient = useQueryClient()
  const [drawer, setDrawer] = useState<DrawerState | null>(null)

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['loans'],
    queryFn: fetchLoans,
  })

  const { data: bankCards = [] } = useQuery({
    queryKey: ['bank-cards'],
    queryFn: () => listBankCards(),
  })

  const cardOptions = useMemo(
    () => bankCards.map((c) => ({
      value: String(c.id ?? ''),
      label: displayCardTitle(c),
    })).filter((o) => o.value),
    [bankCards],
  )

  const reloadCards = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['bank-cards'] })
    queryClient.invalidateQueries({ queryKey: ['accounts'] })
  }, [queryClient])

  const reload = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['loans'] })
  }, [queryClient])

  const openCreate = () => setDrawer({ loan: null, tab: 'detail' })
  const openDetail = (loan: LoanRow, tab: 'detail' | 'links' = 'detail') => setDrawer({ loan, tab })
  const closeDrawer = () => setDrawer(null)

  const onDelete = async (loan: LoanRow) => {
    if (!loan.id) return
    try {
      await deleteLoan(loan.id)
      message.success(loanCopy.deleted)
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : loanCopy.deleteFailed)
    }
  }

  const summary = data?.summary
  const loans = data?.loans ?? []

  const drawerLoan = useMemo(() => {
    if (!drawer) return null
    if (!drawer.loan?.id) return drawer.loan
    return loans.find((l) => l.id === drawer.loan?.id) ?? drawer.loan
  }, [drawer, loans])
  const maxRate = useMemo(
    () => Math.max(...loans.map((l) => l.interestRatePct ?? 0), 1),
    [loans],
  )

  const menuItems = (loan: LoanRow): MenuProps['items'] => [
    {
      key: 'edit',
      icon: <EditOutlined />,
      label: loanCopy.editDetails,
      onClick: () => openDetail(loan, 'detail'),
    },
    {
      key: 'links',
      icon: <LinkOutlined />,
      label: (
        <span>
          {loanCopy.linkTransactions}
          {(loan.linkCount ?? 0) > 0 && (
            <Badge count={loan.linkCount} size="small" style={{ marginLeft: 8 }} />
          )}
        </span>
      ),
      onClick: () => openDetail(loan, 'links'),
    },
    { type: 'divider' },
    {
      key: 'delete',
      icon: <DeleteOutlined />,
      label: loanCopy.delete,
      danger: true,
      onClick: () => {
        if (window.confirm(loanCopy.deleteConfirm(loan.lenderName ?? 'this lender'))) {
          onDelete(loan)
        }
      },
    },
  ]

  return (
    <DataPageLayout
      title={loanCopy.pageTitle}
      subtitle={loanCopy.pageSubtitle}
      icon={<BankOutlined />}
      className="fs-data-page--loans"
      actions={(
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          {loanCopy.addLoan}
        </Button>
      )}
    >
      {isError && (
        <Typography.Paragraph type="danger">
          {error instanceof Error ? error.message : loanCopy.loadFailed}
        </Typography.Paragraph>
      )}

      {summary && (
        <div className="fs-loans-hero">
          <ContentCard className="fs-loans-hero-card fs-loans-hero-card--primary">
            <div className="fs-loans-hero-card__label">{loanCopy.totalOutstanding}</div>
            <div className="fs-loans-hero-card__value">{formatMoney(summary.totalOutstanding ?? 0)}</div>
            <div className="fs-loans-hero-card__hint">{loanCopy.activeLoans(summary.loanCount ?? 0)}</div>
          </ContentCard>
          <ContentCard className="fs-loans-hero-card">
            <div className="fs-loans-hero-card__label">{loanCopy.totalMonthly}</div>
            <div className="fs-loans-hero-card__value">{formatMoney(summary.totalMonthlyPayment ?? 0)}</div>
          </ContentCard>
          <ContentCard className="fs-loans-hero-card fs-loans-hero-card--rate">
            <div className="fs-loans-hero-card__label">{loanCopy.weightedAvgRate}</div>
            <div className="fs-loans-hero-card__value fs-loans-hero-card__value--rate">
              {formatRate(summary.weightedAvgRatePct)}
            </div>
          </ContentCard>
        </div>
      )}

      {isLoading ? (
        <div className="fs-loans-loading"><Spin size="large" /></div>
      ) : loans.length === 0 ? (
        <ContentCard>
          <EmptyState
            title={loanCopy.noLoans}
            description={loanCopy.noLoansHint}
            action={<Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>{loanCopy.addFirstLoan}</Button>}
          />
        </ContentCard>
      ) : (
        <div className="fs-loan-grid">
          {loans.map((loan) => {
            const accent = bankAccent(loan.lenderBankCode, loan.lenderName)
            const active = isActiveLoan(loan)
            const paidPct = payoffPct(loan)
            const ratePct = loan.interestRatePct ?? 0
            const rateBar = maxRate > 0 ? Math.round((ratePct / maxRate) * 100) : 0
            const repayLabel = repaymentLabel(loan.repaymentMethod)
            const totalPrincipal = loan.principalAmount ?? 0
            const remaining = loan.outstandingBalance ?? totalPrincipal
            const repaidFlow = linkedRepaymentTotal(loan)
            const principalRepaid = principalProgressRepaid(loan)
            const installmentText = formatInstallmentProgress(loan)

            return (
              <ContentCard
                key={loan.id}
                className={`fs-loan-card${active ? '' : ' fs-loan-card--closed'}`}
                styles={{ body: { padding: 0 } }}
              >
                <div
                  className="fs-loan-card-inner"
                  style={{ '--loan-accent': accent } as CSSProperties}
                  onClick={() => openDetail(loan, 'detail')}
                  onKeyDown={(e) => e.key === 'Enter' && openDetail(loan, 'detail')}
                  role="button"
                  tabIndex={0}
                >
                  <div className="fs-loan-card-head">
                    <div className="fs-loan-card-bank">
                      <div className="fs-loan-card-avatar">{bankInitial(loan.lenderName)}</div>
                      <div>
                        <div className="fs-loan-card-lender">{loan.lenderName}</div>
                        {loan.name && <div className="fs-loan-card-alias">{loan.name}</div>}
                      </div>
                    </div>
                    <div className="fs-loan-card-head-right" onClick={(e) => e.stopPropagation()}>
                      <span className="fs-loan-card-rate">{formatRate(ratePct)}</span>
                      <Dropdown menu={{ items: menuItems(loan) }} trigger={['click']}>
                        <Button type="text" size="small" icon={<MoreOutlined />} aria-label={loanCopy.actions} />
                      </Dropdown>
                    </div>
                  </div>

                  <div className="fs-loan-card-stats">
                    <div className="fs-loan-card-stat">
                      <span className="fs-loan-card-stat-label">{loanCopy.totalPrincipal}</span>
                      <span className="fs-loan-card-stat-value">{formatMoney(totalPrincipal)}</span>
                    </div>
                    <div className="fs-loan-card-stat fs-loan-card-stat--primary">
                      <span className="fs-loan-card-stat-label">{loanCopy.remaining}</span>
                      <span className="fs-loan-card-stat-value">{formatMoney(remaining)}</span>
                    </div>
                    <div className="fs-loan-card-stat">
                      <span className="fs-loan-card-stat-label">{loanCopy.principalProgress}</span>
                      <span className="fs-loan-card-stat-value fs-loan-card-stat-value--paid">
                        {formatMoney(principalRepaid)}
                      </span>
                    </div>
                    <div className="fs-loan-card-stat">
                      <span className="fs-loan-card-stat-label">{loanCopy.linkedRepayments}</span>
                      <span className="fs-loan-card-stat-value">{formatMoney(repaidFlow)}</span>
                      {(loan.linkedRepaymentCount ?? 0) > 0 && (
                        <span className="fs-loan-card-stat-hint">
                          {loanCopy.linkedRepaymentHint(loan.linkedRepaymentCount ?? 0)}
                        </span>
                      )}
                    </div>
                    <div className="fs-loan-card-stat">
                      <span className="fs-loan-card-stat-label">{loanCopy.installments}</span>
                      <span className="fs-loan-card-stat-value">{installmentText}</span>
                      <span className="fs-loan-card-stat-hint">{loanCopy.installmentHint}</span>
                    </div>
                  </div>

                  <div className="fs-loan-card-ratebar" aria-hidden>
                    <div className="fs-loan-card-ratebar-fill" style={{ width: `${rateBar}%` }} />
                  </div>

                  {paidPct > 0 && (
                    <Progress
                      percent={paidPct}
                      size="small"
                      showInfo={false}
                      strokeColor={accent}
                      className="fs-loan-card-progress"
                    />
                  )}

                  <div className="fs-loan-card-meta">
                    {loan.monthlyPayment != null && (
                      <span>{loanCopy.monthly} {formatMoney(loan.monthlyPayment)}</span>
                    )}
                    {repayLabel && <Tag className="fs-loan-card-tag">{repayLabel}</Tag>}
                    {loan.maturityDate && <span>{loanCopy.maturity} {formatDate(loan.maturityDate)}</span>}
                    {!active && <Tag>{loanCopy.closed}</Tag>}
                  </div>

                  <div className="fs-loan-card-foot">
                    <Tooltip title={loan.disbursementCardLabel || 'Disbursement card not set'}>
                      <span className="fs-loan-card-card-label">
                        {loanCopy.disbursementCard} · {loan.disbursementCardLabel || '—'}
                      </span>
                    </Tooltip>
                    <div className="fs-loan-card-actions" onClick={(e) => e.stopPropagation()}>
                      <Tooltip title={loanCopy.linkTransactions}>
                        <Button
                          type="text"
                          size="small"
                          icon={<LinkOutlined />}
                          onClick={() => openDetail(loan, 'links')}
                        >
                          {(loan.linkCount ?? 0) > 0 ? loan.linkCount : loanCopy.link}
                        </Button>
                      </Tooltip>
                      <Tooltip title={loanCopy.edit}>
                        <Button
                          type="text"
                          size="small"
                          icon={<EditOutlined />}
                          onClick={() => openDetail(loan, 'detail')}
                        />
                      </Tooltip>
                    </div>
                  </div>
                </div>
              </ContentCard>
            )
          })}
        </div>
      )}

      <LoanDetailDrawer
        loan={drawerLoan}
        open={!!drawer}
        initialTab={drawer?.tab ?? 'detail'}
        cardOptions={cardOptions}
        onClose={closeDrawer}
        onSaved={reload}
        onCardsChanged={reloadCards}
      />
    </DataPageLayout>
  )
}
