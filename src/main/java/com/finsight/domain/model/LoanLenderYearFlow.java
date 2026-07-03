package com.finsight.domain.model;

import java.math.BigDecimal;

/** Loan-linked transaction amount aggregated by lender and calendar year. */
public record LoanLenderYearFlow(
        int year,
        String loanId,
        String lenderName,
        String linkType,
        BigDecimal amount
) {
}
