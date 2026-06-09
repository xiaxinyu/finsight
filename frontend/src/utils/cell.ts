import dayjs from 'dayjs'

/** Safe cell text — never renders [object Object]. */
export function cellText(value: unknown): string {
  if (value == null) return ''
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  return ''
}

/** Format API date (ISO string, timestamp, Date) for table display. */
export function formatTableDate(value: unknown): string {
  if (value == null || value === '') return ''
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}/.test(value)) {
    const d = dayjs(value.slice(0, 10))
    return d.isValid() ? d.format('MM/DD/YYYY') : cellText(value)
  }
  const d = dayjs(value as string | number | Date)
  return d.isValid() ? d.format('MM/DD/YYYY') : cellText(value)
}
