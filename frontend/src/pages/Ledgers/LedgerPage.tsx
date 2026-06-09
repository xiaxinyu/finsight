import { useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button, DatePicker, Space, TreeSelect } from 'antd'
import { PageContainer, ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import { ledgerConfigs } from '../../config/ledgers'
import { listLedger } from '../../api/ledger'
import { type TransactionRow } from '../../api/transaction'
import { useConsumeTreeSelect } from '../../hooks/useConsumeTree'
import { MoneyText } from '../../components/MoneyText'
import { formatDateMmDdYyyy } from '../../utils/format'

const { RangePicker } = DatePicker

export function LedgerPage() {
  const { ledgerId = '' } = useParams()
  const cfg = ledgerConfigs[ledgerId]
  const actionRef = useRef<ActionType>(null)
  const [range, setRange] = useState({ start: formatDateMmDdYyyy(dayjs().startOf('year')), end: formatDateMmDdYyyy(dayjs()) })
  const [consume, setConsume] = useState('')

  const { treeData } = useConsumeTreeSelect(cfg?.txnType)

  if (!cfg) return <PageContainer title="Ledger not found" />

  const columns: ProColumns<TransactionRow>[] = [
    { title: 'Date', dataIndex: 'transactionDate', width: 110 },
    { title: 'Description', dataIndex: 'transactionDesc', ellipsis: true },
    { title: 'Amount', dataIndex: 'balanceMoney', align: 'right', render: (_, r) => <MoneyText value={r.balanceMoney} unit /> },
    { title: 'Category', dataIndex: 'consumeName', width: 140 },
    { title: 'Card', dataIndex: 'cardTypeName', width: 100 },
  ]

  return (
    <PageContainer title={cfg.title}>
      <Space wrap style={{ marginBottom: 16 }}>
        <RangePicker defaultValue={[dayjs(range.start, 'MM/DD/YYYY'), dayjs(range.end, 'MM/DD/YYYY')]}
          onChange={(v) => v && setRange({ start: formatDateMmDdYyyy(v[0]!), end: formatDateMmDdYyyy(v[1]!) })} />
        {cfg.txnType && <TreeSelect allowClear placeholder="Category" style={{ width: 200 }} treeData={treeData} onChange={(v) => setConsume(v || '')} />}
        <Button type="primary" onClick={() => actionRef.current?.reload()}>Apply</Button>
      </Space>
      <ProTable<TransactionRow>
        actionRef={actionRef}
        rowKey="id"
        size="small"
        search={false}
        request={async (params) => {
          const res = await listLedger(cfg.listEndpoint, {
            page: params.current || 1,
            rows: params.pageSize || 20,
            transactionDateStartStr: range.start,
            transactionDateEndStr: range.end,
            consumeID: consume,
            txnTypes: cfg.txnType,
          })
          return { data: res.rows, total: res.total, success: true }
        }}
        columns={columns}
        pagination={{ defaultPageSize: 20 }}
      />
    </PageContainer>
  )
}
