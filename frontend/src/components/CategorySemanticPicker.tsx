import { Alert, Tag, Typography } from 'antd'
import {
  catalogSemanticTagLabel,
  useSemanticsCatalog,
} from '../hooks/useSemanticsCatalog'
import type { SemanticTagId } from '../utils/categorySemantics'
import {
  compactGroupTitle,
  filterSemanticTagGroups,
  inclusionSummary,
  isFlatFixedSemanticTag,
  parentTxnMismatchWarning,
  profileCategorySemantics,
  reportRoleForSemanticTag,
  reportRoleFromSemanticSelection,
  resolveSemanticTag,
  semanticTagLabel,
  txnTypeFilter,
} from '../utils/categorySemantics'

type PickerProps = {
  semanticTag?: string
  reportRole?: string
  txnTypes?: string
  parentId?: string
  categoryCode?: string
  categoryName?: string
  onSemanticTagChange: (tag: SemanticTagId, reportRole: string) => void
  showPreview?: boolean
  inferred?: boolean
  mismatchWarning?: string | null
}

function tagColor(tag: SemanticTagId): string {
  if (isFlatFixedSemanticTag(tag) || tag === 'fixed_spending') return 'purple'
  if (tag === 'subscription_spending') return 'geekblue'
  if (tag === 'dining_spending' || tag === 'daily_spending' || tag === 'other_expense') return 'orange'
  if (tag === 'shopping_spending') return 'gold'
  if (tag === 'transport_spending') return 'blue'
  if (tag === 'entertainment_spending') return 'volcano'
  if (tag === 'education_spending') return 'cyan'
  if (tag === 'medical_spending') return 'green'
  if (tag === 'social_spending') return 'magenta'
  if (tag === 'real_income' || tag === 'other_income') return 'green'
  if (tag === 'investment_income') return 'purple'
  if (tag === 'refund_reimbursement') return 'blue'
  if (tag === 'essential_spending') return 'cyan'
  if (tag === 'investment') return 'geekblue'
  if (tag === 'liability') return 'volcano'
  return 'default'
}

type ReportSurface = { id: string; label: string }

function txnBadgeLabel(filter: ReturnType<typeof txnTypeFilter>): string {
  if (filter === 'income') return 'Income'
  if (filter === 'expense') return 'Expense'
  return 'Mixed'
}

function isFixedTagGroup(title: string): boolean {
  return title === 'Fixed' || title === 'Fixed Commitments'
}

function SelectableTag({
  selected,
  label,
  description,
  tone,
  onSelect,
}: {
  selected: boolean
  label: string
  description?: string
  tone?: 'fixed' | 'default'
  onSelect: () => void
}) {
  return (
    <button
      type="button"
      title={description}
      aria-pressed={selected}
      className={`fs-category-semantic-tag${tone === 'fixed' ? ' fs-category-semantic-tag--fixed' : ''}${selected ? ' fs-category-semantic-tag--active' : ''}`}
      onClick={onSelect}
    >
      {label}
    </button>
  )
}

