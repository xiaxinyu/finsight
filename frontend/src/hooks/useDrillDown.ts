import { useCallback, useState } from 'react'
import type { DrillDownContext } from '../components/drilldown/types'

export function useDrillDown() {
  const [open, setOpen] = useState(false)
  const [context, setContext] = useState<DrillDownContext | null>(null)

  const openDrill = useCallback((ctx: DrillDownContext) => {
    setContext(ctx)
    setOpen(true)
  }, [])

  const closeDrill = useCallback(() => {
    setOpen(false)
    setContext(null)
  }, [])

  return { open, context, openDrill, closeDrill }
}
