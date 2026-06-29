import { useQuery } from '@tanstack/react-query'
import { fetchSemanticsCatalog, type SemanticsCatalog } from '../api/admin'
import {
  FIXED_COST_KIND_LABELS,
  SEMANTIC_TAG_GROUPS,
  SEMANTIC_TAG_LABELS,
  filterSemanticTagGroups,
  type FixedCostKind,
  type SemanticTagId,
} from '../utils/categorySemantics'

const FALLBACK: SemanticsCatalog = {
  fieldLabel: 'Reporting Classification',
  previewSectionLabel: 'Report Impact Preview',
  fixedCostKindSectionLabel: 'Fixed Cost Type',
  semanticTagGroups: SEMANTIC_TAG_GROUPS.map((g) => ({
    title: g.title,
    appliesTo: g.appliesTo,
    tags: [...g.tags],
  })),
  semanticTags: Object.fromEntries(
    Object.entries(SEMANTIC_TAG_LABELS).map(([id, label]) => [id, { id, label }]),
  ),
  fixedCostKinds: Object.fromEntries(
    Object.entries(FIXED_COST_KIND_LABELS).map(([id, label]) => [id, { id, label }]),
  ),
  reportSurfaces: [
    { id: 'income', label: 'Income Trend' },
    { id: 'expense', label: 'Expense Trend' },
    { id: 'budget', label: 'Budget' },
    { id: 'fixed_cost', label: 'Fixed Cost Report' },
    { id: 'cashflow', label: 'Cash Flow' },
  ],
}

export function useSemanticsCatalog() {
  return useQuery({
    queryKey: ['semantics-catalog'],
    queryFn: fetchSemanticsCatalog,
    staleTime: 300_000,
    placeholderData: FALLBACK,
  })
}

export function catalogSemanticTagLabel(catalog: SemanticsCatalog | undefined, tag: SemanticTagId): string {
  return catalog?.semanticTags?.[tag]?.label ?? SEMANTIC_TAG_LABELS[tag] ?? tag
}

export function catalogFixedCostKindLabel(
  catalog: SemanticsCatalog | undefined,
  kind: FixedCostKind,
): string {
  return catalog?.fixedCostKinds?.[kind]?.label ?? FIXED_COST_KIND_LABELS[kind] ?? kind
}

export function catalogSemanticTagGroups(
  catalog: SemanticsCatalog | undefined,
  txnTypes?: string,
  parentId?: string,
  categoryCode?: string,
) {
  const groups = catalog?.semanticTagGroups?.length
    ? catalog.semanticTagGroups.map((g) => ({
      title: g.title,
      appliesTo: g.appliesTo,
      tags: g.tags as SemanticTagId[],
    }))
    : SEMANTIC_TAG_GROUPS
  return filterSemanticTagGroups(groups, txnTypes, parentId, categoryCode)
}
