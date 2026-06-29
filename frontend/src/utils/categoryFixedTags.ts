/** Flat fixed-cost tags — one click, no secondary picker. */
export const FLAT_FIXED_SEMANTIC_TAGS = [
  'fixed_housing',
  'fixed_utilities',
  'fixed_telecom',
  'fixed_insurance',
  'fixed_tuition',
  'fixed_repayment',
  'fixed_misc',
] as const

export type FlatFixedSemanticTag = typeof FLAT_FIXED_SEMANTIC_TAGS[number]

export type FixedCostKind =
  | 'rent'
  | 'utilities'
  | 'telecom'
  | 'insurance'
  | 'subscription'
  | 'education'
  | 'repayment'
  | 'other'

const KIND_TO_FLAT: Record<FixedCostKind, FlatFixedSemanticTag> = {
  rent: 'fixed_housing',
  utilities: 'fixed_utilities',
  telecom: 'fixed_telecom',
  insurance: 'fixed_insurance',
  subscription: 'fixed_misc',
  education: 'fixed_tuition',
  repayment: 'fixed_repayment',
  other: 'fixed_misc',
}

const FLAT_TO_KIND: Record<FlatFixedSemanticTag, FixedCostKind> = {
  fixed_housing: 'rent',
  fixed_utilities: 'utilities',
  fixed_telecom: 'telecom',
  fixed_insurance: 'insurance',
  fixed_tuition: 'education',
  fixed_repayment: 'repayment',
  fixed_misc: 'other',
}

export function isFlatFixedSemanticTag(tag?: string | null): tag is FlatFixedSemanticTag {
  return Boolean(tag && (FLAT_FIXED_SEMANTIC_TAGS as readonly string[]).includes(tag))
}

export function isAnyFixedSemanticTag(tag?: string | null): boolean {
  return tag === 'fixed_spending' || isFlatFixedSemanticTag(tag)
}

export function flatFixedTagForKind(kind?: FixedCostKind | null): FlatFixedSemanticTag {
  if (!kind) return 'fixed_housing'
  return KIND_TO_FLAT[kind] ?? 'fixed_misc'
}

export function fixedKindFromFlatTag(tag: string): FixedCostKind | null {
  if (isFlatFixedSemanticTag(tag)) return FLAT_TO_KIND[tag]
  return null
}

export function normalizeFixedSemanticTag(
  tag?: string | null,
  fixedKind?: FixedCostKind | null,
): FlatFixedSemanticTag | string {
  if (isFlatFixedSemanticTag(tag)) return tag
  if (tag === 'fixed_spending' && fixedKind) return flatFixedTagForKind(fixedKind)
  if (tag === 'fixed_spending') return 'fixed_housing'
  return tag ?? 'other'
}
