import type { ConsumeCategoryRow } from '../api/admin'

export type CategoryTreeNode = {
  key: string
  code: string
  title: string
  children?: CategoryTreeNode[]
}

export function buildCategoryTree(categories: ConsumeCategoryRow[]): CategoryTreeNode[] {
  const active = categories.filter((c) => c.deleted !== 1)
  const roots = active.filter((c) => c.level === 1 || !c.parentId)
  const childrenByParent = new Map<string, ConsumeCategoryRow[]>()
  for (const c of active) {
    if (c.level === 2 && c.parentId) {
      const list = childrenByParent.get(c.parentId) || []
      list.push(c)
      childrenByParent.set(c.parentId, list)
    }
  }
  const toNode = (c: ConsumeCategoryRow): CategoryTreeNode => ({
    key: c.id || c.code || '',
    code: c.code || c.id || '',
    title: c.name || c.code || c.id || '',
    children: (childrenByParent.get(c.code || '') || [])
      .sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0))
      .map(toNode),
  })
  return roots.sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0)).map(toNode)
}

export function toAntTreeNodes(nodes: CategoryTreeNode[]): { key: string; title: string; children?: ReturnType<typeof toAntTreeNodes> }[] {
  return nodes.map((n) => ({
    key: n.key,
    title: n.title,
    children: n.children?.length ? toAntTreeNodes(n.children) : undefined,
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
