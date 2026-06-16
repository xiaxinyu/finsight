import dayjs from 'dayjs'
import {
  detectPresetId,
  isAllTimePeriod,
  presetRange,
  type PeriodPresetId,
  type PeriodRange,
} from './periodPresets'

export function periodFromStrings(start: string, end: string): PeriodRange {
  if (!start?.trim() && !end?.trim()) {
    return presetRange('allTime')
  }
  return [dayjs(start, 'MM/DD/YYYY'), dayjs(end, 'MM/DD/YYYY')]
}

export function periodToStrings(
  range: PeriodRange,
  presetId?: PeriodPresetId,
): { start: string; end: string } {
  const id = presetId ?? detectPresetId(range)
  if (id === 'allTime' || isAllTimePeriod(range)) {
    return { start: '', end: '' }
  }
  return {
    start: range[0].format('MM/DD/YYYY'),
    end: range[1].format('MM/DD/YYYY'),
  }
}

