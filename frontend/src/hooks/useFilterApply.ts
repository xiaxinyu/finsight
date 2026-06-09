import { useCallback, useMemo, useState } from 'react'

function normalizeFilterValue(v: unknown): unknown {
  if (v != null && typeof v === 'object' && 'format' in v && typeof (v as { format: (f: string) => string }).format === 'function') {
    return (v as { format: (f: string) => string }).format('MM/DD/YYYY')
  }
  if (Array.isArray(v)) return v.map(normalizeFilterValue)
  if (v != null && typeof v === 'object') {
    return Object.fromEntries(Object.entries(v).map(([k, val]) => [k, normalizeFilterValue(val)]))
  }
  return v
}

/** Compare draft vs applied filters (handles dayjs ranges). */
export function isFilterDirty<T>(draft: T, applied: T): boolean {
  return JSON.stringify(normalizeFilterValue(draft)) !== JSON.stringify(normalizeFilterValue(applied))
}

/** Draft vs applied filter state — commit on Apply to control fetch timing. */
export function useFilterApply<T>(initial: T) {
  const [draft, setDraft] = useState<T>(initial)
  const [applied, setApplied] = useState<T>(initial)
  const [applying, setApplying] = useState(false)

  const isDirty = useMemo(() => isFilterDirty(draft, applied), [draft, applied])

  const apply = useCallback(async (run: () => Promise<unknown>) => {
    setApplying(true)
    setApplied(draft)
    try {
      await run()
    } finally {
      setApplying(false)
    }
  }, [draft])

  const applySync = useCallback(() => {
    setApplied(draft)
  }, [draft])

  return { draft, setDraft, applied, applying, isDirty, apply, applySync }
}
