import { useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Button, Empty, Input, Popconfirm, Segmented, Spin, Tag, Tooltip, Typography, message,
} from 'antd'
import {
  CreditCardOutlined, DeleteOutlined, InfoCircleOutlined,
  LinkOutlined, SearchOutlined,
} from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import {
  LOAN_LINK_TYPE_LABELS,
  addLoanLink,
  fetchLoanLinks,
  removeLoanLink,
  type LoanLinkType,
  type LoanRow,
  type LoanTxnLinkRow,
} from '../../api/loans'
import { listTransactions, type TransactionRow } from '../../api/transaction'
import { formatMoney } from '../../utils/format'
import { MIN_REPAYMENT_INSTALLMENT, formatDate, paidInstallmentCount } from './loanDisplay'

type Props = {
  loan: LoanRow
  onLinksChanged?: () => void
}

type DatePreset = '3m' | '6m' | '1y' | 'all'

const DATE_PRESETS: { label: string; value: DatePreset }[] = [
  { label: '近3月', value: '3m' },
  { label: '近6月', value: '6m' },
  { label: '近1年', value: '1y' },
  { label: '全部', value: 'all' },
]

function presetRange(preset: DatePreset): [Dayjs, Dayjs] {
  const end = dayjs()
  if (preset === '3m') return [end.subtract(3, 'month'), end]
  if (preset === '6m') return [end.subtract(6, 'month'), end]
  if (preset === '1y') return [end.subtract(1, 'year'), end]
  return [dayjs('2010-01-01'), end]
}

function linkAmount(row: LoanTxnLinkRow): number | null {
  if (row.incomeMoney != null && row.incomeMoney > 0) return row.incomeMoney
  if (row.expenseAmount != null && row.expenseAmount !== 0) return -Math.abs(row.expenseAmount)
  return null
}

function absLinkAmount(row: LoanTxnLinkRow): number {
  const amt = linkAmount(row)
  return amt == null ? 0 : Math.abs(amt)
}

function qualifiesInstallment(row: LoanTxnLinkRow): boolean {
  return row.linkType === 'REPAYMENT' && absLinkAmount(row) > MIN_REPAYMENT_INSTALLMENT
}

function txnSignedAmount(row: TransactionRow): number {
  if (row.incomeMoney != null && row.incomeMoney > 0) return row.incomeMoney
  if (row.balanceMoney != null && row.balanceMoney < 0) return Math.abs(row.balanceMoney)
  if (row.balanceMoney != null && row.balanceMoney > 0) return -row.balanceMoney
  return 0
}

function linkTypeTagColor(type?: LoanLinkType) {
  if (type === 'DISBURSEMENT') return 'success'
  if (type === 'REPAYMENT') return 'processing'
  if (type === 'INTEREST') return 'warning'
  return 'default'
}

