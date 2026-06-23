import { Modal, Table, Typography, Alert, Descriptions, Tag, Space } from 'antd'
import type { CategoryImpactPreview } from '../api/admin'

type Props = {
  open: boolean
  loading?: boolean
  preview: CategoryImpactPreview | null
  actionLabel: string
  confirmLabel?: string
  confirmDanger?: boolean
  confirmDisabled?: boolean
  onCancel: () => void
  onConfirm: () => void
}

export function CategoryImpactPreviewModal({
  open,
  loading,
  preview,
  actionLabel,
  confirmLabel = 'Confirm',
  confirmDanger = false,
  confirmDisabled = false,
  onCancel,
  onConfirm,
}: Props) {
  return (
    <Modal
      open={open}
      title={`${actionLabel} — impact preview`}
      width={720}
      okText={confirmLabel}
      okButtonProps={{ danger: confirmDanger, disabled: confirmDisabled || !preview }}
      cancelText="Cancel"
      confirmLoading={loading}
      onCancel={onCancel}
      onOk={onConfirm}
      destroyOnClose
    >
      {!preview ? (
        <Typography.Text type="secondary">Loading impact…</Typography.Text>
      ) : (
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            {preview.summary}
          </Typography.Paragraph>

          {preview.warnings?.length ? (
            <Alert
              type="warning"
              showIcon
              message="Review before proceeding"
              description={(
                <ul style={{ margin: 0, paddingLeft: 18 }}>
                  {preview.warnings.map((w: string) => <li key={w}>{w}</li>)}
                </ul>
              )}
            />
          ) : null}

          <Descriptions size="small" bordered column={2}>
            <Descriptions.Item label="Category">{preview.categoryName || preview.categoryCode}</Descriptions.Item>
            <Descriptions.Item label="Code">{preview.categoryCode}</Descriptions.Item>
            <Descriptions.Item label="Transactions">{preview.transactionCount?.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="Total amount">{Number(preview.totalAmount || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</Descriptions.Item>
            <Descriptions.Item label="Active rules">{preview.activeRuleCount}</Descriptions.Item>
            <Descriptions.Item label="Inactive rules">{preview.inactiveRuleCount}</Descriptions.Item>
            <Descriptions.Item label="Child categories">{preview.childCategoryCount}</Descriptions.Item>
            {preview.targetCode ? (
              <Descriptions.Item label="Merge target">{preview.targetName || preview.targetCode}</Descriptions.Item>
            ) : null}
          </Descriptions>

          {preview.affectedReports?.length ? (
            <div>
              <Typography.Text strong>Affected reports</Typography.Text>
              <div style={{ marginTop: 6 }}>
                {preview.affectedReports.map((r: string) => (
                  <Tag key={r} style={{ marginBottom: 4 }}>{r}</Tag>
                ))}
              </div>
            </div>
          ) : null}

          {preview.amountByMonth?.length ? (
            <div>
              <Typography.Text strong>Amount by month (recent)</Typography.Text>
              <Table
                size="small"
                pagination={false}
                style={{ marginTop: 8 }}
                rowKey="yearMonth"
                dataSource={preview.amountByMonth}
                columns={[
                  { title: 'Month', dataIndex: 'yearMonth', width: 100 },
                  { title: 'Txns', dataIndex: 'txnCount', width: 80, align: 'right' as const },
                  {
                    title: 'Amount',
                    dataIndex: 'amount',
                    align: 'right' as const,
                    render: (v: number) => Number(v || 0).toLocaleString(undefined, { minimumFractionDigits: 2 }),
                  },
                ]}
              />
            </div>
          ) : null}
        </Space>
      )}
    </Modal>
  )
}
