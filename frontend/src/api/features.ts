import { getJson } from './client'
import { normalizeResult } from './normalize'

export type FeatureFlags = {
  planningPersist: boolean
  advisor: boolean
  localAi: boolean
  profile: boolean
  forecast: boolean
  merchantMining: boolean
  metricsReconcileGate: boolean
}

export const defaultFeatureFlags: FeatureFlags = {
  planningPersist: false,
  advisor: true,
  localAi: true,
  profile: true,
  forecast: true,
  merchantMining: true,
  metricsReconcileGate: false,
}

export async function fetchFeatureFlags(): Promise<FeatureFlags> {
  const raw = await getJson('/api/v1/features')
  const n = normalizeResult(raw)
  if (!n.ok || !n.data) {
    return defaultFeatureFlags
  }
  return { ...defaultFeatureFlags, ...(n.data as Partial<FeatureFlags>) }
}