function LinkTxnRow({
  row, linked, linking, onLink, onUnlink,
}: {
  row: LoanTxnLinkRow | TransactionRow
  linked: boolean
  linking?: boolean
  onLink?: () => void
  onUnlink?: () => void
}) {
  const isLinkRow = 'linkType' in row
  const amt = isLinkRow ? linkAmount(row as LoanTxnLinkRow) : txnSignedAmount(row as TransactionRow)
  const positive = amt != null && amt > 0
  const absAmt = amt == null ? 0 : Math.abs(amt)
  const linkRow = isLinkRow ? row as LoanTxnLinkRow : null
  const desc = row.transactionDesc || ('id' in row ? row.id : row.transactionId)
  const date = row.transactionDate
  const cardName = isLinkRow ? linkRow?.bankCardName : (row as TransactionRow).bankCardName

  return (
    <li className={`fs-loan-txn-item${linked ? ' fs-loan-txn-item--linked' : ''}${linkRow && qualifiesInstallment(linkRow) ? ' fs-loan-txn-item--installment' : ''}`}>
      <div className="fs-loan-txn-item-main">
        <div className="fs-loan-txn-item-top">
          <span className="fs-loan-txn-date">{formatDate(date)}</span>
          {linkRow?.linkType && (
            <Tag color={linkTypeTagColor(linkRow.linkType)} className="fs-loan-txn-tag">
              {LOAN_LINK_TYPE_LABELS[linkRow.linkType]}
            </Tag>
          )}
          {linkRow?.linkType === 'REPAYMENT' && absAmt > 0 && absAmt <= MIN_REPAYMENT_INSTALLMENT && (
            <Tooltip title={`金额 ≤ ¥${MIN_REPAYMENT_INSTALLMENT}，不计入已还期数`}>
              <Tag className="fs-loan-txn-tag fs-loan-txn-tag--skip">不计期</Tag>
            </Tooltip>
          )}
          {linkRow?.linkType === 'REPAYMENT' && qualifiesInstallment(linkRow) && (
            <Tag color="green" className="fs-loan-txn-tag">计 1 期</Tag>
          )}
          <span className={`fs-loan-txn-amt ${positive ? 'fs-loan-txn-amt--in' : 'fs-loan-txn-amt--out'}`}>
            {amt == null ? '—' : formatMoney(amt)}
          </span>
        </div>
        <div className="fs-loan-txn-desc" title={desc}>{desc}</div>
        {cardName && (
          <div className="fs-loan-txn-meta">
            <CreditCardOutlined /> {cardName}
          </div>
        )}
      </div>
      {linked ? (
        <Popconfirm title="解除关联？" onConfirm={onUnlink}>
          <Button type="text" size="small" danger icon={<DeleteOutlined />} aria-label="解除关联" />
        </Popconfirm>
      ) : (
        <Button
          type="primary"
          size="small"
          ghost
          icon={<LinkOutlined />}
          loading={linking}
          onClick={onLink}
        >
          关联
        </Button>
      )}
    </li>
  )
}

