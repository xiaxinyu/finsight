export type LedgerConfig = {
  title: string
  listEndpoint: string
  txnType?: string
  mutations?: { add?: string; update?: string; delete?: string; copy?: string }
}

export const ledgerConfigs: Record<string, LedgerConfig> = {
  salary: { title: 'Income Ledger', listEndpoint: '/salary/getSalarys', txnType: 'income' },
  expense: { title: 'Expense Ledger', listEndpoint: '/expense/getExpenses', txnType: 'expense' },
  'house-rent': { title: 'Rent Ledger', listEndpoint: '/house-rent/getHouseRents', txnType: 'expense' },
  endowment: { title: 'Pension Ledger', listEndpoint: '/endowment/getEndowments', mutations: { add: '/endowment/add', delete: '/endowment/delete', update: '/endowment/update', copy: '/endowment/copy' } },
  accumulation: { title: 'Provident Fund', listEndpoint: '/accumulation/getAccumulations', mutations: { add: '/accumulation/add', delete: '/accumulation/delete', update: '/accumulation/update', copy: '/accumulation/copy' } },
  medical: { title: 'Medical Insurance', listEndpoint: '/medical/getMedicals', mutations: { add: '/medical/add', delete: '/medical/delete', update: '/medical/update', copy: '/medical/copy' } },
  unemployment: { title: 'Unemployment Insurance', listEndpoint: '/unemployment/getUnEmployments', mutations: { add: '/unemployment/add', delete: '/unemployment/delete', update: '/unemployment/update', copy: '/unemployment/copy' } },
}
