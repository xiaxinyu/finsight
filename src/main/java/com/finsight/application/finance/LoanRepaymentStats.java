package com.finsight.application.finance;

import java.math.BigDecimal;

/**
 * Rules for deriving loan repayment progress from linked ledger transactions.
 */
public final class LoanRepaymentStats {

    /** Repayment transactions at or below this amount are ignored (fees, rounding). */
    public static final BigDecimal MIN_INSTALLMENT_AMOUNT = new BigDecimal("100");

    private LoanRepaymentStats() {
    }
}
