import { useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Button, DatePicker, Input, Select, Space, TreeSelect, message } from 'antd'
import { PageContainer, ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import {
  classifyTransactions, consumeTree, deleteTransaction, expenseToIncome,
  incomeToExpense, listCards, listTransactions, updateTransaction, type TransactionRow,
} from '../../api/transaction'
import { MoneyText } from '../../components/MoneyText'
import { formatDateMmDdYyyy } from '../../utils/format'

const { RangePicker } = DatePicker

export function TransactionsPage() {
  const actionRef = useRef<ActionType>(null)
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([])
  const [filters, setFilters] = useState({
    start: formatDateMmDdYyyy(dayjs().startOf('year')),
    end: formatDateMmDdYyyy(dayjs()),
    card: '',
    consume: '',
    keyword: '',
  })

  const { data: cards } = useQuery({ queryKey: ['cards'], queryFn: listCards })
  const { data: tree } = useQuery({ queryKey: ['consume-tree'], queryFn: () => consumeTree() })

  const treeData = (tree || []).map(function mapNode(n): { title: string; value: string; children?: ReturnType<typeof mapNode>[] } {
    return { title: n.text, value: n.id, children: n.children?.map(mapNode) }
  })

  const columns: ProColumns<TransactionRow>[] = [
    { title: 'Date', dataIndex: 'transactionDate', width: 110, valueType: 'date' },
    { title: 'Description', dataIndex: 'transactionDesc', ellipsis: true, width: 220 },
    { title: 'Amount', dataIndex: 'balanceMoney', width: 120, align: 'right', render: (_, r) => <MoneyText value={r.balanceMoney} type="expense" unit /> },
    { title: 'Card', dataIndex: 'cardTypeName', width: 100 },
    { title: 'Category', dataIndex: 'consumeName', width: 140 },
    { title: 'Memo', dataIndex: 'demoArea', width: 120, ellipsis: true },
  ]

  const reload = () => actionRef.current?.reload()

  const runBatch = async (fn: () => Promise<unknown>, okMsg: string) => {
    if (!selectedRowKeys.length) { message.warning('Select rows first'); return }
    try {
      await fn()
      message.success(okMsg)
      setSelectedRowKeys([])
      reload()
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'Failed')
    }
  }

  return (
    <PageContainer title="Transactions">
      <Space wrap style={{ marginBottom: 16 }}>
        <RangePicker defaultValue={[dayjs(filters.start, 'MM/DD/YYYY'), dayjs(filters.end, 'MM/DD/YYYY')]}
          onChange={(v) => v && setFilters((f) => ({ ...f, start: formatDateMmDdYyyy(v[0]!), end: formatDateMmDdYyyy(v[1]!) }))} />
        <Select allowClear placeholder="Card" style={{ width: 140 }} options={(cards || []).map((c) => ({ value: c.key, label: c.value }))}
          onChange={(v) => setFilters((f) => ({ ...f, card: v || '' }))} />
        <TreeSelect allowClear placeholder="Category" style={{ width: 200 }} treeData={treeData} treeDefaultExpandAll
          onChange={(v) => setFilters((f) => ({ ...f, consume: v || '' }))} />
        <Input.Search placeholder="Keyword" allowClear style={{ width: 180 }} onSearch={(v) => { setFilters((f) => ({ ...f, keyword: v })); reload() }} />
        <Button type="primary" onClick={reload}>Apply</Button>
        <Button danger onClick={() => runBatch(() => Promise.all(selectedRowKeys.map(deleteTransaction)), 'Deleted')}>Delete</Button>
        <Button onClick={() => runBatch(() => classifyTransactions(selectedRowKeys.join(',')), 'Classified')}>Auto-classify</Button>
        <Button onClick={() => runBatch(() => incomeToExpense(selectedRowKeys.join(',')), 'Moved to expense')}>→ Expense</Button>
        <Button onClick={() => runBatch(() => expenseToIncome(selectedRowKeys.join(',')), 'Moved to income')}>→ Income</Button>
      </Space>
      <ProTable<TransactionRow>
        actionRef={actionRef}
        rowKey="id"
        size="small"
        scroll={{ x: 900 }}
        search={false}
        options={{ density: true, reload: true }}
        rowSelection={{ selectedRowKeys, onChange: (keys) => setSelectedRowKeys(keys as string[]) }}
        request={async (params) => {
          const res = await listTransactions({
            page: params.current || 1,
            rows: params.pageSize || 20,
            transactionDateStartStr: filters.start,
            transactionDateEndStr: filters.end,
            cardTypeName: filters.card,
            consumeID: filters.consume,
            demoArea: filters.keyword,
          })
          return { data: res.rows, total: res.total, success: true }
        }}
        columns={columns}
        editable={{
          type: 'single',
          onSave: async (_, row) => { await updateTransaction(row); message.success('Saved'); reload() },
          editableKeys: ['demoArea'],
        }}
        pagination={{ defaultPageSize: 20, showSizeChanger: true }}
      />
    </PageContainer>
  )
}
