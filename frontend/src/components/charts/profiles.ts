import type { EChartsOption, SeriesOption } from 'echarts'
import {
  axisLabelInterval,
  axisLabelRotation,
  daySpan,
  formatAxisDateLabel,
  formatCategories,
  hidePointMarkers,
} from './axis'

const FONT_AXIS = 11
const FONT_LEGEND = 12
const FONT_TITLE = 14
const FONT_TOOLTIP = 12

function merge(target: EChartsOption, source: EChartsOption): EChartsOption {
  return {
    ...target,
    ...source,
    title: { ...(target.title as object), ...(source.title as object) },
    tooltip: { ...(target.tooltip as object), ...(source.tooltip as object) },
    legend: { ...(target.legend as object), ...(source.legend as object) },
    grid: { ...(target.grid as object), ...(source.grid as object) },
    xAxis: source.xAxis ?? target.xAxis,
    yAxis: source.yAxis ?? target.yAxis,
    series: source.series ?? target.series,
    dataZoom: source.dataZoom ?? target.dataZoom,
  }
}

function baseTooltip(): EChartsOption['tooltip'] {
  return {
    trigger: 'axis',
    axisPointer: {
      type: 'cross',
      crossStyle: { color: '#94a3b8', width: 1 },
      lineStyle: { color: '#94a3b8', type: 'dashed' },
    },
    textStyle: { fontSize: FONT_TOOLTIP },
    confine: true,
    formatter(params: unknown) {
      const items = Array.isArray(params) ? params : [params]
      if (!items.length) return ''
      const first = items[0] as { axisValue?: string; name?: string }
      const title = formatAxisDateLabel(String(first.axisValue ?? first.name ?? ''))
      const lines = items.map((p) => {
        const row = p as { marker?: string; seriesName?: string; value?: number | number[] }
        const val = Array.isArray(row.value) ? row.value[1] : row.value
        const num = Number(val)
        const formatted = Number.isFinite(num)
          ? num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
          : String(val ?? '')
        return `${row.marker ?? ''}${row.seriesName ?? ''}: ${formatted}`
      })
      return `<div style="font-size:${FONT_TOOLTIP}px"><b>${title}</b><br/>${lines.join('<br/>')}</div>`
    },
  }
}

function enhanceSeries(series: SeriesOption[] | undefined, pointCount: number): SeriesOption[] {
  if (!series?.length) return []
  const hideSymbols = hidePointMarkers(pointCount)
  return series.map((s, idx) => {
    const line = s as SeriesOption & { lineStyle?: { type?: string } }
    const isCompareBaseline = line.name === 'Baseline' || line.name === '基准'
    return {
      ...s,
      showSymbol: hideSymbols ? false : true,
      symbolSize: hideSymbols ? 0 : 6,
      emphasis: {
        focus: 'series',
        scale: !hideSymbols,
        itemStyle: { shadowBlur: 8, shadowColor: 'rgba(37,99,235,0.35)' },
      },
      ...(s.type === 'line' && !hideSymbols ? { symbolSize: 8 } : {}),
      ...(s.type === 'line'
        ? {
            lineStyle: {
              ...(line.lineStyle || {}),
              type: isCompareBaseline ? 'dashed' : 'solid',
              width: isCompareBaseline ? 2 : 2.5,
            },
          }
        : {}),
      ...(idx === 0 && s.type === 'line' ? {} : {}),
    }
  })
}

function categoryXAxis(labels: string[], boundaryGap = true): EChartsOption['xAxis'] {
  const formatted = formatCategories(labels)
  const count = daySpan(formatted)
  const rotate = axisLabelRotation(count)
  return {
    type: 'category',
    boundaryGap,
    data: formatted,
    axisLabel: {
      fontSize: FONT_AXIS,
      rotate,
      interval: axisLabelInterval(count),
      formatter: (v: string) => formatAxisDateLabel(v),
    },
    axisTick: { alignWithLabel: true },
  }
}

function valueYAxis(): EChartsOption['yAxis'] {
  return {
    type: 'value',
    scale: true,
    axisLabel: { fontSize: FONT_AXIS },
    splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } },
  }
}

function optionalDataZoom(count: number): EChartsOption['dataZoom'] {
  if (count <= 14) return undefined
  return [
    { type: 'inside', throttle: 50 },
    { type: 'slider', height: 18, bottom: 4, textStyle: { fontSize: 10 } },
  ]
}

export function emptyChartOption(message = 'No data in range'): EChartsOption {
  return {
    title: {
      text: message,
      left: 'center',
      top: 'middle',
      textStyle: { color: '#94a3b8', fontSize: FONT_TITLE, fontWeight: 600 },
    },
    series: [],
  }
}

export function applyProfile(profile: string, option: EChartsOption): EChartsOption {
  const rawCats = ((option.xAxis as { data?: string[] })?.data) || []
  const cats = formatCategories(rawCats)
  const count = cats.length
  const rotate = axisLabelRotation(count)

  const base: EChartsOption = {
    textStyle: { fontFamily: 'inherit' },
    title: {
      textStyle: { fontSize: FONT_TITLE, fontWeight: 700, color: '#1e293b' },
      left: 'left',
      top: 4,
      padding: [0, 0, 12, 0],
    },
    legend: {
      textStyle: { fontSize: FONT_LEGEND },
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      top: 4,
    },
    tooltip: baseTooltip(),
    grid: { left: 56, right: 24, top: 52, bottom: rotate ? 88 : 72, containLabel: true },
  }

  if (profile === 'donut') {
    return merge(base, merge({
      tooltip: { trigger: 'item', textStyle: { fontSize: FONT_TOOLTIP } },
      legend: { type: 'scroll', orient: 'vertical', left: 'left', textStyle: { fontSize: FONT_LEGEND } },
    }, option))
  }

  if (profile === 'categoryBar') {
    const merged = merge(base, merge({
      grid: { left: 72, right: 24, top: 48, bottom: rotate ? 88 : 72, containLabel: true },
      xAxis: categoryXAxis(cats, true),
      yAxis: valueYAxis(),
      series: enhanceSeries(option.series as SeriesOption[], count),
      dataZoom: optionalDataZoom(count),
    }, option))
    if (rawCats.length) merged.xAxis = categoryXAxis(rawCats, true)
    return merged
  }

  if (profile === 'compareBars') {
    const merged = applyProfile('timeSeries', merge({ xAxis: { boundaryGap: true } }, option))
    const series = enhanceSeries(merged.series as SeriesOption[], count)
    return { ...merged, series }
  }

  // timeSeries default
  const merged = merge(base, merge({
    xAxis: categoryXAxis(cats, false),
    yAxis: valueYAxis(),
    series: enhanceSeries(option.series as SeriesOption[], count),
    dataZoom: optionalDataZoom(count),
  }, option))
  if (rawCats.length) merged.xAxis = categoryXAxis(rawCats, profile === 'compareBars')
  return merged
}
