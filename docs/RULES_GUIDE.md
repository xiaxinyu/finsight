# Consumption Rules & Regex Guide

A practical guide to writing stable, maintainable **transaction classification rules** and **keyword/regex matches** to improve automation accuracy.

## Basics
- **Fields**: `description (demoArea)`, `merchant (consumeName)`, `amount`, `bankCard`, `transactionDate`.
- **Rule Types**:
  - Keyword match: for stable text fragments (e.g., “Starbucks”).
  - Regex match: for variable formats (order IDs, store IDs, case variants).
- **Priority**: Lower number = higher priority; more specific rules should rank higher.
- **Tags**: Add dimensions like `#BusinessTrip`, `#Reimbursable`, used alongside categories.

## Principles
- Specificity first: Prefer “merchant + keyword” over amount-only or vague terms.
- Maintainability: Unify regex for similar merchants; avoid per-branch rules.
- Minimal matching: Use boundaries (`\\b`, `^`, `$`) and non-greedy patterns to reduce false positives.
- Combine conditions: Use AND/OR across multiple fields when needed.

## Examples
- Keyword:
  - Description contains “STARBUCKS” or “星巴克” → Category: `Food/Coffee`
  - Description contains “饿了么/Eleme” → Category: `Food/Delivery`, Tag: `#Delivery`
- Regex:
  - Description: `(?i)^(alipay|支付宝).*订阅` → Category: `Subscriptions`
  - Merchant: `(?i)uber\\s+(rides?|trip)` → Category: `Transport/Rides`
- Combined:
  - Merchant “Apple” AND description `(Music|iCloud)` → Category: `Subscriptions`; Tag: `#DigitalServices`

## Conflict Resolution
- Match specific rules first (merchant-level), then general ones (e.g., “Delivery”).
- Use priorities: specific at 10; general at 50.
- Negative filters: use negative lookaheads to avoid known false signals.

## Testing & Validation
- Batch preview: test rules against sample transactions to check hit rates.
- Safe rollout: validate on sandbox dataset before applying globally.
- Audit: record rule versions and matched transaction counts for traceability.

## Tagging Suggestions
- Goal-oriented: `#Reimbursable`, `#TaxDeductible`, `#BudgetWatch`.
- Lifecycle: `#OneTime`, `#Recurring`, `#Installment`.
- Contextual: `#BusinessTrip`, `#Family`, `#Learning`.

## Pitfalls
- Amount-only matching: risky unless the amount is highly characteristic.
- Overusing regex: high maintenance; try keywords or merchant normalization first.
- Case sensitivity: add `(?i)` or normalize text to lowercase.

## Best Practices Checklist
- Normalize merchants before writing rules.
- Create unit samples for core merchants.
- Clear naming: `merchant.apple.music_subscription.v1`.
- Regularly prune redundant or outdated rules.

