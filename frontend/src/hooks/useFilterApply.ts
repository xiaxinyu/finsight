import { useCallback, useState } from 'react'

/** Draft vs applied filter state — commit on Apply to control fetch timing. */
export function useFilterApply<T>(initial: T) {
  const [draft, setDraft] = useState<T>(initial)
  const [applied, setApplied] = useState<T>(initial)
  const [applying, setApplying] = useState(false)

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

  return { draft, setDraft, applied, applying, apply, applySync }
}
