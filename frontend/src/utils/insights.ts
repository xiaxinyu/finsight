import { formatMoney } from './format'
import type { InsightBullet } from '../components/InsightPanel'

function pct(n: number) {
  return `${(Number(n) || 0).toFixed(1)}%`
}

export function fromCategorySpend(rows: { key: string; value: number }[], total: number, year?: string): InsightBullet[] {
  if (!rows.length || total <= 0) {
    return [{ text: 'No expense data for the selected filters. Try a broader date range (e.g. 2025) or clear category filters.', warn: true }]
  }
  const sorted = [...rows].sort((a, b) => b.value - a.value)
  const top = sorted[0]
  const topShare = total > 0 ? (top.value / total) * 100 : 0
  const bullets: InsightBullet[] = [
    { text: `Top category "${top.key}" accounts for ${pct(topShare)} of spend (${formatMoney(top.value)}).` },
  ]
  if (topShare >= 40) {
    bullets.push({ text: `Spending is highly concentrated — consider diversifying or setting a cap on "${top.key}".`, warn: true })
  }
  if (year && Number(year) >= new Date().getFullYear()) {
    bullets.push({ text: 'Current-year data may still be sparse; compare with the prior full year for clearer trends.', warn: true })
  }
  return bullets
}

export function fromIncomeExpense(rows: { income: number; expense: number; surplus: number }[], year?: string): InsightBullet[] {
  if (!rows.length) return [{ text: 'No monthly income/expense data. Adjust year or filters.', warn: true }]
  let income = 0, expense = 0, deficitMonths = 0
  rows.forEach((r) => {
    income += r.income
    expense += r.expense
    if (r.surplus < 0) deficitMonths++
  })
  const surplus = income - expense
  const savings = income > 0 ? (surplus / income) * 100 : 0
  const bullets: InsightBullet[] = [
    { text: `${year || 'Period'} totals: Income ${formatMoney(income)}, Expense ${formatMoney(expense)}, Net ${formatMoney(surplus)} (${pct(savings)} savings rate).` },
  ]
  if (deficitMonths > 0) bullets.push({ text: `${deficitMonths} month(s) ended in deficit — review fixed costs and discretionary spend.`, warn: true })
  else if (savings >= 20) bullets.push({ text: 'Healthy savings rate — net cash flow is positive across the period.' })
  else if (savings < 0) bullets.push({ text: 'Period ended in deficit — spending exceeded income.', warn: true })
  return bullets
}

export function fromYearCompare(totalA: number, totalB: number, yearA: string, yearB: string): InsightBullet[] {
  if (totalA <= 0 && totalB <= 0) return [{ text: 'Both years have no expense data for current filters.', warn: true }]
  const delta = totalB - totalA
  const dp = totalA > 0 ? (delta / totalA) * 100 : 0
  const bullets: InsightBullet[] = [
    { text: `Expense changed from ${formatMoney(totalA)} (${yearA}) to ${formatMoney(totalB)} (${yearB}): ${delta >= 0 ? '+' : ''}${formatMoney(delta)} (${pct(dp)}).` },
  ]
  if (Math.abs(dp) >= 15) bullets.push({ text: 'Large year-over-year shift — inspect top category changes in the table.', warn: true })
  return bullets
}
