import type { EChartsOption } from 'echarts'
import type { ProfileHistoryPoint } from '../../api/analytics'
import { PROFILE_DIM_LABELS } from './profileRadar'

export function historyDateRange(asOf: string, months = 6): { from: string; to: string } {
  const end = new Date(`${asOf}T12:00:00`)
  const start = new Date(end)
  start.setMonth(start.getMonth() - months)
  return {
    from: start.toISOString().slice(0, 10),
    to: end.toISOString().slice(0, 10),
  }
}

/** Latest score per calendar day for charting. */
export function aggregateHistoryByDay(points: ProfileHistoryPoint[] | undefined): ProfileHistoryPoint[] {
  if (!points?.length) return []
  const byDay = new Map<string, ProfileHistoryPoint>()
  for (const p of points) {
    const day = String(p.snapshotDate).slice(0, 10)
    const existing = byDay.get(day)
    if (!existing || Number(p.score) >= Number(existing.score)) {
      byDay.set(day, { ...p, snapshotDate: day })
    }
  }
  return [...byDay.values()].sort((a, b) => String(a.snapshotDate).localeCompare(String(b.snapshotDate)))
}

export function buildProfileHistoryOption(
  dimensionId: string,
  points: ProfileHistoryPoint[] | undefined,
): EChartsOption {
  const series = aggregateHistoryByDay(points)
  if (!series.length) {
    return {
      title: { text: 'No history yet', left: 'center', top: 'middle', textStyle: { color: '#94a3b8', fontSize: 13 } },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value', min: 0, max: 100 },
      series: [],
    }
  }

  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 24, bottom: 28 },
    xAxis: {
      type: 'category',
      data: series.map((p) => String(p.snapshotDate).slice(5)),
      boundaryGap: false,
    },
    yAxis: { type: 'value', min: 0, max: 100, splitLine: { lineStyle: { type: 'dashed' } } },
    series: [{
      type: 'line',
      name: PROFILE_DIM_LABELS[dimensionId] || dimensionId,
      data: series.map((p) => Number(p.score)),
      smooth: true,
      showSymbol: series.length <= 12,
      areaStyle: { opacity: 0.08 },
      lineStyle: { width: 2, color: '#2563eb' },
      itemStyle: { color: '#2563eb' },
    }],
  }
}
