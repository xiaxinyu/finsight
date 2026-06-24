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

export function isDateLikeLabel(label: string): boolean {
  if (!label) return false
  return /^\d{1,2}\/\d{1,2}/.test(label) || /^\d{4}-\d{2}/.test(label)
}

/** Rotation for categorical (non-date) x-axis labels — long Chinese names need steeper angles. */
export function categoryLabelRotation(labels: string[]): number {
  const textLabels = labels.filter((l) => l && !isDateLikeLabel(l))
  if (!textLabels.length) return axisLabelRotation(labels.length)
  const maxLen = textLabels.reduce((m, l) => Math.max(m, l.length), 0)
  const count = labels.length
  if (maxLen > 14 || count > 8) return 40
  if (maxLen > 10 || count > 6) return 35
  if (maxLen > 6 || count > 4) return 28
  if (maxLen > 4) return 18
  return 0
}

export function categoryAxisBottomMargin(rotate: number): number {
  if (rotate >= 35) return 96
  if (rotate >= 25) return 80
  if (rotate > 0) return 64
  return 48
}

export function categoryLabelWidth(labels: string[]): number {
  const maxLen = labels.reduce((m, l) => Math.max(m, String(l).length), 0)
  return Math.min(200, Math.max(72, maxLen * 7))
}

export function truncateCategoryLabel(label: string, max = 12): string {
  if (!label || label.length <= max) return label
  return `${label.slice(0, Math.max(1, max - 1))}…`
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