export function CategorySemanticPicker({
  semanticTag,
  reportRole,
  txnTypes,
  parentId,
  categoryCode,
  categoryName,
  onSemanticTagChange,
  showPreview = true,
  inferred = false,
  mismatchWarning = null,
}: PickerProps) {
  const { data: catalog } = useSemanticsCatalog()
  const activeTag = resolveSemanticTag(semanticTag, reportRole, parentId, categoryCode, txnTypes, categoryName)
  const preview = profileCategorySemantics(reportRole, txnTypes, parentId, categoryCode, activeTag)
  const allGroups = (catalog?.semanticTagGroups ?? []).map((g) => ({
    title: g.title,
    appliesTo: g.appliesTo,
    tags: g.tags as SemanticTagId[],
  }))
  const tagGroups = filterSemanticTagGroups(
    allGroups.length ? allGroups : [],
    txnTypes,
    parentId,
    categoryCode,
  )
  const reportSurfaces = (catalog?.reportSurfaces ?? []) as ReportSurface[]
  const txnFilter = txnTypeFilter(txnTypes)
  const parentWarning = parentTxnMismatchWarning(parentId, txnTypes)

  const selectTag = (tag: SemanticTagId) => {
    if (tag === 'subscription_spending') {
      onSemanticTagChange(tag, reportRoleFromSemanticSelection(tag, 'subscription'))
      return
    }
    onSemanticTagChange(tag, reportRoleForSemanticTag(tag, parentId, categoryCode))
  }

  const primaryLabel = catalogSemanticTagLabel(catalog, activeTag) || semanticTagLabel(activeTag)
  const activeSurfaces = reportSurfaces.filter((s) => {
    if (s.id === 'income') return preview.includeInIncomeTrend
    if (s.id === 'expense') return preview.includeInExpenseTrend
    if (s.id === 'budget') return preview.includeInBudget
    if (s.id === 'fixed_cost') return preview.includeInFixedCostReport
    if (s.id === 'cashflow') return preview.includeInCashflow
    return false
  })

  return (
    <div className={`fs-category-classification-panel fs-category-classification-panel--${txnFilter}`}>
      <div className="fs-category-classification-toolbar">
        <span className={`fs-category-txn-badge fs-category-txn-badge--${txnFilter}`}>
          {txnBadgeLabel(txnFilter)}
        </span>
        {showPreview && (
          <div className="fs-category-classification-selection">
            <Tag color={tagColor(activeTag)} bordered={false} className="fs-category-selection-tag">
              {primaryLabel}
            </Tag>
            {activeSurfaces.length > 0 && (
              <Typography.Text type="secondary" className="fs-category-selection-reports">
                → {activeSurfaces.map((s) => s.label).join(' · ')}
              </Typography.Text>
            )}
          </div>
        )}
      </div>

      {(parentWarning || mismatchWarning) && (
        <Alert
          type="warning"
          showIcon
          message={parentWarning || mismatchWarning}
          className="fs-category-classification-alert"
        />
      )}

      <div className="fs-category-classification-body">
        {tagGroups.map((group) => {
          const fixedRow = isFixedTagGroup(group.title)
          return (
          <div
            key={group.title}
            className={`fs-category-semantic-group fs-category-semantic-group--inline${fixedRow ? ' fs-category-semantic-group--fixed' : ''}`}
          >
            <span className="fs-category-semantic-group-label">
              {compactGroupTitle(group.title, txnFilter)}
            </span>
            <div className="fs-category-semantic-tag-row">
              {group.tags.map((tag) => {
                const meta = catalog?.semanticTags?.[tag]
                return (
                  <SelectableTag
                    key={tag}
                    selected={activeTag === tag}
                    label={catalogSemanticTagLabel(catalog, tag)}
                    description={meta?.description}
                    tone={fixedRow ? 'fixed' : 'default'}
                    onSelect={() => selectTag(tag)}
                  />
                )
              })}
            </div>
          </div>
          )
        })}
      </div>

      {showPreview && (
        <Typography.Text type="secondary" className="fs-category-report-preview-footnote">
          {inclusionSummary(preview)}
          {inferred ? ' · Default inferred — save to persist.' : ''}
        </Typography.Text>
      )}
    </div>
  )
}

/** Ant Design Form control — binds semanticTag; syncs reportRole via callback. */
export function CategoryReportRoleControl({
  value,
  onChange,
  reportRole,
  txnTypes,
  parentId,
  categoryCode,
  categoryName,
  inferred,
  mismatchWarning,
  onReportRoleSync,
}: {
  value?: string
  onChange?: (value: string) => void
  reportRole?: string
  txnTypes?: string
  parentId?: string
  categoryCode?: string
  categoryName?: string
  inferred?: boolean
  mismatchWarning?: string | null
  onReportRoleSync?: (reportRole: string) => void
}) {
  return (
    <CategorySemanticPicker
      semanticTag={value}
      reportRole={reportRole}
      txnTypes={txnTypes}
      parentId={parentId}
      categoryCode={categoryCode}
      categoryName={categoryName}
      onSemanticTagChange={(tag, role) => {
        onChange?.(tag)
        onReportRoleSync?.(role)
      }}
      inferred={inferred}
      mismatchWarning={mismatchWarning}
    />
  )
}
