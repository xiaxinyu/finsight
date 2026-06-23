import type { ConsumeCategoryRow } from '../api/admin'
import type { TreeSelectNode } from '../hooks/useConsumeTree'

export type CategoryTreeNode = {
  key: string
  code: string
  title: string
  children?: CategoryTreeNode[]
}

function parentCodeOf(
  c: ConsumeCategoryRow,
  byCode: Map<string, ConsumeCategoryRow>,
  byId: Map<string, ConsumeCategoryRow>,
): string | null {
  const raw = String(c.parentId ?? '').trim()
  if (!raw) return null
  if (byCode.has(raw)) return raw
  const parent = byId.get(raw)
  return parent?.code ?? null
}

function indexCategories(categories: ConsumeCategoryRow[]) {
  const active = categories.filter((c) => c.deleted !== 1 && c.code)
  const byCode = new Map(active.map((c) => [c.code!, c]))
  const byId = new Map(active.filter((c) => c.id).map((c) => [c.id!, c]))
  return { active, byCode, byId }
}

export function buildCategoryTree(categories: ConsumeCategoryRow[]): CategoryTreeNode[] {
  const { active, byCode, byId } = indexCategories(categories)
  const childrenByParent = new Map<string, ConsumeCategoryRow[]>()

  for (const c of active) {
    const parentCode = parentCodeOf(c, byCode, byId)
    if (parentCode && byCode.has(parentCode)) {
      const list = childrenByParent.get(parentCode) ?? []
      list.push(c)
      childrenByParent.set(parentCode, list)
    }
  }

  const childCodes = new Set<string>()
  for (const kids of childrenByParent.values()) {
    for (const c of kids) {
      if (c.code) childCodes.add(c.code)
    }
  }

  const roots = active
    .filter((c) => !childCodes.has(c.code!))
    .sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0))

  const toNode = (c: ConsumeCategoryRow): CategoryTreeNode => ({
    key: c.id || c.code!,
    code: c.code!,
    title: c.name || c.code!,
    children: (childrenByParent.get(c.code!) ?? [])
      .sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0))
      .map(toNode),
  })

  return roots.map(toNode)
}

export function toAntTreeNodes(nodes: CategoryTreeNode[]): { key: string; title: string; children?: ReturnType<typeof toAntTreeNodes> }[] {
  return nodes.map((n) => ({
    key: n.key,
    title: n.title,
    children: n.children?.length ? toAntTreeNodes(n.children) : undefined,
  }))
}

type AssetCountSummary = { transactionCount?: number; activeRuleCount?: number }

export function toAntTreeNodesWithCounts(
  nodes: CategoryTreeNode[],
  summary: Record<string, AssetCountSummary>,
  flat: Array<{ id?: string; code?: string }>,
): { key: string; title: string; children?: ReturnType<typeof toAntTreeNodesWithCounts> }[] {
  const mapNode = (node: CategoryTreeNode): {
    key: string
    title: string
    txnTotal: number
    children?: ReturnType<typeof toAntTreeNodesWithCounts>
  } => {
    const cat = flat.find((c) => (c.id || c.code) === node.key)
    const code = cat?.code || node.code
    const direct = code ? summary[code] : undefined
    const childMapped = (node.children || []).map(mapNode)
    const txnTotal = (direct?.transactionCount ?? 0)
      + childMapped.reduce((sum, ch) => sum + ch.txnTotal, 0)
    const title = txnTotal > 0 ? `${node.title} (${txnTotal})` : node.title
    return {
      key: node.key,
      title,
      txnTotal,
      children: childMapped.length
        ? childMapped.map((ch) => ({ key: ch.key, title: ch.title, children: ch.children }))
        : undefined,
    }
  }
  return nodes.map((n) => {
    const mapped = mapNode(n)
    return { key: mapped.key, title: mapped.title, children: mapped.children }
  })
}

export type CategoryTreeSelectNode = {
  title: string
  value: string
  disabled?: boolean
  children?: CategoryTreeSelectNode[]
}

/** Codes in the source category subtree (self + descendants) — invalid merge targets. */
export function collectSubtreeCodes(categories: ConsumeCategoryRow[], rootCode: string): Set<string> {
  const { active, byCode, byId } = indexCategories(categories)
  const out = new Set<string>([rootCode])
  const queue = [rootCode]
  while (queue.length) {
    const parent = queue.pop()!
    for (const c of active) {
      const parentCode = parentCodeOf(c, byCode, byId)
      if (parentCode === parent && c.code && !out.has(c.code)) {
        out.add(c.code)
        queue.push(c.code)
      }
    }
  }
  return out
}

export function collectSubtreeCodesFromTree(nodes: TreeSelectNode[], rootCode: string): Set<string> {
  const out = new Set<string>()
  const walk = (node: TreeSelectNode, under: boolean) => {
    const include = under || node.value === rootCode
    if (include) out.add(node.value)
    for (const child of node.children ?? []) walk(child, include)
  }
  for (const node of nodes) walk(node, false)
  return out
}

export function toCategoryTreeSelect(
  nodes: CategoryTreeNode[],
  opts: { excludeCodes?: Set<string>; l1TargetsOnly?: boolean },
  depth = 1,
): CategoryTreeSelectNode[] {
  return nodes.map((n) => ({
    title: `${n.title} (${n.code})`,
    value: n.code,
    disabled: opts.excludeCodes?.has(n.code)
      || (opts.l1TargetsOnly === true && depth > 1),
    children: n.children?.length
      ? toCategoryTreeSelect(n.children, opts, depth + 1)
      : undefined,
  }))
}

export function applyMergeTargetConstraints(
  nodes: TreeSelectNode[],
  opts: { excludeCodes?: Set<string>; l1TargetsOnly?: boolean },
  depth = 1,
): CategoryTreeSelectNode[] {
  return nodes.map((n) => ({
    title: `${n.title} (${n.value})`,
    value: n.value,
    disabled: opts.excludeCodes?.has(n.value)
      || (opts.l1TargetsOnly === true && depth > 1),
    children: n.children?.length
      ? applyMergeTargetConstraints(n.children, opts, depth + 1)
      : undefined,
  }))
}

/** Category ids/codes that a rule's categoryId may reference. */
export function categoryKeys(cat: ConsumeCategoryRow): string[] {
  const keys = new Set<string>()
  if (cat.id) keys.add(cat.id)
  if (cat.code) keys.add(cat.code)
  return [...keys]
}

export function ruleMatchesCategory(
  categoryId: string | undefined,
  cat: ConsumeCategoryRow,
): boolean {
  if (!categoryId) return false
  return categoryKeys(cat).includes(categoryId)
}
