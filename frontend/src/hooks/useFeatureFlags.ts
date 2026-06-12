import { useQuery } from '@tanstack/react-query'
import { defaultFeatureFlags, fetchFeatureFlags } from '../api/features'

export function useFeatureFlags() {
  const query = useQuery({
    queryKey: ['feature-flags'],
    queryFn: fetchFeatureFlags,
    staleTime: 5 * 60 * 1000,
  })

  return {
    ...query,
    flags: query.data ?? defaultFeatureFlags,
  }
}
