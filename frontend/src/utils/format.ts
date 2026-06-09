import dayjs from 'dayjs'

export function formatMoney(n: number | null | undefined, opts?: { symbol?: boolean }) {
  const v = Number(n) || 0
  const num = v.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return opts?.symbol === false ? num : `¥${num}`
}

export function formatNumber(n: number | null | undefined) {
  const v = Number(n)
  if (isNaN(v)) return ''
  return v.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

export function formatDateMmDdYyyy(d: dayjs.Dayjs | Date | string) {
  return dayjs(d).format('MM/DD/YYYY')
}

export function yearRange(year?: number | string) {
  const y = String(year || dayjs().year())
  return { start: `01/01/${y}`, end: `12/31/${y}` }
}

export const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

export function yearOptions(count = 16, fromYear = dayjs().year()) {
  return Array.from({ length: count }, (_, i) => {
    const y = fromYear - i
    return { value: y, label: String(y) }
  })
}
