package com.finsight.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class LoanTxnLink extends AuditableEntity {
    private String id;
    private String loanId;
    private String transactionId;
    /** DISBURSEMENT | REPAYMENT | INTEREST | OTHER */
    private String linkType;
    private String userId;
    private Date createdAt;
}
