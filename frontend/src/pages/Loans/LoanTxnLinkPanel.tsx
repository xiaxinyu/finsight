import { useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Button, Empty, Input, Popconfirm, Segmented, Spin, Tag, Typography, message,
} from 'antd'
import {
  ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, LinkOutlined, SearchOutlined,
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
import { formatDate } from './loanDisplay'

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

export function LoanTxnLinkPanel({ loan, onLinksChanged }: Props) {
  const queryClient = useQueryClient()
  const loanId = loan.id ?? ''
  const [linkType, setLinkType] = useState<LoanLinkType>('REPAYMENT')
  const [datePreset, setDatePreset] = useState<DatePreset>('6m')
  const [search, setSearch] = useState('')
  const [linkingId, setLinkingId] = useState<string>()

  const dateRange = useMemo(() => presetRange(datePreset), [datePreset])

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
      'loan-link-candidates', loanId, linkType, datePreset,
      loan.disbursementCardId, loan.repaymentCardId,
      [...linkedTxnIds].join(','),
    ],
    queryFn: async () => {
      const [start, end] = dateRange
      const cardId = linkType === 'DISBURSEMENT'
        ? loan.disbursementCardId
        : (loan.repaymentCardId || loan.disbursementCardId)
      const res = await listTransactions({
        page: 1,
        rows: 100,
        transactionDateStartStr: start.format('YYYY-MM-DD'),
        transactionDateEndStr: end.format('YYYY-MM-DD'),
        cardId,
        sortField: 'transactionDate',
        sortOrder: 'desc',
      })
      return (res.rows ?? []).filter((t) => t.id && !linkedTxnIds.has(t.id))
    },
    enabled: !!loanId && !!(loan.disbursementCardId || loan.repaymentCardId),
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
    let repayment = 0
    let interest = 0
    for (const l of links) {
      const amt = linkAmount(l)
      if (amt == null) continue
      if (l.linkType === 'DISBURSEMENT') disbursement += amt
      else if (l.linkType === 'INTEREST') interest += Math.abs(amt)
      else repayment += Math.abs(amt)
    }
    return { disbursement, repayment, interest, count: links.length }
  }, [links])

  const reload = () => {
    queryClient.invalidateQueries({ queryKey: ['loan-links', loanId] })
    queryClient.invalidateQueries({ queryKey: ['loan-link-candidates', loanId] })
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

  const hasCard = !!(loan.disbursementCardId || loan.repaymentCardId)

  return (
    <div className="fs-loan-link-panel">
      <Alert
        type="info"
        showIcon
        className="fs-loan-link-panel-hint"
        message="关联交易用于追溯放款/还款流水，不会自动修改剩余本金。"
      />

      {linkSummary.count > 0 && (
        <div className="fs-loan-link-summary">
          <div className="fs-loan-link-summary-item">
            <span className="fs-loan-link-summary-label">已关联</span>
            <strong>{linkSummary.count} 笔</strong>
          </div>
          {linkSummary.disbursement > 0 && (
            <div className="fs-loan-link-summary-item fs-loan-link-summary-item--in">
              <ArrowDownOutlined /> 放款 {formatMoney(linkSummary.disbursement)}
            </div>
          )}
          {linkSummary.repayment > 0 && (
            <div className="fs-loan-link-summary-item fs-loan-link-summary-item--out">
              <ArrowUpOutlined /> 还款 {formatMoney(linkSummary.repayment)}
            </div>
          )}
          {linkSummary.interest > 0 && (
            <div className="fs-loan-link-summary-item">
              付息 {formatMoney(linkSummary.interest)}
            </div>
          )}
        </div>
      )}

      <section className="fs-loan-link-section">
        <Typography.Text strong className="fs-loan-link-section-title">已关联流水</Typography.Text>
        {linksLoading ? (
          <div className="fs-loan-link-center"><Spin /></div>
        ) : links.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无关联，从下方选择交易关联" />
        ) : (
          <ul className="fs-loan-txn-list">
            {links.map((row) => {
              const amt = linkAmount(row)
              const positive = amt != null && amt > 0
              return (
                <li key={row.transactionId ?? row.id} className="fs-loan-txn-item fs-loan-txn-item--linked">
                  <div className="fs-loan-txn-item-main">
                    <div className="fs-loan-txn-item-top">
                      <span className="fs-loan-txn-date">{formatDate(row.transactionDate)}</span>
                      <Tag color={linkTypeTagColor(row.linkType)} className="fs-loan-txn-tag">
                        {row.linkType ? LOAN_LINK_TYPE_LABELS[row.linkType] : '—'}
                      </Tag>
                      <span className={`fs-loan-txn-amt ${positive ? 'fs-loan-txn-amt--in' : 'fs-loan-txn-amt--out'}`}>
                        {amt == null ? '—' : formatMoney(amt)}
                      </span>
                    </div>
                    <div className="fs-loan-txn-desc">{row.transactionDesc || row.transactionId}</div>
                    {row.bankCardName && (
                      <div className="fs-loan-txn-meta">{row.bankCardName}</div>
                    )}
                  </div>
                  <Popconfirm title="解除关联？" onConfirm={() => row.transactionId && onUnlink(row.transactionId)}>
                    <Button type="text" size="small" danger icon={<DeleteOutlined />} aria-label="解除关联" />
                  </Popconfirm>
                </li>
              )
            })}
          </ul>
        )}
      </section>

      <section className="fs-loan-link-section fs-loan-link-section--pick">
        <Typography.Text strong className="fs-loan-link-section-title">选择交易关联</Typography.Text>

        {!hasCard && (
          <Alert type="warning" showIcon message="请先在「详情」中设置放款卡，才能筛选对应流水。" />
        )}

        <div className="fs-loan-link-toolbar">
          <Segmented<LoanLinkType>
            value={linkType}
            onChange={(v) => setLinkType(v)}
            options={(Object.keys(LOAN_LINK_TYPE_LABELS) as LoanLinkType[]).map((k) => ({
              value: k,
              label: LOAN_LINK_TYPE_LABELS[k],
            }))}
          />
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

        {txnLoading ? (
          <div className="fs-loan-link-center"><Spin tip="加载交易…" /></div>
        ) : filteredCandidates.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={hasCard ? '该时间段内没有可关联的交易' : '未设置放款卡'}
          />
        ) : (
          <ul className="fs-loan-txn-list fs-loan-txn-list--pick">
            {filteredCandidates.map((t) => {
              const amt = txnSignedAmount(t)
              const positive = amt > 0
              return (
                <li key={t.id} className="fs-loan-txn-item">
                  <div className="fs-loan-txn-item-main">
                    <div className="fs-loan-txn-item-top">
                      <span className="fs-loan-txn-date">{formatDate(t.transactionDate)}</span>
                      <span className={`fs-loan-txn-amt ${positive ? 'fs-loan-txn-amt--in' : 'fs-loan-txn-amt--out'}`}>
                        {formatMoney(amt)}
                      </span>
                    </div>
                    <div className="fs-loan-txn-desc">{t.transactionDesc || t.id}</div>
                    {t.bankCardName && <div className="fs-loan-txn-meta">{t.bankCardName}</div>}
                  </div>
                  <Button
                    type="primary"
                    size="small"
                    ghost
                    icon={<LinkOutlined />}
                    loading={linkingId === t.id}
                    onClick={() => onLinkTxn(t.id)}
                  >
                    关联
                  </Button>
                </li>
              )
            })}
          </ul>
        )}
      </section>
    </div>
  )
}
