import type { EChartsOption } from 'echarts'
import type { ProfileDimension } from '../../api/analytics'
import { profileScoreColor } from './profileDisplay'

export const PROFILE_DIM_LABELS: Record<string, string> = {
  income_stability: 'Income stability',
  spending_control: 'Spending control',
  savings_discipline: 'Savings discipline',
  fixed_burden: 'Fixed burden',
  liquidity_safety: 'Liquidity safety',
  debt_pressure: 'Debt pressure',
  lifestyle_inflation: 'Lifestyle inflation',
  spending_concentration: 'Spending concentration',
  seasonality_risk: 'Seasonality risk',
  data_trust: 'Data trust',
}

export const PROFILE_USER_TYPE_LABELS: Record<string, string> = {
  disciplined_saver: 'Disciplined saver',
  high_fixed_burden: 'High fixed burden',
  cashflow_stressed: 'Cashflow stressed',
  volatile_income: 'Volatile income',
  lifestyle_inflation: 'Lifestyle inflation',
  debt_pressure: 'Debt pressure',
  data_quality_risk: 'Data quality risk',
  balanced: 'Balanced',
}

export function profileUserTypeLabel(userType: string): string {
  return PROFILE_USER_TYPE_LABELS[userType] ?? userType.replace(/_/g, ' ')
}

const EMPTY_RADAR_OPTION: EChartsOption = {
  tooltip: {},
  radar: { indicator: [], radius: '62%' },
  series: [{ type: 'radar', data: [] }],
}

export function buildProfileRadarOption(dimensions: ProfileDimension[] | undefined): EChartsOption {
  if (!dimensions?.length) {
    return EMPTY_RADAR_OPTION
  }

  const pointColors = dimensions.map((d) => profileScoreColor(d.score, d.id))

  return {
    color: pointColors,
    tooltip: { trigger: 'item' },
    radar: {
      indicator: dimensions.map((d) => ({ name: PROFILE_DIM_LABELS[d.id] || d.id, max: 100 })),
      radius: '68%',
      splitNumber: 4,
      axisName: { color: '#64748b', fontSize: 11 },
      splitArea: { areaStyle: { color: ['#f8fafc', '#fff'] } },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
      axisLine: { lineStyle: { color: '#cbd5e1' } },
    },
    series: [{
      type: 'radar' as const,
      data: [{
        value: dimensions.map((d) => d.score),
        name: 'Profile',
        lineStyle: { width: 2, color: '#64748b' },
        areaStyle: { opacity: 0.12, color: '#94a3b8' },
        itemStyle: {
          color: (params: { dimensionIndex?: number }) => {
            const idx = params.dimensionIndex ?? 0
            return pointColors[idx] ?? '#2563eb'
          },
        },
        symbolSize: 6,
      }],
    }],
  }
}
