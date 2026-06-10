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
}
