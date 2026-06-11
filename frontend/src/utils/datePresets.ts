import type { Dayjs } from 'dayjs'
import { defaultPeriodRange, presetRange } from './periodPresets'

type Range = [Dayjs, Dayjs]

/** Legacy Ant Design RangePicker presets — prefer PeriodRangePicker */
export const dateRangePresets: { label: string; value: Range }[] = [
  { label: 'This year', value: defaultPeriodRange() },
  { label: 'All time', value: presetRange('allTime') },
  { label: 'Last 7 days', value: presetRange('last7') },
  { label: 'This month', value: presetRange('thisMonth') },
  { label: 'Last month', value: presetRange('lastMonth') },
  { label: 'Last year', value: presetRange('lastYear') },
]
