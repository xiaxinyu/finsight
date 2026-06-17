import dayjs, { type Dayjs } from 'dayjs'
import quarterOfYear from 'dayjs/plugin/quarterOfYear'

dayjs.extend(quarterOfYear)

export type PeriodRange = [Dayjs, Dayjs]
/** Sentinel range for “no date filter” — never sent to the API as literal bounds. */
export const ALL_TIME_PERIOD: PeriodRange = [dayjs('1900-01-01'), dayjs('1900-01-01')]

export type PeriodPresetId =
  | 'allTime'
  | 'last7'
  | 'last30'
  | 'thisMonth'
  | 'thisQuarter'
  | 'thisYear'
  | 'lastYear'
  | 'lastMonth'
  | 'lastQuarter'
  | 'last3Months'
  | 'last6Months'
  | 'last12Months'
  | 'custom'

export type PeriodSection = 'recommended' | 'relative' | 'calendar'

type PresetDef = {
  label: string
  range: () => PeriodRange
  shift?: (range: PeriodRange, dir: -1 | 1) => PeriodRange
}

const endToday = () => dayjs().endOf('day')

function shiftByMonths(range: PeriodRange, dir: -1 | 1, months: number): PeriodRange {
  const start = range[0].add(dir * months, 'month').startOf('month')
  const endCandidate = start.add(months - 1, 'month').endOf('month')
  const end = endCandidate.isAfter(dayjs()) ? endToday() : endCandidate
  return [start, end]
}

function shiftByDays(range: PeriodRange, dir: -1 | 1): PeriodRange {
  const days = range[1].diff(range[0], 'day') + 1
  const start = range[0].add(dir * days, 'day')
  return [start.startOf('day'), start.add(days - 1, 'day').endOf('day')]
}

function shiftMonthWindow(range: PeriodRange, dir: -1 | 1, anchorToMonthEnd: boolean): PeriodRange {
  const anchor = range[0].add(dir, 'month')
  const isCurrent = anchor.isSame(dayjs(), 'month')
  return [anchor.startOf('month'), anchorToMonthEnd && !isCurrent ? anchor.endOf('month') : endToday()]
}

export const PERIOD_SECTION_LABELS: Record<PeriodSection, string> = {
  recommended: 'Recommended',
  relative: 'Relative dates',
  calendar: 'Calendar months',
}

type BuiltinPresetId = Exclude<PeriodPresetId, 'custom'>

export const SECTION_PRESET_IDS: Record<PeriodSection, BuiltinPresetId[]> = {
  recommended: ['thisYear', 'allTime', 'last7', 'thisMonth', 'last12Months'],
  relative: ['last30'],
  calendar: [
    'thisQuarter', 'thisYear', 'lastYear', 'lastMonth', 'lastQuarter',
    'last3Months', 'last6Months',
  ],
}

const PRESETS: Record<BuiltinPresetId, PresetDef> = {
  allTime: {
    label: 'All time',
    range: () => ALL_TIME_PERIOD,
  },
  last7: {
    label: 'Last 7 days',
    range: () => [dayjs().subtract(6, 'day').startOf('day'), endToday()],
    shift: shiftByDays,
  },
  last30: {
    label: 'Last 30 days',
    range: () => [dayjs().subtract(29, 'day').startOf('day'), endToday()],
    shift: shiftByDays,
  },
  thisMonth: {
    label: 'This month',
    range: () => [dayjs().startOf('month'), endToday()],
    shift: (range, dir) => shiftMonthWindow(range, dir, true),
  },
  thisQuarter: {
    label: 'This quarter',
    range: () => [dayjs().startOf('quarter'), endToday()],
    shift: (range, dir) => {
      const anchor = range[0].add(dir, 'quarter')
      const isCurrent = anchor.isSame(dayjs(), 'quarter')
      return [anchor.startOf('quarter'), isCurrent ? endToday() : anchor.endOf('quarter')]
    },
  },
  thisYear: {
    label: 'This year',
    range: () => [dayjs().startOf('year'), endToday()],
    shift: (range, dir) => {
      const anchor = range[0].add(dir, 'year')
      const isCurrent = anchor.isSame(dayjs(), 'year')
      return [anchor.startOf('year'), isCurrent ? endToday() : anchor.endOf('year')]
    },
  },
  lastYear: {
    label: 'Last year',
    range: () => {
      const y = dayjs().subtract(1, 'year')
      return [y.startOf('year'), y.endOf('year')]
    },
    shift: (range, dir) => {
      const anchor = range[0].add(dir, 'year')
      return [anchor.startOf('year'), anchor.endOf('year')]
    },
  },
  lastMonth: {
    label: 'Last month',
    range: () => {
      const m = dayjs().subtract(1, 'month')
      return [m.startOf('month'), m.endOf('month')]
    },
    shift: (range, dir) => {
      const anchor = range[0].add(dir, 'month')
      return [anchor.startOf('month'), anchor.endOf('month')]
    },
  },
  lastQuarter: {
    label: 'Last quarter',
    range: () => {
      const q = dayjs().subtract(1, 'quarter')
      return [q.startOf('quarter'), q.endOf('quarter')]
    },
    shift: (range, dir) => {
      const anchor = range[0].add(dir, 'quarter')
      return [anchor.startOf('quarter'), anchor.endOf('quarter')]
    },
  },
  last3Months: {
    label: 'Last 3 months',
    range: () => [dayjs().subtract(2, 'month').startOf('month'), endToday()],
    shift: (range, dir) => shiftByMonths(range, dir, 3),
  },
  last6Months: {
    label: 'Last 6 months',
    range: () => [dayjs().subtract(5, 'month').startOf('month'), endToday()],
    shift: (range, dir) => shiftByMonths(range, dir, 6),
  },
  last12Months: {
    label: 'Last 12 months',
    range: () => [dayjs().subtract(11, 'month').startOf('month'), endToday()],
    shift: (range, dir) => shiftByMonths(range, dir, 12),
  },
}

