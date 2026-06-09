import { useRef } from 'react'
import { message } from 'antd'
import { PageContainer, ProTable, type ActionType } from '@ant-design/pro-components'
import { createCard, deleteCard, listCardsAdmin, updateCard } from '../../api/admin'

export function CardsAdminPage() {
  const actionRef = useRef<ActionType>(null)
  return (
    <PageContainer title="Bank Cards">
      <ProTable
        actionRef={actionRef}
        rowKey="id"
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
    </PageContainer>
  )
}
