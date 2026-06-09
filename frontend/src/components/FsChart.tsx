import ReactECharts from 'echarts-for-react'
import type { EChartsOption } from 'echarts'
import { applyProfile } from './charts/profiles'

type Props = {
  option: EChartsOption
  profile?: string
  height?: number | string
  onEvents?: Record<string, (params: unknown) => void>
}

export function FsChart({ option, profile = 'timeSeries', height = 360, onEvents }: Props) {
  const finalOption = applyProfile(profile, option)
  return (
    <ReactECharts
      option={finalOption}
      style={{ height, width: '100%' }}
      notMerge
      lazyUpdate
      onEvents={onEvents}
    />
  )
}
