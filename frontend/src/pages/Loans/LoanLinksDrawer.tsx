import { useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert, Button, DatePicker, Drawer, Empty, Popconfirm, Select, Space, Spin, Table, Tag, Typography, message,
} from 'antd'
import { LinkOutlined, PlusOutlined } from '@ant-design/icons'
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

type Props = {
  loan: LoanRow | null
  open: boolean
  onClose: () => void
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

function formatTxnDate(v?: string) {
  if (!v) return '—'
  return dayjs(v).format('YYYY-MM-DD')
}

export function LoanLinksDrawer({ loan, open, onClose }: Props) {
  const queryClient = useQueryClient()
  const loanId = loan?.id ?? ''
  const [linkType, setLinkType] = useState<LoanLinkType>('REPAYMENT')
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(3, 'month'),
    dayjs(),
  ])
  const [selectedTxnId, setSelectedTxnId] = useState<string>()
  const [adding, setAdding] = useState(false)

  const { data: links = [], isLoading: linksLoading } = useQuery({
    queryKey: ['loan-links', loanId],
    queryFn: () => fetchLoanLinks(loanId),
    enabled: open && !!loanId,
  })

  const cardIds = useMemo(() => {
    const ids = new Set<string>()
    if (loan?.disbursementCardId) ids.add(loan.disbursementCardId)
    if (loan?.repaymentCardId) ids.add(loan.repaymentCardId)
    return [...ids]
  }, [loan?.disbursementCardId, loan?.repaymentCardId])

  const linkedTxnIds = useMemo(
    () => links.map((l) => l.transactionId).filter(Boolean).join(','),
    [links],
  )

  const { data: txnCandidates = [], isLoading: txnLoading } = useQuery({
    queryKey: ['loan-link-candidates', loanId, linkType, dateRange[0].format('YYYY-MM-DD'), dateRange[1].format('YYYY-MM-DD'), linkedTxnIds],
    queryFn: async () => {
      const start = dateRange[0].format('YYYY-MM-DD')
      const end = dateRange[1].format('YYYY-MM-DD')
      const cardId = linkType === 'DISBURSEMENT'
        ? loan?.disbursementCardId
        : (loan?.repaymentCardId || loan?.disbursementCardId)
      const res = await listTransactions({
        page: 1,
        rows: 50,
        transactionDateStartStr: start,
        transactionDateEndStr: end,
        cardId,
        sortField: 'transactionDate',
        sortOrder: 'desc',
      })
      const linked = new Set(links.map((l) => l.transactionId))
      return (res.rows ?? []).filter((t) => t.id && !linked.has(t.id))
    },
    enabled: open && !!loanId && cardIds.length > 0,
  })

  const reloadLinks = () => {
    queryClient.invalidateQueries({ queryKey: ['loan-links', loanId] })
    queryClient.invalidateQueries({ queryKey: ['loan-link-candidates', loanId] })
  }

  const onAddLink = async () => {
    if (!loanId || !selectedTxnId) {
      message.warning('Select a transaction to link')
      return
    }
    setAdding(true)
    try {
      await addLoanLink(loanId, selectedTxnId, linkType)
      message.success('Transaction linked')
      setSelectedTxnId(undefined)
      reloadLinks()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Failed to link transaction')
    } finally {
      setAdding(false)
    }
  }

  const columns = [
    {
      title: 'Date',
      dataIndex: 'transactionDate',
      width: 100,
      render: (v: string) => formatTxnDate(v),
    },
    {
      title: 'Type',
      dataIndex: 'linkType',
      width: 80,
      render: (v: LoanLinkType) => (
        <Tag color={v === 'DISBURSEMENT' ? 'green' : v === 'REPAYMENT' ? 'blue' : 'default'}>
          {v ? LOAN_LINK_TYPE_LABELS[v] : '—'}
        </Tag>
      ),
    },
    {
      title: 'Amount',
      key: 'amount',
      width: 110,
      align: 'right' as const,
      render: (_: unknown, row: LoanTxnLinkRow) => {
        const amt = linkAmount(row)
        return amt == null ? '—' : formatMoney(amt)
      },
    },
    {
      title: 'Description',
      dataIndex: 'transactionDesc',
      ellipsis: true,
    },
    {
      title: 'Card',
      dataIndex: 'bankCardName',
      width: 120,
      ellipsis: true,
      render: (v: string) => v || '—',
    },
    {
      title: '',
      key: 'actions',
      width: 72,
      render: (_: unknown, row: LoanTxnLinkRow) => (
        <Popconfirm
          title="Remove this link?"
          onConfirm={async () => {
            if (!loanId || !row.transactionId) return
            try {
              await removeLoanLink(loanId, row.transactionId)
              message.success('Link removed')
              reloadLinks()
            } catch (e) {
              message.error(e instanceof Error ? e.message : 'Failed to remove link')
            }
          }}
        >
          <a>Unlink</a>
        </Popconfirm>
      ),
    },
  ]

  return (
    <Drawer
      title={loan ? `Linked transactions — ${loan.lenderName}` : 'Linked transactions'}
      width={720}
      open={open}
      onClose={onClose}
      destroyOnClose
    >
      {loan && (
        <>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
            Associate ledger entries with this loan: disbursement credits, repayments, or interest payments.
            Links do not auto-update outstanding balance — adjust the loan manually when needed.
          </Typography.Paragraph>

          <div className="fs-loan-links-add">
            <Typography.Text strong>Add link</Typography.Text>
            <Space wrap style={{ marginTop: 8, width: '100%' }}>
              <Select<LoanLinkType>
                value={linkType}
                onChange={(v) => { setLinkType(v); setSelectedTxnId(undefined) }}
                options={(Object.keys(LOAN_LINK_TYPE_LABELS) as LoanLinkType[]).map((k) => ({
                  value: k,
                  label: LOAN_LINK_TYPE_LABELS[k],
                }))}
                style={{ width: 120 }}
              />
              <DatePicker.RangePicker
                value={dateRange}
                onChange={(v) => {
                  if (v?.[0] && v[1]) setDateRange([v[0], v[1]])
                }}
                allowClear={false}
              />
              <Select
                showSearch
                placeholder="Search transaction"
                optionFilterProp="label"
                style={{ minWidth: 280, flex: 1 }}
                loading={txnLoading}
                value={selectedTxnId}
                onChange={setSelectedTxnId}
                options={txnCandidates.map((t) => {
                  const amt = txnSignedAmount(t)
                  const label = `${formatTxnDate(t.transactionDate)} · ${formatMoney(amt)} · ${t.transactionDesc ?? t.id}`
                  return { value: t.id, label }
                })}
              />
              <Button type="primary" icon={<PlusOutlined />} loading={adding} onClick={onAddLink}>
                Link
              </Button>
            </Space>
            {!cardIds.length && (
              <Alert type="warning" showIcon style={{ marginTop: 8 }} message="Set a disbursement card on the loan before linking transactions." />
            )}
          </div>

          <Typography.Title level={5} style={{ marginTop: 20, marginBottom: 8 }}>
            <LinkOutlined /> Linked ({links.length})
          </Typography.Title>

          {linksLoading ? (
            <Spin />
          ) : links.length === 0 ? (
            <Empty description="No linked transactions yet" />
          ) : (
            <Table
              rowKey={(r) => r.transactionId ?? r.id ?? ''}
              size="small"
              columns={columns}
              dataSource={links}
              pagination={false}
              scroll={{ x: 'max-content' }}
            />
          )}
        </>
      )}
    </Drawer>
  )
}
