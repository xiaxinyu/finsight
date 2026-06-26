import { Link } from 'react-router-dom'
import {
  Alert, Button, Modal, Skeleton, Space, Table, Tag, Typography,
} from 'antd'
import {
  BarChartOutlined,
  ClusterOutlined,
  FileSearchOutlined,
  LineChartOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons'
import type { CategoryAsset, CategoryChildCandidate } from '../api/admin'
import { budgetBehaviorLabel, economicNatureLabel, fixedCostKindLabel, inclusionSummary, profileCategorySemantics, semanticTagFromReportRole, semanticTagLabel } from '../utils/categorySemantics'
import { ContentCard } from './ContentCard'
import { EmptyState } from './EmptyState'
import { formatMoney } from '../utils/format'

const QUALITY_LABELS: Record<string, { label: string; color: string }> = {
  empty: { label: 'No transactions', color: 'default' },
  no_active_rules: { label: 'No active rules', color: 'orange' },
  orphan_rules: { label: 'Orphan rules', color: 'red' },
  other_expense_concentration: { label: 'High OTHER volume', color: 'volcano' },
}

type StatTone = 'default' | 'income' | 'expense' | 'warn' | 'muted'

function StatTile({
  label,
  value,
  tone = 'default',
  hint,
  wide,
}: {
  label: string
  value: string
  tone?: StatTone
  hint?: string
  wide?: boolean
}) {
  return (
    <div
      className={`fs-category-stat${tone !== 'default' ? ` fs-category-stat--${tone}` : ''}${wide ? ' fs-category-stat--wide' : ''}`}
      title={hint}
    >
      <span className="fs-category-stat-value">{value}</span>
      <span className="fs-category-stat-label">{label}</span>
    </div>
  )
}

function StatGridSkeleton() {
  return (
    <div className="fs-category-stat-grid">
      {Array.from({ length: 6 }, (_, i) => (
        <div key={i} className="fs-category-stat fs-category-stat--skeleton">
          <Skeleton.Input active size="small" style={{ width: '72%', height: 22 }} />
          <Skeleton.Input active size="small" style={{ width: '48%', height: 12, marginTop: 6 }} />
        </div>
      ))}
    </div>
  )
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
    return (
      <div className="fs-category-asset-panel">
        <ContentCard title="Usage & coverage" size="small" className="fs-category-asset-card">
          <StatGridSkeleton />
        </ContentCard>
      </div>
    )
  }
  if (!asset?.categoryCode) {
    return null
  }

  const code = asset.categoryCode
  const txnLink = `/transactions?consume=${encodeURIComponent(code)}`
  const rulesLink = `/admin/rules?category=${encodeURIComponent(code)}`
  const unclassifiedLink = '/transactions?unclassified=1'
  const monthRows = asset.amountByMonth ?? []

  return (
    <div className="fs-category-asset-panel">
      <ContentCard title="Reporting Classification" size="small" className="fs-category-asset-card fs-category-semantics-card">
        <div className="fs-category-semantics-tags">
          <Tag color="blue">{semanticTagLabel(profileCategorySemantics(
            asset.reportRole,
            undefined,
            asset.parentId,
            asset.categoryCode,
          ).semanticTag)}</Tag>
          {asset.fixedCostKind ? (
            <Tag color="purple">{fixedCostKindLabel(asset.fixedCostKind)}</Tag>
          ) : null}
          <Tag>{economicNatureLabel(asset.economicNature)}</Tag>
          {asset.budgetBehavior && asset.budgetBehavior !== 'variable' && asset.budgetBehavior !== 'none' ? (
            <Tag color="purple">{budgetBehaviorLabel(asset.budgetBehavior)}</Tag>
          ) : null}
        </div>
        <Typography.Paragraph type="secondary" className="fs-category-semantics-inclusion">
          {inclusionSummary(asset)}
        </Typography.Paragraph>
        <div className="fs-category-semantics-flags">
          <Tag color={asset.includeInIncomeTrend ? 'green' : 'default'}>Income trend</Tag>
          <Tag color={asset.includeInExpenseTrend ? 'orange' : 'default'}>Expense trend</Tag>
          <Tag color={asset.includeInBudget ? 'geekblue' : 'default'}>Budget</Tag>
        </div>
      </ContentCard>

      <ContentCard title="Usage & coverage" size="small" className="fs-category-asset-card">
        {asset.qualityFlags?.length ? (
          <Space wrap size={[6, 6]} className="fs-category-quality-flags">
            {asset.qualityFlags.map((flag) => {
              const meta = QUALITY_LABELS[flag] ?? { label: flag, color: 'default' }
              return <Tag key={flag} color={meta.color} className="fs-category-quality-tag">{meta.label}</Tag>
            })}
          </Space>
        ) : null}

        <div className="fs-category-stat-grid">
          <StatTile
            label="Transactions"
            value={(asset.transactionCount ?? 0).toLocaleString()}
          />
          <StatTile
            label="Total amount"
            value={formatMoney(asset.totalAmount ?? 0)}
            tone="income"
            wide
          />
          <StatTile
            label="Last transaction"
            value={asset.lastTransactionDate || '—'}
            tone="muted"
          />
          <StatTile
            label="Child categories"
            value={String(asset.childCategoryCount ?? 0)}
          />
          <StatTile
            label="Active rules"
            value={String(asset.activeRuleCount ?? 0)}
            tone={(asset.activeRuleCount ?? 0) > 0 ? 'default' : 'warn'}
          />
          <StatTile
            label="Inactive rules"
            value={String(asset.inactiveRuleCount ?? 0)}
            tone="muted"
          />
          {(asset.orphanRuleCount ?? 0) > 0 ? (
            <StatTile
              label="Orphan rules"
              value={String(asset.orphanRuleCount)}
              tone="warn"
              wide
            />
          ) : null}
        </div>

        <div className="fs-category-asset-actions">
          <Link to={txnLink} className="fs-category-asset-action">
            <UnorderedListOutlined aria-hidden />
            <span>Transactions</span>
          </Link>
          <Link to={rulesLink} className="fs-category-asset-action">
            <FileSearchOutlined aria-hidden />
            <span>Rules</span>
          </Link>
          {onViewReportImpact ? (
            <button type="button" className="fs-category-asset-action" onClick={onViewReportImpact}>
              <BarChartOutlined aria-hidden />
              <span>Report impact</span>
            </button>
          ) : null}
          <Link to={unclassifiedLink} className="fs-category-asset-action fs-category-asset-action--link">
            <ClusterOutlined aria-hidden />
            <span>Unclassified queue</span>
          </Link>
        </div>
      </ContentCard>

      <ContentCard title="Recent months" size="small" className="fs-category-asset-card fs-category-asset-card--months">
        {monthRows.length ? (
          <Table
            className="fs-category-month-table"
            size="small"
            pagination={false}
            rowKey="yearMonth"
            dataSource={monthRows}
            columns={[
              { title: 'Month', dataIndex: 'yearMonth', width: 108 },
              {
                title: 'Txns',
                dataIndex: 'txnCount',
                width: 72,
                align: 'right' as const,
                render: (v: number) => (
                  <span className="fs-mono">{Number(v || 0).toLocaleString()}</span>
                ),
              },
              {
                title: 'Amount',
                dataIndex: 'amount',
                align: 'right' as const,
                render: (v: number) => (
                  <span className="fs-mono fs-category-month-amount">{formatMoney(v)}</span>
                ),
              },
            ]}
          />
        ) : (
          <EmptyState
            compact
            icon={<LineChartOutlined />}
            title="No monthly activity"
            description="Transactions in this category will appear here once recorded."
            action={(
              <Link to={txnLink}>
                <Button size="small" type="link">Browse transactions</Button>
              </Link>
            )}
          />
        )}
      </ContentCard>

      {asset.childCandidates?.length ? (
        <ContentCard
          title="Seed catalog candidates"
          size="small"
          className="fs-category-asset-card"
          extra={<Tag className="fs-tag">Missing L2</Tag>}
        >
          <Alert
            type="info"
            showIcon
            className="fs-category-candidate-alert"
            message="Review before create — inserts a new unique code only; never changes existing codes."
          />
          <Table
            className="fs-category-candidate-table"
            size="small"
            pagination={false}
            rowKey="code"
            dataSource={asset.childCandidates}
            columns={[
              { title: 'Code', dataIndex: 'code', width: 110, render: (v: string) => <span className="fs-mono">{v}</span> },
              { title: 'Name', dataIndex: 'name', ellipsis: true },
              {
                title: 'Semantics',
                dataIndex: 'reportRole',
                width: 120,
                render: (v: string, row: CategoryChildCandidate) => (
                  <Tag>{semanticTagLabel(semanticTagFromReportRole(v, row.parentL1Code, row.code))}</Tag>
                ),
              },
              {
                title: '',
                key: 'action',
                width: 88,
                align: 'right' as const,
                render: (_: unknown, row: CategoryChildCandidate) => (
                  <Button size="small" type="link" onClick={() => onCreateCandidate?.(row)}>
                    Create…
                  </Button>
                ),
              },
            ]}
          />
        </ContentCard>
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
        <Space direction="vertical" style={{ width: '100%' }}>
          <Typography.Text>
            Insert new L2 under <strong>{parentName || candidate.parentL1Code}</strong>:
          </Typography.Text>
          <dl className="fs-category-candidate-dl">
            <div><dt>Code</dt><dd className="fs-mono">{candidate.code}</dd></div>
            <div><dt>Name</dt><dd>{candidate.name}</dd></div>
            <div><dt>Txn types</dt><dd>{candidate.txnTypes}</dd></div>
            {candidate.reportRole ? (
              <div><dt>Reporting Classification</dt><dd>{semanticTagLabel(semanticTagFromReportRole(candidate.reportRole, candidate.parentL1Code, candidate.code))}</dd></div>
            ) : null}
          </dl>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            {candidate.reason}
          </Typography.Paragraph>
        </Space>
      ) : null}
    </Modal>
  )
}
