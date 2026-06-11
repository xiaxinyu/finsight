import { useQuery } from '@tanstack/react-query'
import { cardTree } from '../api/transaction'

export function useCardTree() {
  const { data, isLoading } = useQuery({
    queryKey: ['card-tree'],
    queryFn: cardTree,
    staleTime: 60_000,
  })
  return { tree: data || [], isLoading }
}
