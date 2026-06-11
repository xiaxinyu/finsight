import { useRef } from 'react'
import { Button, message, Popconfirm, Select } from 'antd'
import { CreditCardOutlined, PlusOutlined } from '@ant-design/icons'
import { ProTable, type ActionType } from '@ant-design/pro-components'
import { createCard, deleteCard, listCardsAdmin, updateCard } from '../../../api/admin'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { EmptyState } from '../../../components/EmptyState'
import { useFillTableHeight } from '../../../hooks/useFillTableHeight'

const BANK_OPTIONS = [
  { value: 'CMB', label: 'CMB' },
  { value: 'CCB', label: 'CCB' },
  { value: 'CGB', label: 'CGB' },
  { value: 'CRBANK', label: 'CRBANK' },
  { value: 'ICBC', label: 'ICBC' },
  { value: 'ALIPAY', label: 'ALIPAY' },
  { value: 'WECHAT', label: 'WECHAT' },
]

const CARD_TYPE_OPTIONS = [
  { value: 'debit', label: 'Debit' },
  { value: 'credit', label: 'Credit' },
  { value: 'ewallet', label: 'E-wallet' },
]

export function CardsAdminPage() {
  const actionRef = useRef<ActionType>(null)
  const tablePanelRef = useRef<HTMLDivElement>(null)
  const tableHeight = useFillTableHeight(tablePanelRef)

  return (
    <DataPageLayout
      title="Bank Cards"
      subtitle="Linked accounts used for statement imports and transaction matching"
      icon={<CreditCardOutlined />}
      className="fs-data-page--dense fs-data-page--fill"
      actions={(
        <Button
          type="primary"
          size="small"
          icon={<PlusOutlined />}
          onClick={() => actionRef.current?.addEditRecord?.(
            { bankCode: 'CMB', cardTypeCode: 'debit', cardNo: '', cardName: '' },
            { position: 'top' },
          )}
        >
          Add card
        </Button>
      )}
    >
      <div ref={tablePanelRef} className="fs-table-panel fs-table-panel--editable">
        <ProTable
          className="fs-data-table"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          scroll={{ x: 'max-content', y: tableHeight }}
          search={false}
          options={{ density: true, reload: true }}
          locale={{ emptyText: <EmptyState compact title="No cards" description="Add a bank card to enable imports." /> }}
          request={async () => {
            const data = await listCardsAdmin()
            return { data: data as Record<string, unknown>[], total: (data as unknown[]).length, success: true }
          }}
          columns={[
            {
              title: 'Bank',
              dataIndex: 'bankCode',
              width: 100,
              editable: () => true,
              renderFormItem: () => <Select size="small" options={BANK_OPTIONS} />,
            },
            {
              title: 'Type',
              dataIndex: 'cardTypeCode',
              width: 100,
              editable: () => true,
              renderFormItem: () => <Select size="small" options={CARD_TYPE_OPTIONS} />,
            },
            { title: 'Card No', dataIndex: 'cardNo', width: 180, editable: () => true },
            { title: 'Name', dataIndex: 'cardName', ellipsis: true, editable: () => true },
            {
              title: 'Actions',
              valueType: 'option',
              width: 120,
              render: (_, row, __, action) => [
                <a key="edit" onClick={() => action?.startEditable?.(row.id as string)}>Edit</a>,
                <Popconfirm
                  key="del"
                  title="Delete this card?"
                  onConfirm={async () => {
                    await deleteCard(String(row.id))
                    message.success('Deleted')
                    actionRef.current?.reload()
                  }}
                >
                  <a>Delete</a>
                </Popconfirm>,
              ],
            },
          ]}
          editable={{
            type: 'single',
            onSave: async (_, row) => {
              if (row.id) await updateCard(String(row.id), row as Record<string, unknown>)
              else await createCard(row as Record<string, unknown>)
              message.success('Saved')
              actionRef.current?.reload()
            },
          }}
          pagination={{ defaultPageSize: 20, showSizeChanger: true, size: 'small', showTotal: (t) => `${t} cards` }}
        />
      </div>
    </DataPageLayout>
  )
}
