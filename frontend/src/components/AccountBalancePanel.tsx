import { formatMoney } from '../utils/format'

type Account = { key: string; value: number }

export function AccountBalancePanel({ accounts }: { accounts: Account[] }) {
  const rows = accounts
    .map((a) => ({ name: a.key, balance: Number(a.value) }))
    .filter((a) => a.name)
    .sort((a, b) => b.balance - a.balance)

  if (rows.length === 0) return null

  const total = rows.reduce((s, r) => s + r.balance, 0)

  return (
    <div className="fs-dash-accounts">
      <div className="fs-dash-accounts-head">
        <span className="fs-dash-accounts-title">Account balances</span>
        <span className="fs-dash-accounts-total">{formatMoney(total)}</span>
      </div>
      <div className="fs-dash-accounts-list">
        {rows.map((row) => (
          <div key={row.name} className="fs-dash-account-row">
            <span className="fs-dash-account-name">{row.name}</span>
            <span className={`fs-dash-account-balance${row.balance < 0 ? ' fs-dash-account-balance--neg' : ''}`}>
              {formatMoney(row.balance)}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
