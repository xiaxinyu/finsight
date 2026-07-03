import { useMemo } from 'react'
import { Button, Popconfirm, Tooltip, message } from 'antd'
import { DeleteOutlined, EditOutlined } from '@ant-design/icons'
import type { ProColumns } from '@ant-design/pro-components'
import type { TransactionRow } from '../../api/transaction'
import { deleteTransaction } from '../../api/transaction'
import { TableHeader } from '../../components/TableHeader'
import { TransactionAmountCell } from '../../components/TransactionAmountCell'
import { TransactionCardCell } from '../../components/TransactionCardCell'
import { TransactionCategoryCell } from '../../components/TransactionCategoryCell'
import { TransactionLedgerCell } from '../../components/TransactionLedgerCell'
import { TransactionMerchantCell } from '../../components/TransactionMerchantCell'
import { formatTableDate } from '../../utils/cell'

type UseTransactionColumnsArgs = {
  editingId: string | null
  pageMaxAmount: number
  startEdit: (row: TransactionRow) => void
  reload: () => Promise<void>
}

export function useTransactionColumns({
  editingId,
  pageMaxAmount,
  startEdit,
  reload,
}: UseTransactionColumnsArgs): ProColumns<TransactionRow>[] {
  return useMemo(() => [
    {
      title: <TableHeader name="Date" />,
      dataIndex: 'transactionDate',
      width: 84,
      fixed: 'left',
      sorter: true,
      defaultSortOrder: 'descend',
      render: (_, r) => <span className="fs-tx-date">{formatTableDate(r.transactionDate)}</span>,
    },
    {
      title: <TableHeader name="Transaction" />,
      dataIndex: 'transactionDesc',
      className: 'fs-col-tx-desc',
      width: 280,
      fixed: 'left',
      ellipsis: true,
      render: (_, r) => <TransactionLedgerCell row={r} showTags={false} />,
    },
    {
      title: <TableHeader name="Merchant" />,
      dataIndex: 'opponentName',
      width: 120,
      ellipsis: true,
      responsive: ['md'],
      render: (_, r) => <TransactionMerchantCell row={r} />,
    },
    {
      title: <TableHeader name="Category" />,
      dataIndex: 'consumeName',
      width: 168,
      ellipsis: true,
      render: (_, r) => <TransactionCategoryCell row={r} />,
    },
    {
      title: <TableHeader name="Amount" unit="CNY" />,
      dataIndex: 'editAmount',
      width: 112,
      align: 'right',
      sorter: true,
      render: (_, r) => <TransactionAmountCell row={r} pageMaxAmount={pageMaxAmount} />,
    },
    {
      title: <TableHeader name="Card" />,
      dataIndex: 'bankCode',
      width: 108,
      ellipsis: true,
      editable: false,
      sorter: true,
      render: (_, r) => <TransactionCardCell row={r} />,
    },
    {
      title: '',
      valueType: 'option',
      width: 64,
      fixed: 'right',
      className: 'fs-col-actions',
      render: (_, record) => (
        <div className="fs-inline-actions">
          <Tooltip title="Edit">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              className="fs-row-action"
              disabled={editingId != null}
              onClick={() => startEdit(record)}
            />
          </Tooltip>
          <Popconfirm
            title="Delete this transaction?"
            onConfirm={async () => {
              await deleteTransaction(record.id)
              message.success('Deleted')
              await reload()
            }}
          >
            <Tooltip title="Delete">
              <Button type="text" size="small" danger icon={<DeleteOutlined />} className="fs-row-action"
                disabled={editingId != null} />
            </Tooltip>
          </Popconfirm>
        </div>
      ),
    },
  ], [editingId, pageMaxAmount, reload, startEdit])
}
