package com.finsight.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Loan extends AuditableEntity {
    private String id;
    private String userId;
    private String name;
    private String lenderName;
    private String lenderBankCode;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal interestRatePct;
    private BigDecimal monthlyPayment;
    /** Total loan term in months (optional). */
    private Integer termMonths;
    /** Installments already paid before / outside linked transactions. */
    private Integer paidInstallments;
    /** EQUAL_INSTALLMENT | EQUAL_PRINCIPAL | INTEREST_FIRST | BULLET | OTHER */
    private String repaymentMethod;
    private Date maturityDate;
    /** fin_bank_account.id — where loan proceeds are credited */
    private String disbursementCardId;
    /** fin_bank_account.id — optional debit account for repayments */
    private String repaymentCardId;
    /** ACTIVE | CLOSED */
    private String status;
    private String notes;
    private Integer sortOrder;
    private Integer deleted;

    /** Enriched for API responses (not persisted on fin_loan). */
    private String disbursementCardLabel;
    private String repaymentCardLabel;
    private Integer linkCount;
    /** Enriched: REPAYMENT links with txn amount above minimum installment threshold. */
    private Integer linkedRepaymentCount;
    /** Enriched: sum of qualifying linked REPAYMENT transaction amounts. */
    private BigDecimal linkedRepaymentAmount;
}
