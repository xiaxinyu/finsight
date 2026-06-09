import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'

type Range = [Dayjs, Dayjs]

export const dateRangePresets: { label: string; value: Range }[] = [
  { label: 'Today', value: [dayjs().startOf('day'), dayjs().endOf('day')] },
  { label: 'This week', value: [dayjs().startOf('week'), dayjs().endOf('day')] },
  { label: 'This month', value: [dayjs().startOf('month'), dayjs().endOf('day')] },
  { label: 'Last month', value: [dayjs().subtract(1, 'month').startOf('month'), dayjs().subtract(1, 'month').endOf('month')] },
  { label: 'YTD', value: [dayjs().startOf('year'), dayjs().endOf('day')] },
]
