import { Link } from 'react-router-dom'
import {
  Alert, Button, Descriptions, Modal, Space, Table, Tag, Typography,
} from 'antd'
import type { CategoryAsset, CategoryChildCandidate } from '../api/admin'

const QUALITY_LABELS: Record<string, { label: string; color: string }> = {
  empty: { label: 'No transactions', color: 'default' },
  no_active_rules: { label: 'No active rules', color: 'orange' },
  orphan_rules: { label: 'Orphan rules', color: 'red' },
  other_expense_concentration: { label: 'High OTHER volume', color: 'volcano' },
}

type Props = {
  asset: CategoryAsset | null
  loading?: boolean
  onCreateCandidate?: (candidate: CategoryChildCandidate) => void
  onViewReportImpact?: () => void
}

export function CategoryAssetPanel({
  asset,
  loading,
  onCreateCandidate,
  onViewReportImpact,
}: Props) {
  if (loading) {
    return <Typography.Text type="secondary">Loading usage & coverage…</Typography.Text>
  }
  if (!asset?.categoryCode) {
    return null
  }

  const code = asset.categoryCode
  const txnLink = `/transactions?consume=${encodeURIComponent(code)}`
  const rulesLink = `/admin/rules?category=${encodeURIComponent(code)}`
  const unclassifiedLink = '/transactions?unclassified=1'

  return (
    <div className="fs-category-asset-panel">
      <Typography.Title level={5} style={{ marginTop: 0 }}>
        Usage & coverage
      </Typography.Title>

      {asset.qualityFlags?.length ? (
        <Space wrap size={[4, 4]} style={{ marginBottom: 12 }}>
          {asset.qualityFlags.map((flag) => {
            const meta = QUALITY_LABELS[flag] ?? { label: flag, color: 'default' }
            return <Tag key={flag} color={meta.color}>{meta.label}</Tag>
          })}
        </Space>
      ) : null}

      <Descriptions size="small" bordered column={2} style={{ marginBottom: 12 }}>
        <Descriptions.Item label="Transactions">
          {(asset.transactionCount ?? 0).toLocaleString()}
        </Descriptions.Item>
        <Descriptions.Item label="Total amount">
          {Number(asset.totalAmount ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
        </Descriptions.Item>
        <Descriptions.Item label="Last transaction">
          {asset.lastTransactionDate || '—'}
        </Descriptions.Item>
        <Descriptions.Item label="Child categories">
          {asset.childCategoryCount ?? 0}
        </Descriptions.Item>
        <Descriptions.Item label="Active rules">{asset.activeRuleCount ?? 0}</Descriptions.Item>
        <Descriptions.Item label="Inactive rules">{asset.inactiveRuleCount ?? 0}</Descriptions.Item>
        {(asset.orphanRuleCount ?? 0) > 0 ? (
          <Descriptions.Item label="Orphan rules" span={2}>
            {asset.orphanRuleCount}
          </Descriptions.Item>
        ) : null}
      </Descriptions>

      <Space wrap size="small" style={{ marginBottom: 12 }}>
        <Link to={txnLink}>
          <Button size="small">View transactions</Button>
        </Link>
        <Link to={rulesLink}>
          <Button size="small">View rules</Button>
        </Link>
        {onViewReportImpact ? (
          <Button size="small" onClick={onViewReportImpact}>View report impact</Button>
        ) : null}
        <Link to={unclassifiedLink}>
          <Button size="small" type="link">Unclassified queue</Button>
        </Link>
      </Space>

      {asset.amountByMonth?.length ? (
        <>
          <Typography.Text strong>Recent months</Typography.Text>
          <Table
            size="small"
            pagination={false}
            style={{ marginTop: 8, marginBottom: 12 }}
            rowKey="yearMonth"
            dataSource={asset.amountByMonth}
            columns={[
              { title: 'Month', dataIndex: 'yearMonth', width: 100 },
              { title: 'Txns', dataIndex: 'txnCount', width: 72, align: 'right' as const },
              {
                title: 'Amount',
                dataIndex: 'amount',
                align: 'right' as const,
                render: (v: number) => Number(v || 0).toLocaleString(undefined, { minimumFractionDigits: 2 }),
              },
            ]}
          />
        </>
      ) : null}

      {asset.childCandidates?.length ? (
        <>
          <Typography.Text strong>Seed catalog candidates (missing L2)</Typography.Text>
          <Alert
            type="info"
            showIcon
            style={{ margin: '8px 0' }}
            message="Review before create — inserts a new unique code only; never changes existing codes."
          />
          <Table
            size="small"
            pagination={false}
            rowKey="code"
            dataSource={asset.childCandidates}
            columns={[
              { title: 'Code', dataIndex: 'code', width: 110 },
              { title: 'Name', dataIndex: 'name' },
              {
                title: '',
                key: 'action',
                width: 88,
                render: (_: unknown, row: CategoryChildCandidate) => (
                  <Button size="small" type="link" onClick={() => onCreateCandidate?.(row)}>
                    Create…
                  </Button>
                ),
              },
            ]}
          />
        </>
      ) : null}
    </div>
  )
}

type CandidateModalProps = {
  open: boolean
  candidate: CategoryChildCandidate | null
  parentName?: string
  loading?: boolean
  onCancel: () => void
  onConfirm: () => void
}

export function CategoryCandidateConfirmModal({
  open,
  candidate,
  parentName,
  loading,
  onCancel,
  onConfirm,
}: CandidateModalProps) {
  return (
    <Modal
      open={open}
      title="Create subcategory from catalog"
      okText="Create category"
      confirmLoading={loading}
      onCancel={onCancel}
      onOk={onConfirm}
      destroyOnClose
    >
      {candidate ? (
        <Space direction="vertical">
          <Typography.Text>
            Insert new L2 under <strong>{parentName || candidate.parentL1Code}</strong>:
          </Typography.Text>
          <Descriptions size="small" bordered column={1}>
            <Descriptions.Item label="Code">{candidate.code}</Descriptions.Item>
            <Descriptions.Item label="Name">{candidate.name}</Descriptions.Item>
            <Descriptions.Item label="Txn types">{candidate.txnTypes}</Descriptions.Item>
          </Descriptions>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            {candidate.reason}
          </Typography.Paragraph>
        </Space>
      ) : null}
    </Modal>
  )
}
