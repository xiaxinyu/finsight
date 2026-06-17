import type { TransactionRow } from '../api/transaction'
import { aggregateTransactionRows } from '../api/transaction'
import { formatTableDate } from './cell'

export type SelectionSummary = {
  count: number
  income: number
  expense: number
  net: number
  dateFrom?: string
  dateTo?: string
}

export function summarizeSelection(rows: TransactionRow[]): SelectionSummary {
  const agg = aggregateTransactionRows(rows)
  const dates = rows
    .map((r) => r.transactionDate)
    .filter(Boolean)
    .map((d) => String(d))
    .sort()
  return {
    count: rows.length,
    income: agg.income,
    expense: agg.expense,
    net: agg.net,
    dateFrom: dates[0] ? formatTableDate(dates[0]) : undefined,
    dateTo: dates.length ? formatTableDate(dates[dates.length - 1]) : undefined,
  }
}