export function LoanTxnLinkPanel({ loan, onLinksChanged }: Props) {
  const queryClient = useQueryClient()
  const loanId = loan.id ?? ''
  const [linkType, setLinkType] = useState<LoanLinkType>('REPAYMENT')
  const [datePreset, setDatePreset] = useState<DatePreset>('all')
  const [search, setSearch] = useState('')
  const [linkingId, setLinkingId] = useState<string>()

  const dateRange = useMemo(() => presetRange(datePreset), [datePreset])

  const activeCardId = useMemo(() => (
    linkType === 'DISBURSEMENT'
      ? loan.disbursementCardId
      : (loan.repaymentCardId || loan.disbursementCardId)
  ), [linkType, loan.disbursementCardId, loan.repaymentCardId])

  const activeCardLabel = useMemo(() => (
    linkType === 'DISBURSEMENT'
      ? loan.disbursementCardLabel
      : (loan.repaymentCardLabel || loan.disbursementCardLabel)
  ), [linkType, loan.disbursementCardLabel, loan.repaymentCardLabel])

  const { data: links = [], isLoading: linksLoading } = useQuery({
    queryKey: ['loan-links', loanId],
    queryFn: () => fetchLoanLinks(loanId),
    enabled: !!loanId,
  })

  const linkedTxnIds = useMemo(
    () => new Set(links.map((l) => l.transactionId).filter(Boolean)),
    [links],
  )

  const { data: txnCandidates = [], isLoading: txnLoading } = useQuery({
    queryKey: [
      'loan-link-candidates', loanId, linkType, datePreset, activeCardId,
      [...linkedTxnIds].join(','),
    ],
    queryFn: async () => {
      const [start, end] = dateRange
      if (!activeCardId) return []
      const res = await listTransactions({
        page: 1,
        rows: 150,
        transactionDateStartStr: start.format('YYYY-MM-DD'),
        transactionDateEndStr: end.format('YYYY-MM-DD'),
        cardId: activeCardId,
        strictCard: '1',
        sortField: 'transactionDate',
        sortOrder: 'desc',
      })
      return (res.rows ?? []).filter((t) => {
        if (!t.id || linkedTxnIds.has(t.id)) return false
        return !t.bankCardId || t.bankCardId === activeCardId
      })
    },
    enabled: !!loanId && !!activeCardId,
  })

  const filteredCandidates = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return txnCandidates
    return txnCandidates.filter((t) =>
      (t.transactionDesc ?? '').toLowerCase().includes(q)
      || (t.bankCardName ?? '').toLowerCase().includes(q)
      || String(t.id).includes(q),
    )
  }, [txnCandidates, search])

  const linkSummary = useMemo(() => {
    let disbursement = 0
    let repaymentAll = 0
    let repaymentQualifying = 0
    let qualifyingCount = 0
    let interest = 0
    let disbursementCount = 0
    let repaymentCount = 0
    for (const l of links) {
      const amt = linkAmount(l)
      const abs = amt == null ? 0 : Math.abs(amt)
      if (l.linkType === 'DISBURSEMENT') {
        disbursementCount += 1
        if (amt != null && amt > 0) disbursement += amt
      } else if (l.linkType === 'INTEREST') {
        if (amt != null) interest += abs
      } else {
        repaymentCount += 1
        repaymentAll += abs
        if (qualifiesInstallment(l)) {
          qualifyingCount += 1
          repaymentQualifying += abs
        }
      }
    }
    return {
      disbursement, repaymentAll, repaymentQualifying, interest, count: links.length,
      qualifyingCount, disbursementCount, repaymentCount,
    }
  }, [links])

  const reload = () => {
    queryClient.invalidateQueries({ queryKey: ['loan-links', loanId] })
    queryClient.invalidateQueries({ queryKey: ['loan-link-candidates', loanId] })
    queryClient.invalidateQueries({ queryKey: ['loans'] })
    onLinksChanged?.()
  }

  const onLinkTxn = async (txnId: string) => {
    setLinkingId(txnId)
    try {
      await addLoanLink(loanId, txnId, linkType)
      message.success('已关联')
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '关联失败')
    } finally {
      setLinkingId(undefined)
    }
  }

  const onUnlink = async (txnId: string) => {
    try {
      await removeLoanLink(loanId, txnId)
      message.success('已解除关联')
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '解除失败')
    }
  }

  const hasCard = !!activeCardId
  const serverPaidPeriods = paidInstallmentCount(loan)

  return (
    <div className="fs-loan-link-panel">
      <div className="fs-loan-link-info-bar">
        <InfoCircleOutlined className="fs-loan-link-info-bar-icon" />
        <span>关联流水用于追溯放款/还款；已还金额与期数由还款关联自动统计（单笔 &gt; ¥{MIN_REPAYMENT_INSTALLMENT} 计 1 期），不会修改剩余本金。</span>
      </div>

      {hasCard && (
        <div className="fs-loan-link-card-chip">
          <CreditCardOutlined />
          <span>
            当前筛选：<strong>{activeCardLabel || activeCardId}</strong>
            · {linkType === 'DISBURSEMENT' ? '放款卡' : '还款卡'}流水
          </span>
        </div>
      )}

      <div className="fs-loan-link-dashboard">
        <div className="fs-loan-link-dash-card">
          <span className="fs-loan-link-dash-label">已关联</span>
          <strong className="fs-loan-link-dash-value">{linkSummary.count}</strong>
          <span className="fs-loan-link-dash-sub">笔流水</span>
        </div>
        <div className="fs-loan-link-dash-card fs-loan-link-dash-card--period">
          <span className="fs-loan-link-dash-label">有效还款期数</span>
          <strong className="fs-loan-link-dash-value">{serverPaidPeriods}</strong>
          <span className="fs-loan-link-dash-sub">
            {linkSummary.repaymentCount > serverPaidPeriods
              ? `${linkSummary.repaymentCount - serverPaidPeriods} 笔小额不计`
              : '单笔 > ¥100'}
          </span>
        </div>
        <div className="fs-loan-link-dash-card fs-loan-link-dash-card--out">
          <span className="fs-loan-link-dash-label">还款合计</span>
          <strong className="fs-loan-link-dash-value">{formatMoney(linkSummary.repaymentQualifying)}</strong>
          <span className="fs-loan-link-dash-sub">
            {linkSummary.repaymentAll > linkSummary.repaymentQualifying
              ? `全部 ${formatMoney(linkSummary.repaymentAll)}`
              : `${linkSummary.repaymentCount} 笔`}
          </span>
        </div>
        {linkSummary.disbursement > 0 && (
          <div className="fs-loan-link-dash-card fs-loan-link-dash-card--in">
            <span className="fs-loan-link-dash-label">放款</span>
            <strong className="fs-loan-link-dash-value">{formatMoney(linkSummary.disbursement)}</strong>
            <span className="fs-loan-link-dash-sub">{linkSummary.disbursementCount} 笔</span>
          </div>
        )}
        {linkSummary.interest > 0 && (
          <div className="fs-loan-link-dash-card">
            <span className="fs-loan-link-dash-label">付息</span>
            <strong className="fs-loan-link-dash-value">{formatMoney(linkSummary.interest)}</strong>
          </div>
        )}
      </div>

      <div className="fs-loan-link-layout">
        <section className="fs-loan-link-col fs-loan-link-col--linked">
          <div className="fs-loan-link-section-head">
            <Typography.Text strong>已关联流水</Typography.Text>
            <Tag>{links.length} 笔</Tag>
          </div>
          {linksLoading ? (
            <div className="fs-loan-link-center"><Spin /></div>
          ) : links.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无关联" className="fs-loan-link-empty" />
          ) : (
            <ul className="fs-loan-txn-list fs-loan-txn-list--linked">
              {links.map((row) => (
                <LinkTxnRow
                  key={row.transactionId ?? row.id}
                  row={row}
                  linked
                  onUnlink={() => row.transactionId && onUnlink(row.transactionId)}
                />
              ))}
            </ul>
          )}
        </section>

        <section className="fs-loan-link-col fs-loan-link-col--pick">
          <div className="fs-loan-link-section-head">
            <Typography.Text strong>选择交易关联</Typography.Text>
            <Tag color="processing">{filteredCandidates.length} 条可选</Tag>
          </div>

          {!hasCard && (
            <Alert type="warning" showIcon message="请先在「详情」中设置放款卡，才能筛选对应流水。" />
          )}

          <div className="fs-loan-link-toolbar">
            <Segmented<LoanLinkType>
              block
              value={linkType}
              onChange={(v) => setLinkType(v)}
              options={(Object.keys(LOAN_LINK_TYPE_LABELS) as LoanLinkType[]).map((k) => ({
                value: k,
                label: LOAN_LINK_TYPE_LABELS[k],
              }))}
            />
            <div className="fs-loan-link-toolbar-row">
              <Segmented<DatePreset>
                value={datePreset}
                onChange={(v) => setDatePreset(v)}
                options={DATE_PRESETS}
              />
              <Input
                allowClear
                prefix={<SearchOutlined />}
                placeholder="搜索描述、卡名…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="fs-loan-link-search"
              />
            </div>
          </div>

          {txnLoading ? (
            <div className="fs-loan-link-center"><Spin tip="加载交易…" /></div>
          ) : filteredCandidates.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={hasCard
                ? `「${activeCardLabel || activeCardId}」在该时间段内没有可关联的交易`
                : '未设置放款卡'}
              className="fs-loan-link-empty"
            />
          ) : (
            <ul className="fs-loan-txn-list fs-loan-txn-list--pick">
              {filteredCandidates.map((t) => (
                <LinkTxnRow
                  key={t.id}
                  row={t}
                  linked={false}
                  linking={linkingId === t.id}
                  onLink={() => onLinkTxn(t.id)}
                />
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
