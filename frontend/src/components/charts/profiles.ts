import type { EChartsOption } from 'echarts'

function merge(target: EChartsOption, source: EChartsOption): EChartsOption {
  return { ...target, ...source, xAxis: source.xAxis ?? target.xAxis, yAxis: source.yAxis ?? target.yAxis, series: source.series ?? target.series }
}

function axisInterval(n: number) {
  if (n <= 12) return 0
  const target = Math.min(12, Math.max(6, 10))
  return Math.ceil(n / target) - 1
}

export function emptyChartOption(message = 'No data in range'): EChartsOption {
  return {
    title: {
      text: message,
      left: 'center',
      top: 'middle',
      textStyle: { color: '#94a3b8', fontSize: 14, fontWeight: 600 },
    },
    series: [],
  }
}

export function applyProfile(profile: string, option: EChartsOption): EChartsOption {
  if (profile === 'categoryBar') {
    return merge({
      title: { textStyle: { fontSize: 14, fontWeight: 700 }, left: 'left' },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 72, right: 24, top: 48, bottom: 72, containLabel: true },
      xAxis: { type: 'category', axisLabel: { fontSize: 11, interval: 0 } },
      yAxis: { type: 'value', scale: false, axisLabel: { fontSize: 11 } },
    }, option)
  }
  if (profile === 'donut') {
    return merge({
      tooltip: { trigger: 'item' },
      legend: { type: 'scroll', orient: 'vertical', left: 'left', textStyle: { fontSize: 12 } },
    }, option)
  }
  if (profile === 'compareBars') {
    return applyProfile('timeSeries', merge({ xAxis: { boundaryGap: true } }, option))
  }
  // timeSeries default
  const cats = (option.xAxis as { data?: string[] })?.data || []
  const rotate = cats.length > 28 ? 35 : 0
  return merge({
    title: { textStyle: { fontSize: 14, fontWeight: 700 }, left: 'left', top: 8 },
    legend: { textStyle: { fontSize: 12 }, top: 4 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    grid: { left: 56, right: 24, top: 52, bottom: rotate ? 78 : 64, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      axisLabel: { fontSize: 11, rotate, interval: axisInterval(cats.length) },
    },
    yAxis: { type: 'value', axisLabel: { fontSize: 11 }, splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } } },
  }, option)
}
