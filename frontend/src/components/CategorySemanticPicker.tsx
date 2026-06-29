import { Alert, Tag, Typography } from 'antd'
import {
  catalogFixedCostKindLabel,
  catalogSemanticTagLabel,
  useSemanticsCatalog,
} from '../hooks/useSemanticsCatalog'
import type { FixedCostKind, SemanticTagId } from '../utils/categorySemantics'
import {
  FIXED_COST_KIND_OPTIONS,
  compactGroupTitle,
  filterSemanticTagGroups,
  inclusionSummary,
  isFixedCategory,
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
  if (tag === 'fixed_spending') return 'purple'
  if (tag === 'subscription_spending') return 'geekblue'
  if (tag === 'dining_spending' || tag === 'daily_spending' || tag === 'other_expense') return 'orange'
  if (tag === 'shopping_spending') return 'gold'
  if (tag === 'transport_spending') return 'blue'
  if (tag === 'entertainment_spending') return 'volcano'
  if (tag === 'education_spending') return 'cyan'
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
  const activeFixedKind = preview.fixedCostKind as FixedCostKind | null | undefined
  const allGroups = (catalog?.semanticTagGroups ?? []).map((g) => ({
    title: g.title,
    appliesTo: g.appliesTo,
    tags: g.tags as SemanticTagId[],
  }))
  const tagGroups = filterSemanticTagGroups(allGroups.length ? allGroups : [], txnTypes)
  const reportSurfaces = (catalog?.reportSurfaces ?? []) as ReportSurface[]
  const txnFilter = txnTypeFilter(txnTypes)
  const parentWarning = parentTxnMismatchWarning(parentId, txnTypes)

  const selectTag = (tag: SemanticTagId) => {
    if (tag === 'subscription_spending') {
      onSemanticTagChange(tag, reportRoleFromSemanticSelection(tag, 'subscription'))
      return
    }
    let fixedKind = activeFixedKind
    if (tag === 'fixed_spending' && !fixedKind) {
      fixedKind = inferDefaultFixedKind(parentId, categoryCode)
    }
    if (tag !== 'fixed_spending') {
      fixedKind = null
    }
    onSemanticTagChange(tag, reportRoleForSemanticTag(tag, parentId, categoryCode, fixedKind))
  }

  const selectFixedKind = (kind: FixedCostKind) => {
    onSemanticTagChange('fixed_spending', reportRoleFromSemanticSelection('fixed_spending', kind))
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
            {activeFixedKind && activeTag === 'fixed_spending' && (
              <Tag color="purple" bordered={false}>
                {catalogFixedCostKindLabel(catalog, activeFixedKind)}
              </Tag>
            )}
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
        {tagGroups.map((group) => (
          <div key={group.title} className="fs-category-semantic-group fs-category-semantic-group--inline">
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
                    onSelect={() => selectTag(tag)}
                  />
                )
              })}
            </div>
          </div>
        ))}

        {activeTag === 'fixed_spending' && (
          <div className="fs-category-semantic-group fs-category-semantic-group--inline fs-category-semantic-group--fixed">
            <span className="fs-category-semantic-group-label">
              {catalog?.fixedCostKindSectionLabel ?? 'Type'}
            </span>
            <div className="fs-category-semantic-tag-row">
              {FIXED_COST_KIND_OPTIONS.filter(({ value }) => value !== 'subscription').map(({ value }) => (
                <SelectableTag
                  key={value}
                  selected={(activeFixedKind ?? 'rent') === value}
                  label={catalogFixedCostKindLabel(catalog, value)}
                  tone="fixed"
                  onSelect={() => selectFixedKind(value)}
                />
              ))}
            </div>
          </div>
        )}
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

function inferDefaultFixedKind(parentId?: string, categoryCode?: string): FixedCostKind {
  if (isFixedCategory(parentId, categoryCode)) {
    const code = (categoryCode ?? '').trim().toUpperCase()
    if (code === 'FIXED-02') return 'utilities'
    if (code === 'FIXED-03') return 'telecom'
    if (code === 'FIXED-04') return 'insurance'
    return 'rent'
  }
  return 'rent'
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
