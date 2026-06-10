import { useEffect, useState, type RefObject } from 'react'

const MIN_HEIGHT = 160

/**
 * Sizes an Ant Design Table/ProTable `scroll.y` body to fill a flex panel.
 * Chrome (toolbar, header, pagination) stays fixed; only tbody scrolls.
 */
export function useFillTableHeight(panelRef: RefObject<HTMLElement | null>) {
  const [height, setHeight] = useState(MIN_HEIGHT)

  useEffect(() => {
    const panel = panelRef.current
    if (!panel) {
      return undefined
    }

    const measure = () => {
      const toolbar = panel.querySelector<HTMLElement>('.ant-pro-table-list-toolbar')
      const thead = panel.querySelector<HTMLElement>('.ant-table-thead')
      const pagination = panel.querySelector<HTMLElement>('.ant-pagination')
      const chrome =
        (toolbar?.offsetHeight ?? 0) +
        (thead?.offsetHeight ?? 0) +
        (pagination?.offsetHeight ?? 0) +
        2
      setHeight(Math.max(MIN_HEIGHT, panel.clientHeight - chrome))
    }

    measure()
    const ro = new ResizeObserver(measure)
    ro.observe(panel)
    const mo = new MutationObserver(measure)
    mo.observe(panel, { childList: true, subtree: true, attributes: true })
    window.addEventListener('resize', measure)

    return () => {
      ro.disconnect()
      mo.disconnect()
      window.removeEventListener('resize', measure)
    }
  }, [panelRef])

  return height
}
