package com.finsight.application.transaction;

import com.finsight.domain.model.Transaction;

/**
 * Canonical amount model for ledger rows:
 * <ul>
 *   <li>Inflow: positive {@code incomeMoney}, {@code balanceMoney} = 0</li>
 *   <li>Outflow: positive {@code balanceMoney}, {@code incomeMoney} = 0</li>
 * </ul>
 * Legacy signed {@code balanceMoney} (negative = income) is converted on import.
 */
public final class TransactionAmountNormalizer {

    private TransactionAmountNormalizer() {
    }

    public static void normalize(Transaction t) {
        if (t == null) {
            return;
        }
        double income = t.getIncomeMoney() == null ? 0.0 : t.getIncomeMoney();
        double balance = t.getBalanceMoney() == null ? 0.0 : t.getBalanceMoney();

        if (income > 0 && balance <= 0) {
            t.setIncomeMoney(Math.abs(income));
            t.setBalanceMoney(0.0);
            if (t.getTxnKind() == null || t.getTxnKind().isBlank()) {
                t.setTxnKind("income");
            }
            return;
        }
        if (balance < 0) {
            t.setIncomeMoney(Math.abs(balance));
            t.setBalanceMoney(0.0);
            if (t.getTxnKind() == null || t.getTxnKind().isBlank()) {
                t.setTxnKind("income");
            }
            return;
        }
        if (income > 0 && balance > 0) {
            t.setIncomeMoney(Math.abs(income));
            t.setBalanceMoney(0.0);
            if (t.getTxnKind() == null || t.getTxnKind().isBlank()) {
                t.setTxnKind("income");
            }
            return;
        }
        if (balance > 0) {
            t.setBalanceMoney(Math.abs(balance));
            t.setIncomeMoney(0.0);
            if (t.getTxnKind() == null || t.getTxnKind().isBlank()) {
                t.setTxnKind("expense");
            }
            return;
        }
        if (income < 0) {
            t.setBalanceMoney(Math.abs(income));
            t.setIncomeMoney(0.0);
            if (t.getTxnKind() == null || t.getTxnKind().isBlank()) {
                t.setTxnKind("expense");
            }
        }
    }

    /** Display / edit magnitude — income column first, else absolute balance. */
    public static double canonicalMagnitude(Transaction t) {
        if (t == null) {
            return 0;
        }
        double income = t.getIncomeMoney() == null ? 0.0 : t.getIncomeMoney();
        double balance = t.getBalanceMoney() == null ? 0.0 : t.getBalanceMoney();
        if (income > 0) {
            return Math.abs(income);
        }
        if (balance != 0) {
            return Math.abs(balance);
        }
        return 0;
    }

    /** Reassign income/expense type without changing magnitude. */
    public static void applyTxnKind(Transaction t, String kind) {
        if (t == null || kind == null || kind.isBlank()) {
            return;
        }
        double amt = canonicalMagnitude(t);
        if (amt <= 0) {
            return;
        }
        if ("income".equalsIgnoreCase(kind)) {
            t.setIncomeMoney(amt);
            t.setBalanceMoney(0.0);
            t.setTxnKind("income");
            return;
        }
        if ("expense".equalsIgnoreCase(kind)) {
            t.setBalanceMoney(amt);
            t.setIncomeMoney(0.0);
            t.setTxnKind("expense");
        }
    }
}
