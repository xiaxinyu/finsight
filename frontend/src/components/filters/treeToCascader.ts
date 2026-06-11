import type { TreeNode } from '../../api/transaction'

export type CascaderOption = {
  value: string
  label: string
  children?: CascaderOption[]
}

export function treeToCascaderOptions(nodes: TreeNode[]): CascaderOption[] {
  return nodes.map((n) => ({
    value: n.id,
    label: n.text,
    children: n.children?.length ? treeToCascaderOptions(n.children) : undefined,
  }))
}

export function findCascaderPath(
  nodes: CascaderOption[],
  target: string,
  trail: string[] = [],
): string[] | null {
  for (const n of nodes) {
    const next = [...trail, n.value]
    if (n.value === target) return next
    if (n.children?.length) {
      const found = findCascaderPath(n.children, target, next)
      if (found) return found
    }
  }
  return null
}

export function cascaderSearchFilter(inputValue: string, path: CascaderOption[]): boolean {
  const q = inputValue.trim().toLowerCase()
  if (!q) return true
  return path.some((p) => p.label.toLowerCase().includes(q))
}
