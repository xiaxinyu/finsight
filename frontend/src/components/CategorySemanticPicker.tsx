import { useEffect, useState } from 'react'
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
  isCapitalSemanticTag,
  isFlatFixedSemanticTag,
  isLegacySemanticTag,
  parentTxnMismatchWarning,
  profileCategorySemantics,
  reportRoleForSemanticTag,
  reportRoleFromSemanticSelection,
  resolveSemanticTag,
  semanticTagLabel,
  SEMANTIC_GROUP_HINTS,
  shouldHideCapitalRow,
  txnTypeFilter,
  visibleSemanticTagsForGroup,
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
  if (tag === 'groceries_spending') return 'lime'
  if (tag === 'shopping_spending') return 'gold'
  if (tag === 'transport_spending') return 'blue'
  if (tag === 'entertainment_spending') return 'volcano'
  if (tag === 'education_spending') return 'cyan'
  if (tag === 'medical_spending') return 'green'
  if (tag === 'social_spending') return 'magenta'
  if (tag === 'real_income' || tag === 'other_income') return 'green'
  if (tag === 'investment_income') return 'purple'
  if (tag === 'refund_reimbursement' || tag === 'tax_refund') return 'blue'
  if (tag === 'essential_spending' || tag === 'tax_expense') return 'cyan'
  if (tag === 'finance_fee') return 'magenta'
  if (tag === 'finance_loan' || tag === 'finance_credit_loan' || tag === 'finance_installment' || tag === 'liability') {
    return 'volcano'
  }
  if (tag === 'transfer') return 'geekblue'
  if (tag === 'investment') return 'purple'
  if (tag === 'asset_adjustment') return 'gold'
  return 'default'
}

type ReportSurface = { id: string; label: string }

function txnBadgeLabel(filter: ReturnType<typeof txnTypeFilter>): string {
  if (filter === 'income') return 'Income'
  if (filter === 'expense') return 'Expense'
  return 'Mixed'
}

function isFinanceTagGroup(title: string): boolean {
  return title === 'Finance'
}

function isFixedTagGroup(title: string): boolean {
  return title === 'Fixed' || title === 'Fixed Commitments'
}

function isTaxTagGroup(title: string): boolean {
  return title === 'Tax'
}

function SelectableTag({
  selected,
  label,
  description,
  tone,
  legacy,
  onSelect,
}: {
  selected: boolean
  label: string
  description?: string
  tone?: 'fixed' | 'default'
  legacy?: boolean
  onSelect: () => void
}) {
  return (
    <button
      type="button"
      title={description}
      aria-pressed={selected}
      className={`fs-category-semantic-tag${tone === 'fixed' ? ' fs-category-semantic-tag--fixed' : ''}${legacy ? ' fs-category-semantic-tag--legacy' : ''}${selected ? ' fs-category-semantic-tag--active' : ''}`}
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
  const capitalHiddenByDefault = shouldHideCapitalRow(parentId, categoryCode, txnTypes)
  const [capitalExpanded, setCapitalExpanded] = useState(() => isCapitalSemanticTag(activeTag))

  useEffect(() => {
    setCapitalExpanded(isCapitalSemanticTag(activeTag))
  }, [parentId, categoryCode, txnTypes, activeTag])

  const preview = profileCategorySemantics(reportRole, txnTypes, parentId, categoryCode, activeTag, categoryName)
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
    { includeCapital: capitalExpanded || !capitalHiddenByDefault },
  )
  const groupHints = { ...SEMANTIC_GROUP_HINTS, ...(catalog?.groupHints ?? {}) }
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
              {isLegacySemanticTag(activeTag) && (
                <span className="fs-category-selection-legacy"> (legacy)</span>
              )}
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

      <div className="fs-category-classification-grid">
        {tagGroups.map((group) => {
          const fixedRow = isFixedTagGroup(group.title)
          const financeRow = isFinanceTagGroup(group.title)
          const taxRow = isTaxTagGroup(group.title)
          const hint = groupHints[group.title]
          const visibleTags = visibleSemanticTagsForGroup(group, activeTag)
          const groupKey = group.title.toLowerCase().replace(/\s+/g, '-')
          return (
          <section
            key={group.title}
            className={`fs-category-classification-card fs-category-classification-card--${groupKey}${fixedRow ? ' fs-category-classification-card--fixed' : ''}${financeRow ? ' fs-category-classification-card--finance' : ''}${taxRow ? ' fs-category-classification-card--tax' : ''}`}
          >
            <header className="fs-category-classification-card__head">
              <span className="fs-category-classification-card__title">
                {compactGroupTitle(group.title, txnFilter)}
              </span>
              {hint && (
                <Typography.Text type="secondary" className="fs-category-classification-card__hint">
                  {hint}
                </Typography.Text>
              )}
            </header>
            <div className="fs-category-semantic-tag-grid">
              {visibleTags.map((tag) => {
                const meta = catalog?.semanticTags?.[tag]
                return (
                  <SelectableTag
                    key={tag}
                    selected={activeTag === tag}
                    label={catalogSemanticTagLabel(catalog, tag)}
                    description={meta?.description}
                    tone={fixedRow ? 'fixed' : 'default'}
                    legacy={isLegacySemanticTag(tag)}
                    onSelect={() => selectTag(tag)}
                  />
                )
              })}
            </div>
          </section>
          )
        })}
        {capitalHiddenByDefault && !capitalExpanded && (
          <button
            type="button"
            className="fs-category-capital-expand"
            onClick={() => setCapitalExpanded(true)}
          >
            Transfer &amp; finance…
          </button>
        )}
        {isLegacySemanticTag(activeTag) && (
          <Typography.Text type="secondary" className="fs-category-legacy-note">
            Legacy tag &quot;Debt&quot; — save as Loan, Credit loan, or Installment when you edit this category.
          </Typography.Text>
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
