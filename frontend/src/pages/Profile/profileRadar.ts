import type { EChartsOption } from 'echarts'
import type { ProfileDimension } from '../../api/analytics'

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

const EMPTY_RADAR_OPTION: EChartsOption = {
  tooltip: {},
  radar: { indicator: [], radius: '62%' },
  series: [{ type: 'radar', data: [] }],
}

export function buildProfileRadarOption(dimensions: ProfileDimension[] | undefined): EChartsOption {
  if (!dimensions?.length) {
    return EMPTY_RADAR_OPTION
  }

  return {
    tooltip: {},
    radar: {
      indicator: dimensions.map((d) => ({ name: PROFILE_DIM_LABELS[d.id] || d.id, max: 100 })),
      radius: '62%',
    },
    series: [{
      type: 'radar' as const,
      data: [{ value: dimensions.map((d) => d.score), name: 'Profile' }],
      areaStyle: { opacity: 0.15 },
    }],
  }
}
