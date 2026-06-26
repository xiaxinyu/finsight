import { Space, Tag, Tooltip, Typography } from 'antd'
import {
  catalogFixedCostKindLabel,
  catalogSemanticTagLabel,
  useSemanticsCatalog,
} from '../hooks/useSemanticsCatalog'
import type { FixedCostKind, SemanticTagId } from '../utils/categorySemantics'
import {
  FIXED_COST_KIND_OPTIONS,
  filterSemanticTagGroups,
  inclusionSummary,
  profileCategorySemantics,
  reportRoleFromSemanticSelection,
  semanticTagLabel,
} from '../utils/categorySemantics'

type Props = {
  reportRole?: string
  txnTypes?: string
  parentId?: string
  categoryCode?: string
  onReportRoleChange: (reportRole: string) => void
  showPreview?: boolean
  inferred?: boolean
}

function tagColor(tag: SemanticTagId): string {
  if (tag === 'fixed_spending') return 'purple'
  if (tag === 'subscription_spending') return 'geekblue'
  if (tag === 'daily_spending' || tag === 'other_expense') return 'orange'
  if (tag === 'real_income' || tag === 'other_income') return 'green'
  if (tag === 'investment_income') return 'purple'
  if (tag === 'refund_reimbursement') return 'blue'
  if (tag === 'essential_spending') return 'cyan'
  if (tag === 'investment') return 'geekblue'
  if (tag === 'liability') return 'volcano'
  return 'default'
}

type ReportSurface = { id: string; label: string }

function ReportSurfaceTags({
  surfaces,
  preview,
}: {
  surfaces: ReportSurface[]
  preview: ReturnType<typeof profileCategorySemantics>
}) {
  const active = new Set<string>()
  if (preview.includeInIncomeTrend) active.add('income')
  if (preview.includeInExpenseTrend) active.add('expense')
  if (preview.includeInBudget) active.add('budget')
  if (preview.includeInFixedCostReport) active.add('fixed_cost')
  if (preview.includeInCashflow) active.add('cashflow')

  return (
    <Space wrap size={[4, 4]} className="fs-category-report-surfaces">
      {surfaces.map((s) => (
        <Tag
          key={s.id}
          bordered={false}
          color={active.has(s.id) ? 'processing' : 'default'}
          className={active.has(s.id) ? 'fs-category-report-surface--on' : 'fs-category-report-surface--off'}
        >
          {s.label}
        </Tag>
      ))}
    </Space>
  )
}

export function CategorySemanticPicker({
  reportRole,
  txnTypes,
  parentId,
  categoryCode,
  onReportRoleChange,
  showPreview = true,
  inferred = false,
}: Props) {
  const { data: catalog } = useSemanticsCatalog()
  const preview = profileCategorySemantics(reportRole, txnTypes, parentId, categoryCode)
  const activeTag = preview.semanticTag ?? 'other'
  const activeFixedKind = preview.fixedCostKind as FixedCostKind | null | undefined
  const allGroups = (catalog?.semanticTagGroups ?? []).map((g) => ({
    title: g.title,
    appliesTo: g.appliesTo,
    tags: g.tags as SemanticTagId[],
  }))
  const tagGroups = filterSemanticTagGroups(allGroups.length ? allGroups : [], txnTypes)
  const reportSurfaces = (catalog?.reportSurfaces ?? []) as ReportSurface[]

  const selectTag = (tag: SemanticTagId) => {
    if (tag === 'subscription_spending') {
      onReportRoleChange(reportRoleFromSemanticSelection(tag, 'subscription'))
      return
    }
    let fixedKind = activeFixedKind
    if (tag === 'fixed_spending' && !fixedKind) {
      fixedKind = 'rent'
    }
    if (tag !== 'fixed_spending') {
      fixedKind = null
    }
    onReportRoleChange(reportRoleFromSemanticSelection(tag, fixedKind))
  }

  const selectFixedKind = (kind: FixedCostKind) => {
    onReportRoleChange(reportRoleFromSemanticSelection('fixed_spending', kind))
  }

  const primaryLabel = catalogSemanticTagLabel(catalog, activeTag) || semanticTagLabel(activeTag)

  return (
    <div className="fs-category-classification-panel">
      {tagGroups.map((group) => (
        <div key={group.title} className="fs-category-semantic-group">
          <Typography.Text className="fs-category-semantic-group-label">{group.title}</Typography.Text>
          <Space wrap size={[6, 6]} className="fs-category-semantic-tag-row">
            {group.tags.map((tag) => {
              const selected = activeTag === tag
              const meta = catalog?.semanticTags?.[tag]
              return (
                <Tooltip key={tag} title={meta?.description}>
                  <Tag.CheckableTag
                    checked={selected}
                    className={`fs-category-semantic-tag${selected ? ' fs-category-semantic-tag--active' : ''}`}
                    onChange={() => selectTag(tag)}
                  >
                    {catalogSemanticTagLabel(catalog, tag)}
                  </Tag.CheckableTag>
                </Tooltip>
              )
            })}
          </Space>
        </div>
      ))}

      {activeTag === 'fixed_spending' && (
        <div className="fs-category-semantic-group fs-category-semantic-group--fixed">
          <Typography.Text className="fs-category-semantic-group-label">
            {catalog?.fixedCostKindSectionLabel ?? 'Fixed Cost Type'}
          </Typography.Text>
          <Space wrap size={[6, 6]} className="fs-category-semantic-tag-row">
            {FIXED_COST_KIND_OPTIONS.filter(({ value }) => value !== 'subscription').map(({ value }) => {
              const selected = (activeFixedKind ?? 'rent') === value
              return (
                <Tag.CheckableTag
                  key={value}
                  checked={selected}
                  className={`fs-category-semantic-tag fs-category-semantic-tag--fixed${selected ? ' fs-category-semantic-tag--active' : ''}`}
                  onChange={() => selectFixedKind(value)}
                >
                  {catalogFixedCostKindLabel(catalog, value)}
                </Tag.CheckableTag>
              )
            })}
          </Space>
        </div>
      )}

      {showPreview && (
        <div className="fs-category-report-preview">
          <Typography.Text className="fs-category-report-preview-title">
            {catalog?.previewSectionLabel ?? 'Report Impact Preview'}
          </Typography.Text>
          <div className="fs-category-report-preview-primary">
            <Tag color={tagColor(activeTag)} className="fs-category-report-preview-classification">
              {primaryLabel}
            </Tag>
            {activeFixedKind && activeTag === 'fixed_spending' && (
              <Tag color="purple">{catalogFixedCostKindLabel(catalog, activeFixedKind)}</Tag>
            )}
          </div>
          {reportSurfaces.length > 0 && (
            <ReportSurfaceTags surfaces={reportSurfaces} preview={preview} />
          )}
          <Typography.Text type="secondary" className="fs-category-report-preview-summary">
            {inclusionSummary(preview)}
          </Typography.Text>
          {inferred && (
            <Typography.Text type="warning" className="fs-category-report-preview-summary">
              Inferred default — save to persist this classification.
            </Typography.Text>
          )}
        </div>
      )}
    </div>
  )
}
