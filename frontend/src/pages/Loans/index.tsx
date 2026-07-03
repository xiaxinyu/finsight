import { useCallback, useMemo, useState, type CSSProperties } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Badge, Button, Dropdown, Progress, Spin, Tag, Tooltip, Typography, message, type MenuProps,
} from 'antd'
import {
  BankOutlined, DeleteOutlined, EditOutlined, LinkOutlined, MoreOutlined, PlusOutlined,
} from '@ant-design/icons'
import { deleteLoan, fetchLoans, type LoanRow } from '../../api/loans'
import { listAccounts } from '../../api/finance'
import { DataPageLayout } from '../../components/DataPageLayout'
import { ContentCard } from '../../components/ContentCard'
import { EmptyState } from '../../components/EmptyState'
import { formatMoney } from '../../utils/format'
import { LoanDetailDrawer } from './LoanDetailDrawer'
import {
  bankAccent, bankInitial, formatDate, formatRate, isActiveLoan, payoffPct, repaymentLabel,
} from './loanDisplay'

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

  const { data: accounts = [] } = useQuery({
    queryKey: ['accounts'],
    queryFn: listAccounts,
  })

  const cardOptions = useMemo(
    () => accounts.map((a) => ({
      value: String(a.id ?? ''),
      label: String(a.name ?? a.id ?? ''),
    })).filter((o) => o.value),
    [accounts],
  )

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
      message.success('已删除')
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败')
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
      label: '编辑详情',
      onClick: () => openDetail(loan, 'detail'),
    },
    {
      key: 'links',
      icon: <LinkOutlined />,
      label: (
        <span>
          关联交易
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
      label: '删除',
      danger: true,
      onClick: () => {
        if (window.confirm(`确定删除 ${loan.lenderName} 的贷款记录？关联流水也会解除。`)) {
          onDelete(loan)
        }
      },
    },
  ]

  return (
    <DataPageLayout
      title="贷款"
      subtitle="管理各银行贷款 · 追踪利率与月供 · 关联账本流水"
      icon={<BankOutlined />}
      className="fs-data-page--loans"
      actions={(
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          添加贷款
        </Button>
      )}
    >
      {isError && (
        <Typography.Paragraph type="danger">
          {error instanceof Error ? error.message : '加载失败'}
        </Typography.Paragraph>
      )}

      {summary && (
        <div className="fs-loans-hero">
          <ContentCard className="fs-loans-hero-card fs-loans-hero-card--primary">
            <div className="fs-loans-hero-card__label">总剩余本金</div>
            <div className="fs-loans-hero-card__value">{formatMoney(summary.totalOutstanding ?? 0)}</div>
            <div className="fs-loans-hero-card__hint">{summary.loanCount ?? 0} 笔在贷</div>
          </ContentCard>
          <ContentCard className="fs-loans-hero-card">
            <div className="fs-loans-hero-card__label">月供合计</div>
            <div className="fs-loans-hero-card__value">{formatMoney(summary.totalMonthlyPayment ?? 0)}</div>
          </ContentCard>
          <ContentCard className="fs-loans-hero-card fs-loans-hero-card--rate">
            <div className="fs-loans-hero-card__label">加权平均利率</div>
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
            title="暂无贷款"
            description="添加银行贷款：利率、剩余本金、月供，并指定放款到账卡。"
            action={<Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>添加第一笔</Button>}
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
                        <Button type="text" size="small" icon={<MoreOutlined />} aria-label="操作" />
                      </Dropdown>
                    </div>
                  </div>

                  <div className="fs-loan-card-balance">
                    <span className="fs-loan-card-balance-label">剩余</span>
                    <span className="fs-loan-card-balance-value">
                      {formatMoney(loan.outstandingBalance ?? loan.principalAmount ?? 0)}
                    </span>
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
                      <span>月供 {formatMoney(loan.monthlyPayment)}</span>
                    )}
                    {repayLabel && <Tag className="fs-loan-card-tag">{repayLabel}</Tag>}
                    {loan.maturityDate && <span>到期 {formatDate(loan.maturityDate)}</span>}
                    {!active && <Tag>已结清</Tag>}
                  </div>

                  <div className="fs-loan-card-foot">
                    <Tooltip title={loan.disbursementCardLabel || '未设置放款卡'}>
                      <span className="fs-loan-card-card-label">
                        放款卡 · {loan.disbursementCardLabel || '—'}
                      </span>
                    </Tooltip>
                    <div className="fs-loan-card-actions" onClick={(e) => e.stopPropagation()}>
                      <Tooltip title="关联交易">
                        <Button
                          type="text"
                          size="small"
                          icon={<LinkOutlined />}
                          onClick={() => openDetail(loan, 'links')}
                        >
                          {(loan.linkCount ?? 0) > 0 ? loan.linkCount : '关联'}
                        </Button>
                      </Tooltip>
                      <Tooltip title="编辑">
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
      />
    </DataPageLayout>
  )
}
