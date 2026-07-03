import { defaultPeriodStrings } from '../../utils/periodPresets'

export type TxFilters = {
  start: string
  end: string
  card: string
  consume: string
  keyword: string
  unclassified: boolean
  semanticFilter: string
}

export function defaultTxFilters(): TxFilters {
  return {
    ...defaultPeriodStrings(),
    card: '',
    consume: '',
    keyword: '',
    unclassified: false,
    semanticFilter: '',
  }
}

export function txFiltersDiffer(a: TxFilters, b: TxFilters): boolean {
  return a.start !== b.start
    || a.end !== b.end
    || a.card !== b.card
    || a.consume !== b.consume
    || a.keyword.trim() !== b.keyword.trim()
    || a.unclassified !== b.unclassified
    || a.semanticFilter !== b.semanticFilter
}

export function findTreeTitle(
  nodes: { title: string; value: string; children?: typeof nodes }[],
  value: string,
): string {
  for (const n of nodes) {
    if (n.value === value) return n.title
    if (n.children) {
      const t = findTreeTitle(n.children, value)
      if (t) return t
    }
  }
  return ''
}

export function findCardTitle(
  nodes: { id: string; text: string; children?: typeof nodes }[],
  id: string,
): string {
  for (const n of nodes) {
    if (n.id === id) return n.text
    if (n.children) {
      const t = findCardTitle(n.children, id)
      if (t) return t
    }
  }
  return ''
}

export type ClassifyPending =
  | { mode: 'ids'; ids: string }
  | { mode: 'unclassified'; filters: import('../../api/transaction').TransactionQuery }

export const TX_FILTER_PRESETS: { id: string; label: string; patch: Partial<TxFilters> }[] = [
  { id: 'mtd', label: 'This month', patch: defaultPeriodStrings() },
  {
    id: 'unclassified',
    label: 'Unclassified',
    patch: { ...defaultPeriodStrings(), unclassified: true, semanticFilter: '' },
  },
  {
    id: 'all',
    label: 'All time',
    patch: { start: '', end: '', card: '', consume: '', keyword: '', unclassified: false, semanticFilter: '' },
  },
]