export function isAllTimePeriod(range: PeriodRange): boolean {
  return range[0].isSame(ALL_TIME_PERIOD[0], 'day') && range[1].isSame(ALL_TIME_PERIOD[1], 'day')
}

export function formatPeriodPreview(start: Dayjs, end: Dayjs): string {
  if (isAllTimePeriod([start, end])) return 'No date filter'
  if (start.isSame(end, 'month') && start.isSame(end, 'year')) {
    if (start.isSame(start.startOf('month'), 'day') && end.isSame(end.endOf('month'), 'day')) {
      return start.format('MMM YYYY')
    }
    return `${start.format('MMM D')}–${end.format('D, YYYY')}`
  }
  if (start.year() === end.year()) {
    return `${start.format('MMM')}–${end.format('MMM YYYY')}`
  }
  return `${start.format('MMM YYYY')} – ${end.format('MMM YYYY')}`
}

export function defaultPeriodRange(): PeriodRange {
  return PRESETS.thisYear.range()
}

/** MM/DD/YYYY bounds for API filters — same as {@link defaultPeriodRange}. */
export function defaultPeriodStrings(): { start: string; end: string } {
  const [start, end] = defaultPeriodRange()
  return {
    start: start.format('MM/DD/YYYY'),
    end: end.format('MM/DD/YYYY'),
  }
}

export function defaultComparePeriodRange(anchor?: PeriodRange): PeriodRange {
  const [start, end] = anchor ?? defaultPeriodRange()
  return [start.subtract(1, 'year'), end.subtract(1, 'year')]
}

export function presetRange(id: PeriodPresetId): PeriodRange {
  if (id === 'custom') return defaultPeriodRange()
  return PRESETS[id].range()
}

export function presetLabel(id: PeriodPresetId): string {
  if (id === 'custom') return 'Custom date range'
  return PRESETS[id].label
}

export function detectPresetId(range: PeriodRange): PeriodPresetId {
  if (isAllTimePeriod(range)) return 'allTime'
  for (const id of Object.keys(PRESETS) as BuiltinPresetId[]) {
    if (id === 'allTime') continue
    const [s, e] = PRESETS[id].range()
    if (s.isSame(range[0], 'day') && e.isSame(range[1], 'day')) {
      return id
    }
  }
  return 'custom'
}

export function presetsForSection(section: PeriodSection): Array<{ id: BuiltinPresetId; label: string; range: PeriodRange }> {
  return SECTION_PRESET_IDS[section].map((id) => ({
    id,
    label: PRESETS[id].label,
    range: PRESETS[id].range(),
  }))
}

export function shiftPeriod(range: PeriodRange, presetId: PeriodPresetId, dir: -1 | 1): PeriodRange {
  if (presetId === 'allTime') return range
  if (presetId !== 'custom') {
    const preset = PRESETS[presetId]
    if (preset?.shift) return preset.shift(range, dir)
  }
  return shiftByDays(range, dir)
}

export function periodTriggerLabel(range: PeriodRange, presetId: PeriodPresetId): string {
  if (presetId === 'allTime' || isAllTimePeriod(range)) return PRESETS.allTime.label
  if (presetId !== 'custom') return PRESETS[presetId].label
  return formatPeriodPreview(range[0], range[1])
}
