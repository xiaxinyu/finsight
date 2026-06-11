import { Drawer, Spin } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { listTransactions } from '../api/transaction'
import { FsDataTable } from './FsDataTable'
import { EmptyState } from './EmptyState'
import { MoneyText, moneyTypeFromRow } from './MoneyText'
import { rowAmount, rowTxnKind } from '../utils/transactionAmount'

type Props = {
  open: boolean
  params: Record<string, string>
  title?: string
  onClose: () => void
}

export function ReportDrillDrawer({ open, params, title = 'Transaction drill-down', onClose }: Props) {
  const { data, isFetching } = useQuery({
    queryKey: ['report-drill', params],
    enabled: open && !!params.transactionDateStartStr,
    queryFn: () => listTransactions({ ...params, page: 1, rows: 80 }),
  })

  return (
    <Drawer title={title} width={720} open={open} onClose={onClose} className="fs-report-drill-drawer">
      {isFetching ? (
        <div className="fs-report-drill-loading"><Spin tip="Loading transactions…" /></div>
      ) : (
        <FsDataTable
          columns={[
            { title: 'Date', dataIndex: 'transactionDate', sortType: 'date', width: 100 },
            { title: 'Description', dataIndex: 'transactionDesc', ellipsis: true },
            {
              title: 'Amount',
              dataIndex: 'balanceMoney',
              unit: 'CNY',
              align: 'right',
              sortType: 'number',
              render: (_, r) => (
                <MoneyText
                  value={rowAmount(r as { incomeMoney?: number; balanceMoney?: number })}
                  type={moneyTypeFromRow(
                    rowTxnKind(r as { incomeMoney?: number; balanceMoney?: number }),
                    (r as { balanceMoney?: number }).balanceMoney,
                  )}
                />
              ),
            },
          ]}
          dataSource={(data?.rows || []) as unknown as Record<string, unknown>[]}
          rowKey="id"
          locale={{ emptyText: <EmptyState compact title="No transactions" description="No rows match this slice." /> }}
        />
      )}
    </Drawer>
  )
}
