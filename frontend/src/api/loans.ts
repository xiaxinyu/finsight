import { deleteReq, getJson, postJson, putJson } from './client'
import { normalizeResult } from './normalize'

async function unwrap<T>(raw: unknown): Promise<T> {
  const n = normalizeResult(raw)
  if (!n.ok) throw new Error(n.message || 'Request failed')
  return n.data as T
}

export type RepaymentMethod =
  | 'EQUAL_INSTALLMENT'
  | 'EQUAL_PRINCIPAL'
  | 'INTEREST_FIRST'
  | 'BULLET'
  | 'OTHER'

export type LoanStatus = 'ACTIVE' | 'CLOSED'

export type LoanRow = {
  id?: string
  name?: string
  lenderName?: string
  lenderBankCode?: string
  principalAmount?: number
  outstandingBalance?: number
  interestRatePct?: number
  monthlyPayment?: number
  repaymentMethod?: RepaymentMethod
  maturityDate?: string
  disbursementCardId?: string
  repaymentCardId?: string
  disbursementCardLabel?: string
  repaymentCardLabel?: string
  status?: LoanStatus
  notes?: string
  sortOrder?: number
}

export type LoanSummary = {
  loanCount?: number
  totalPrincipal?: number
  totalOutstanding?: number
  totalMonthlyPayment?: number
  weightedAvgRatePct?: number
}

export type LoansListResponse = {
  loans: LoanRow[]
  summary: LoanSummary
}

export type LoanWritePayload = {
  name?: string | null
  lenderName: string
  lenderBankCode?: string | null
  principalAmount: number
  outstandingBalance?: number | null
  interestRatePct?: number | null
  monthlyPayment?: number | null
  repaymentMethod?: RepaymentMethod | null
  maturityDate?: string | null
  disbursementCardId: string
  repaymentCardId?: string | null
  status?: LoanStatus
  notes?: string | null
  sortOrder?: number
}

export const REPAYMENT_METHOD_LABELS: Record<RepaymentMethod, string> = {
  EQUAL_INSTALLMENT: '等额本息',
  EQUAL_PRINCIPAL: '等额本金',
  INTEREST_FIRST: '先息后本',
  BULLET: '到期还本',
  OTHER: '其他',
}

export async function fetchLoans() {
  return unwrap<LoansListResponse>(await getJson('/api/v1/loans'))
}

export async function createLoan(payload: LoanWritePayload) {
  return unwrap<LoanRow>(await postJson('/api/v1/loans', payload))
}

export async function updateLoan(id: string, payload: LoanWritePayload) {
  return unwrap<LoanRow>(await putJson(`/api/v1/loans/${id}`, payload))
}

export async function deleteLoan(id: string) {
  return unwrap<unknown>(await deleteReq(`/api/v1/loans/${id}`))
}

export type LoanLinkType = 'DISBURSEMENT' | 'REPAYMENT' | 'INTEREST' | 'OTHER'

export type LoanTxnLinkRow = {
  id?: string
  loanId?: string
  transactionId?: string
  linkType?: LoanLinkType
  transactionDate?: string
  transactionDesc?: string
  incomeMoney?: number
  expenseAmount?: number
  bankCardName?: string
  createdAt?: string
}

export const LOAN_LINK_TYPE_LABELS: Record<LoanLinkType, string> = {
  DISBURSEMENT: '放款',
  REPAYMENT: '还款',
  INTEREST: '付息',
  OTHER: '其他',
}

export async function fetchLoanLinks(loanId: string) {
  return unwrap<LoanTxnLinkRow[]>(await getJson(`/api/v1/loans/${loanId}/links`))
}

export async function addLoanLink(loanId: string, transactionId: string, linkType: LoanLinkType) {
  return unwrap<LoanTxnLinkRow>(await postJson(`/api/v1/loans/${loanId}/links`, { transactionId, linkType }))
}

export async function removeLoanLink(loanId: string, transactionId: string) {
  return unwrap<unknown>(await deleteReq(`/api/v1/loans/${loanId}/links/${transactionId}`))
}
