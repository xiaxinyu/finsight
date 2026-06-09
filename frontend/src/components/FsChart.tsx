import ReactECharts from 'echarts-for-react'
import type { EChartsOption } from 'echarts'
import { Spin } from 'antd'
import { applyProfile } from './charts/profiles'

type Props = {
  option: EChartsOption
  profile?: string
  height?: number | string
  loading?: boolean
  onEvents?: Record<string, (params: unknown) => void>
}

export function FsChart({ option, profile = 'timeSeries', height = 400, loading = false, onEvents }: Props) {
  const finalOption = applyProfile(profile, option)
  return (
    <div className="fs-chart-wrap" style={{ height, position: 'relative' }}>
      <ReactECharts
        option={finalOption}
        style={{ height: '100%', width: '100%' }}
        notMerge
        lazyUpdate
        onEvents={onEvents}
        opts={{ renderer: 'canvas' }}
      />
      {loading && (
        <div className="fs-chart-overlay">
          <Spin tip="Updating chart…" />
        </div>
      )}
    </div>
  )
}
