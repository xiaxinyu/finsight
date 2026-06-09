import { useEffect, useState } from 'react'

const MIN_HEIGHT = 320

export function useViewportTableHeight(offset = 220) {
  const [height, setHeight] = useState(MIN_HEIGHT)

  useEffect(() => {
    const update = () => setHeight(Math.max(MIN_HEIGHT, window.innerHeight - offset))
    update()
    window.addEventListener('resize', update)
    return () => window.removeEventListener('resize', update)
  }, [offset])

  return height
}
