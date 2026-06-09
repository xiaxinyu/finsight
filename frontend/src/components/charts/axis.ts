import dayjs from 'dayjs'

/** X-axis label density: show 6–12 ticks across the span. */
export function axisLabelInterval(count: number): number | 'auto' {
  if (count <= 12) return 0
  const target = Math.min(12, Math.max(6, 10))
  return Math.ceil(count / target) - 1
}

/** Rotate labels when span exceeds 28 points (≈28 days). */
export function axisLabelRotation(count: number): number {
  return count > 28 ? 35 : 0
}

/** Format category labels as MM/DD when they look like dates. */
export function formatAxisDateLabel(label: string): string {
  if (!label) return label
  if (/^\d{1,2}\/\d{1,2}$/.test(label)) return label
  if (/^\d{1,2}\/\d{1,2}\/\d{4}$/.test(label)) {
    const d = dayjs(label, 'MM/DD/YYYY')
    return d.isValid() ? d.format('MM/DD') : label
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(label)) {
    const d = dayjs(label)
    return d.isValid() ? d.format('MM/DD') : label
  }
  return label
}

export function formatCategories(labels: string[]): string[] {
  return labels.map(formatAxisDateLabel)
}

export function daySpan(labels: string[]): number {
  return labels.length
}

export function hidePointMarkers(count: number): boolean {
  return count > 60
}
