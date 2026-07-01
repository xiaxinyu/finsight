import type { FsMenuItem } from '../config/menuConfig'

const ADMIN_PREFIXES = ['/admin/']

function isAdminMenuItem(item: FsMenuItem): boolean {
  if (item.path && ADMIN_PREFIXES.some((p) => item.path!.startsWith(p))) {
    return true
  }
  if (item.key === 'admin') {
    return true
  }
  return false
}

export function filterMenuByRole(items: FsMenuItem[], isAdmin: boolean): FsMenuItem[] {
  if (isAdmin) {
    return items
  }
  const out: FsMenuItem[] = []
  for (const item of items) {
    if (item.type === 'group') {
      const children = filterMenuByRole(item.children ?? [], isAdmin)
      if (children.length === 0) continue
      out.push({ ...item, children })
      continue
    }
    if (item.children?.length) {
      if (isAdminMenuItem(item)) {
        continue
      }
      const children = filterMenuByRole(item.children, isAdmin)
      if (children.length === 0) continue
      out.push({ ...item, children })
      continue
    }
    if (isAdminMenuItem(item)) {
      continue
    }
    out.push(item)
  }
  return out
}
