import { useQuery } from '@tanstack/react-query'
import { fetchAuthSession } from '../api/auth'

export function useAuth() {
  const query = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: fetchAuthSession,
    staleTime: 60_000,
    retry: false,
  })

  return {
    session: query.data,
    isAdmin: query.data?.admin === true,
    isAuthenticated: query.data?.authenticated === true,
    isLoading: query.isLoading,
    refetch: query.refetch,
  }
}
