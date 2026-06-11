import type { ConsumeCategoryRow } from '../api/admin'

export function categoryTitleMap(categories: ConsumeCategoryRow[]): Map<string, string> {
  const map = new Map<string, string>()
  for (const c of categories) {
    if (c.id) map.set(c.id, c.name || c.code || c.id)
    if (c.code) map.set(c.code, c.name || c.code)
  }
  return map
}

export function resolveCategoryTitle(map: Map<string, string>, id?: string | null): string {
  if (!id) return '—'
  return map.get(id) || id
}

/** Lookup including soft-deleted categories (shown as "Name (removed)"). */
export function resolveCategoryTitleExtended(
  activeMap: Map<string, string>,
  allCategories: { id?: string; code?: string; name?: string; deleted?: number }[],
  id?: string | null,
): string {
  if (!id) return '—'
  const active = activeMap.get(id)
  if (active) return active
  for (const c of allCategories) {
    if (c.deleted !== 1) continue
    const name = c.name || c.code || c.id || ''
    if (c.id === id || c.code === id) return `${name} (removed)`
  }
  return 'Unknown category'
}
