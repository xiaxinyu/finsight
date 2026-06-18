package com.finsight.application.query;

import java.util.Date;

public class TransactionQuery {
    private String txnTypes;
    private Date transactionDateStart;
    private Date transactionDateEnd;
    private Integer consumptionType;
    private String bankCardId;
    private String cardTypeName;
    private String consumeID;
    private String consumeCode;
    private String[] consumes;
    private Boolean emptyConsume;
    private String consumeName;
    private String demoArea;
    private String weekName;
    private String year;
    private String month;
    private String sortField;
    private String sortOrder;
    private String merchantToken;

    public String getTxnTypes() { return txnTypes; }
    public void setTxnTypes(String txnTypes) { this.txnTypes = txnTypes; }
    public Date getTransactionDateStart() { return transactionDateStart; }
    public void setTransactionDateStart(Date transactionDateStart) { this.transactionDateStart = transactionDateStart; }
    public Date getTransactionDateEnd() { return transactionDateEnd; }
    public void setTransactionDateEnd(Date transactionDateEnd) { this.transactionDateEnd = transactionDateEnd; }
    public Integer getConsumptionType() { return consumptionType; }
    public void setConsumptionType(Integer consumptionType) { this.consumptionType = consumptionType; }
    public String getBankCardId() { return bankCardId; }
    public void setBankCardId(String bankCardId) { this.bankCardId = bankCardId; }
    public String getCardTypeName() { return cardTypeName; }
    public void setCardTypeName(String cardTypeName) { this.cardTypeName = cardTypeName; }
    public String getConsumeID() { return consumeID; }
    public void setConsumeID(String consumeID) { this.consumeID = consumeID; }
    public String getConsumeCode() { return consumeCode; }
    public void setConsumeCode(String consumeCode) { this.consumeCode = consumeCode; }
    public String[] getConsumes() { return consumes; }
    public void setConsumes(String[] consumes) { this.consumes = consumes; }
    public Boolean getEmptyConsume() { return emptyConsume; }
    public void setEmptyConsume(Boolean emptyConsume) { this.emptyConsume = emptyConsume; }
    public String getConsumeName() { return consumeName; }
    public void setConsumeName(String consumeName) { this.consumeName = consumeName; }
    public String getDemoArea() { return demoArea; }
    public void setDemoArea(String demoArea) { this.demoArea = demoArea; }
    public String getWeekName() { return weekName; }
    public void setWeekName(String weekName) { this.weekName = weekName; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    public String getMerchantToken() { return merchantToken; }
    public void setMerchantToken(String merchantToken) { this.merchantToken = merchantToken; }
}

