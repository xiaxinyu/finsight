import type { Dayjs } from 'dayjs'
import { defaultPeriodRange, presetRange } from './periodPresets'

type Range = [Dayjs, Dayjs]

/** Legacy Ant Design RangePicker presets — prefer PeriodRangePicker */
export const dateRangePresets: { label: string; value: Range }[] = [
  { label: 'Last 7 days', value: presetRange('last7') },
  { label: 'This month', value: presetRange('thisMonth') },
  { label: 'Last month', value: presetRange('lastMonth') },
  { label: 'This year', value: defaultPeriodRange() },
]
