export type TransactionSemanticTag =
  | 'real_consumption'
  | 'refund'
  | 'reimbursement'
  | 'transfer'
  | 'investment'
  | 'liability'
  | 'fee'
  | 'other'
  | 'unclassified'
  | 'real_income'

export type TransactionSemanticMeta = {
  id: TransactionSemanticTag
  label: string
  color: string
  hint: string
}

const TAG_META: Record<TransactionSemanticTag, TransactionSemanticMeta> = {
  real_income: {
    id: 'real_income',
    label: 'Real income',
    color: 'green',
    hint: 'Counts toward income trend (e.g. salary)',
  },
  real_consumption: {
    id: 'real_consumption',
    label: 'Consumption',
    color: 'orange',
    hint: 'Counts toward expense and budget trends',
  },
  refund: {
    id: 'refund',
    label: 'Refund',
    color: 'blue',
    hint: 'Refund inflow — not counted as income growth',
  },
  reimbursement: {
    id: 'reimbursement',
    label: 'Reimbursement',
    color: 'cyan',
    hint: 'Expense reimbursement — not real income',
  },
  transfer: {
    id: 'transfer',
    label: 'Transfer',
    color: 'geekblue',
    hint: 'Internal transfer — excluded from income/expense trends',
  },
  investment: {
    id: 'investment',
    label: 'Investment',
    color: 'purple',
    hint: 'Investment cash flow — not consumption',
  },
  liability: {
    id: 'liability',
    label: 'Debt / liability',
    color: 'volcano',
    hint: 'Borrowing or repayment — not consumption or income',
  },
  fee: {
    id: 'fee',
    label: 'Fee',
    color: 'magenta',
    hint: 'Bank or finance fees',
  },
  other: {
    id: 'other',
    label: 'Other',
    color: 'default',
    hint: 'Catch-all category — review for data quality',
  },
  unclassified: {
    id: 'unclassified',
    label: 'Unclassified',
    color: 'gold',
    hint: 'No category — excluded from profile concentration',
  },
}

function categoryRoot(row: { consumeCode?: string; consumeID?: string }): string {
  const code = (row.consumeCode || row.consumeID || '').trim().toUpperCase()
  if (!code) return ''
  const dash = code.indexOf('-')
  return dash > 0 ? code.slice(0, dash) : code
}

/** Derive finance semantic tag from category code and txn kind (aligned with v2.0.2 contract). */
export function detectTransactionSemanticTag(row: {
  consumeCode?: string
  consumeID?: string
  consumeName?: string
  txnKind?: string
  incomeMoney?: number
  balanceMoney?: number
}): TransactionSemanticTag | null {
  const code = (row.consumeCode || row.consumeID || '').trim().toUpperCase()
  const root = categoryRoot(row)
  const kind = row.txnKind || (row.incomeMoney && row.incomeMoney > 0 ? 'income' : 'expense')

  if (!code && !(row.consumeName || '').trim()) return 'unclassified'

  if (root === 'REIM' || code.startsWith('REIM')) return 'reimbursement'
  if (root === 'INVEST' || root === 'WEALTH' || code.includes('INVEST')) return 'investment'
  if (root === 'LIABILITY' || (root === 'FIXED' && code.includes('REPAY'))) return 'liability'
  if (root === 'ASSET') return 'transfer'
  if (root === 'FEE' || code.startsWith('FEE')) return 'fee'
  if (root === 'OTHER' || code === 'OTHER') return 'other'

  if (kind === 'income') {
    if (code.includes('REFUND') || (row.consumeName || '').includes('退款')) return 'refund'
    if (code.includes('INC-08') || (row.consumeName || '').includes('借款')) return 'liability'
    if (code.includes('INC-04') || root === 'WEALTH') return 'investment'
    return 'real_income'
  }

  if ((row.consumeName || '').includes('退款') || code.includes('REFUND')) return 'refund'
  if ((row.consumeName || '').includes('转账') || root === 'ASSET') return 'transfer'

  return 'real_consumption'
}

export function semanticTagMeta(tag: TransactionSemanticTag): TransactionSemanticMeta {
  return TAG_META[tag]
}

export function semanticInclusionHint(tag: TransactionSemanticTag | null): string {
  if (!tag) return ''
  switch (tag) {
    case 'real_income':
      return 'Included in income trend'
    case 'real_consumption':
      return 'Included in expense & budget trends'
    case 'refund':
    case 'reimbursement':
      return 'Excluded from income trend'
    case 'transfer':
      return 'Excluded from income, expense, and budget'
    case 'investment':
    case 'liability':
      return 'Excluded from consumption budget'
    case 'fee':
      return 'Included in cashflow; may affect expense analysis'
    case 'unclassified':
    case 'other':
      return 'Data quality risk — classify to improve reports'
    default:
      return ''
  }
}
