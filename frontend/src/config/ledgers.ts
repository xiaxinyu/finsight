export type LedgerConfig = {
  title: string
  listEndpoint: string
  txnType?: string
}

export const ledgerConfigs: Record<string, LedgerConfig> = {
  salary: { title: 'Income Ledger', listEndpoint: '/salary/getSalarys', txnType: 'income' },
  expense: { title: 'Expense Ledger', listEndpoint: '/expense/getExpenses', txnType: 'expense' },
  'house-rent': { title: 'Rent Ledger', listEndpoint: '/house-rent/getHouseRents', txnType: 'expense' },
  endowment: { title: 'Pension Ledger', listEndpoint: '/endowment/getEndowments' },
  accumulation: { title: 'Provident Fund', listEndpoint: '/accumulation/getAccumulations' },
  medical: { title: 'Medical Insurance', listEndpoint: '/medical/getMedicals' },
  unemployment: { title: 'Unemployment Insurance', listEndpoint: '/unemployment/getUnEmployments' },
}

export const ledgerIds = Object.keys(ledgerConfigs)
