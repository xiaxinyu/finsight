package com.finsight.web.api.dto;

import java.math.BigDecimal;

public class LoanWriteRequest {
    private String name;
    private String lenderName;
    private String lenderBankCode;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal interestRatePct;
    private BigDecimal monthlyPayment;
    private String repaymentMethod;
    private String maturityDate;
    private String disbursementCardId;
    private String repaymentCardId;
    private String status;
    private String notes;
    private Integer sortOrder;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLenderName() { return lenderName; }
    public void setLenderName(String lenderName) { this.lenderName = lenderName; }
    public String getLenderBankCode() { return lenderBankCode; }
    public void setLenderBankCode(String lenderBankCode) { this.lenderBankCode = lenderBankCode; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public BigDecimal getInterestRatePct() { return interestRatePct; }
    public void setInterestRatePct(BigDecimal interestRatePct) { this.interestRatePct = interestRatePct; }
    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(BigDecimal monthlyPayment) { this.monthlyPayment = monthlyPayment; }
    public String getRepaymentMethod() { return repaymentMethod; }
    public void setRepaymentMethod(String repaymentMethod) { this.repaymentMethod = repaymentMethod; }
    public String getMaturityDate() { return maturityDate; }
    public void setMaturityDate(String maturityDate) { this.maturityDate = maturityDate; }
    public String getDisbursementCardId() { return disbursementCardId; }
    public void setDisbursementCardId(String disbursementCardId) { this.disbursementCardId = disbursementCardId; }
    public String getRepaymentCardId() { return repaymentCardId; }
    public void setRepaymentCardId(String repaymentCardId) { this.repaymentCardId = repaymentCardId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
