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
import { LOAN_DATE_PRESETS, loanLinkCopy } from './loanLabels'

type Props = {
  loan: LoanRow
  onLinksChanged?: () => void
}

type DatePreset = '3m' | '6m' | '1y' | 'all'

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
            <Tooltip title={loanLinkCopy.skipInstallmentTip(MIN_REPAYMENT_INSTALLMENT)}>
              <Tag className="fs-loan-txn-tag fs-loan-txn-tag--skip">{loanLinkCopy.skipInstallment}</Tag>
            </Tooltip>
          )}
          {linkRow?.linkType === 'REPAYMENT' && qualifiesInstallment(linkRow) && (
            <Tag color="green" className="fs-loan-txn-tag">{loanLinkCopy.countsAsInstallment}</Tag>
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
        <Popconfirm title={loanLinkCopy.unlinkConfirm} onConfirm={onUnlink}>
          <Button type="text" size="small" danger icon={<DeleteOutlined />} aria-label={loanLinkCopy.unlink} />
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
          {loanLinkCopy.link}
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
      message.success(loanLinkCopy.linkedOk)
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : loanLinkCopy.linkFailed)
    } finally {
      setLinkingId(undefined)
    }
  }

  const onUnlink = async (txnId: string) => {
    try {
      await removeLoanLink(loanId, txnId)
      message.success(loanLinkCopy.unlinkedOk)
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : loanLinkCopy.unlinkFailed)
    }
  }

  const hasCard = !!activeCardId
  const serverPaidPeriods = paidInstallmentCount(loan)

  return (
    <div className="fs-loan-link-panel">
      <div className="fs-loan-link-info-bar">
        <InfoCircleOutlined className="fs-loan-link-info-bar-icon" />
        <span>{loanLinkCopy.infoBar(MIN_REPAYMENT_INSTALLMENT)}</span>
      </div>

      {hasCard && (
        <div className="fs-loan-link-card-chip">
          <CreditCardOutlined />
          <span>
            {loanLinkCopy.filterChip(
              activeCardLabel || activeCardId || '',
              linkType === 'DISBURSEMENT' ? loanLinkCopy.disbursementCardRole : loanLinkCopy.repaymentCardRole,
            )}
          </span>
        </div>
      )}

      <div className="fs-loan-link-dashboard">
        <div className="fs-loan-link-dash-card">
          <span className="fs-loan-link-dash-label">{loanLinkCopy.linkedCount}</span>
          <strong className="fs-loan-link-dash-value">{linkSummary.count}</strong>
          <span className="fs-loan-link-dash-sub">{loanLinkCopy.linkedSub}</span>
        </div>
        <div className="fs-loan-link-dash-card fs-loan-link-dash-card--period">
          <span className="fs-loan-link-dash-label">{loanLinkCopy.qualifyingPeriods}</span>
          <strong className="fs-loan-link-dash-value">{serverPaidPeriods}</strong>
          <span className="fs-loan-link-dash-sub">
            {linkSummary.repaymentCount > serverPaidPeriods
              ? loanLinkCopy.smallSkipped(linkSummary.repaymentCount - serverPaidPeriods)
              : loanLinkCopy.perInstallmentRule}
          </span>
        </div>
        <div className="fs-loan-link-dash-card fs-loan-link-dash-card--out">
          <span className="fs-loan-link-dash-label">{loanLinkCopy.repaymentTotal}</span>
          <strong className="fs-loan-link-dash-value">{formatMoney(linkSummary.repaymentQualifying)}</strong>
          <span className="fs-loan-link-dash-sub">
            {linkSummary.repaymentAll > linkSummary.repaymentQualifying
              ? loanLinkCopy.allTotal(formatMoney(linkSummary.repaymentAll))
              : loanLinkCopy.txnCount(linkSummary.repaymentCount)}
          </span>
        </div>
        {linkSummary.disbursement > 0 && (
          <div className="fs-loan-link-dash-card fs-loan-link-dash-card--in">
            <span className="fs-loan-link-dash-label">{loanLinkCopy.disbursement}</span>
            <strong className="fs-loan-link-dash-value">{formatMoney(linkSummary.disbursement)}</strong>
            <span className="fs-loan-link-dash-sub">{loanLinkCopy.txnCount(linkSummary.disbursementCount)}</span>
          </div>
        )}
        {linkSummary.interest > 0 && (
          <div className="fs-loan-link-dash-card">
            <span className="fs-loan-link-dash-label">{loanLinkCopy.interest}</span>
            <strong className="fs-loan-link-dash-value">{formatMoney(linkSummary.interest)}</strong>
          </div>
        )}
      </div>

      <div className="fs-loan-link-layout">
        <section className="fs-loan-link-col fs-loan-link-col--linked">
          <div className="fs-loan-link-section-head">
            <Typography.Text strong>{loanLinkCopy.linkedSection}</Typography.Text>
            <Tag>{loanLinkCopy.txnCount(links.length)}</Tag>
          </div>
          {linksLoading ? (
            <div className="fs-loan-link-center"><Spin /></div>
          ) : links.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={loanLinkCopy.noLinked} className="fs-loan-link-empty" />
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
            <Typography.Text strong>{loanLinkCopy.pickSection}</Typography.Text>
            <Tag color="processing">{loanLinkCopy.candidates(filteredCandidates.length)}</Tag>
          </div>

          {!hasCard && (
            <Alert type="warning" showIcon message={loanLinkCopy.setCardWarning} />
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
                options={LOAN_DATE_PRESETS}
              />
              <Input
                allowClear
                prefix={<SearchOutlined />}
                placeholder={loanLinkCopy.searchPlaceholder}
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="fs-loan-link-search"
              />
            </div>
          </div>

          {txnLoading ? (
            <div className="fs-loan-link-center"><Spin tip={loanLinkCopy.loadingTxns} /></div>
          ) : filteredCandidates.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={hasCard
                ? loanLinkCopy.noCandidates(activeCardLabel || activeCardId || '')
                : loanLinkCopy.noCardSet}
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
