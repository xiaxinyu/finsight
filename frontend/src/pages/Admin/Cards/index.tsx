import { useRef } from 'react'
import { message } from 'antd'
import { CreditCardOutlined } from '@ant-design/icons'
import { ProTable, type ActionType } from '@ant-design/pro-components'
import { createCard, deleteCard, listCardsAdmin, updateCard } from '../../../api/admin'
import { DataPageLayout } from '../../../components/DataPageLayout'
import { EmptyState } from '../../../components/EmptyState'
import { useViewportTableHeight } from '../../../hooks/useViewportTableHeight'

export function CardsAdminPage() {
  const actionRef = useRef<ActionType>(null)
  const tableHeight = useViewportTableHeight(180)

  return (
    <DataPageLayout
      title="Bank Cards"
      subtitle="Configure linked bank cards for imports"
      icon={<CreditCardOutlined />}
    >
      <div className="fs-table-panel">
        <ProTable
          className="fs-data-table"
          actionRef={actionRef}
          rowKey="id"
          size="small"
          scroll={{ y: tableHeight }}
          search={false}
          locale={{ emptyText: <EmptyState compact title="No cards" /> }}
          request={async () => {
            const data = await listCardsAdmin()
            return { data: data as Record<string, unknown>[], total: (data as unknown[]).length, success: true }
          }}
          columns={[
            { title: 'Bank', dataIndex: 'bankCode', editable: () => true },
            { title: 'Type', dataIndex: 'cardTypeCode', editable: () => true },
            { title: 'Card No', dataIndex: 'cardNo', editable: () => true },
            { title: 'Name', dataIndex: 'cardName', editable: () => true },
          ]}
          editable={{
            type: 'single',
            onSave: async (_, row) => {
              if (row.id) await updateCard(String(row.id), row as Record<string, unknown>)
              else await createCard(row as Record<string, unknown>)
              message.success('Saved')
              actionRef.current?.reload()
            },
            onDelete: async (id) => { await deleteCard(String(id)); message.success('Deleted') },
          }}
        />
      </div>
    </DataPageLayout>
  )
}
