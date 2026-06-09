package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Created by Summer.Xia on 08/27/2014.
 */
@TableName("transaction")
@Getter
@Setter
@ToString
public class Transaction extends BaseEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String cardId;
    @TableField(value = "bank_card_id")
    private String bankCardId;
    @TableField(value = "bank_card_name")
    private String bankCardName;
    private Date transactionDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @TableField(value = "bookkeeping_date")
    private Date bookKeepingDate;

    private String transactionDesc;
    private String balanceCurrency;
    private Double balanceMoney;
    private Integer cardTypeId;
    private String cardTypeName;
    private Integer deleted;
    private Integer consumptionType;
    @TableField(value = "consume_id")
    private String consumeID;
    @TableField(value = "consume_code")
    private String consumeCode;
    @TableField(value = "consume_name")
    private String consumeName;

    @TableField(value = "demoArea")
    private String demoArea;

    @TableField(value = "recordID")
    private String recordID;

    @TableField(value = "income_money")
    private Double incomeMoney;

    @TableField(value = "opponent_account")
    private String opponentAccount;

    @TableField(value = "opponent_name")
    private String opponentName;

    @TableField(value = "transaction_time")
    private String transactionTime;

    @TableField(value = "account_balance")
    private Double accountBalance;

    @TableField(value = "txn_kind")
    private String txnKind;

    @TableField(value = "transfer_group_id")
    private String transferGroupId;

    @TableField(exist = false)
    private String transactionDateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getBankCardId() { return bankCardId; }
    public void setBankCardId(String bankCardId) { this.bankCardId = bankCardId; }
    public String getBankCardName() { return bankCardName; }
    public void setBankCardName(String bankCardName) { this.bankCardName = bankCardName; }
    public Date getTransactionDate() { return transactionDate; }
    public void setTransactionDate(Date transactionDate) { this.transactionDate = transactionDate; }
    public Date getBookKeepingDate() { return bookKeepingDate; }
    public void setBookKeepingDate(Date bookKeepingDate) { this.bookKeepingDate = bookKeepingDate; }
    public String getTransactionDesc() { return transactionDesc; }
    public void setTransactionDesc(String transactionDesc) { this.transactionDesc = transactionDesc; }
    public String getBalanceCurrency() { return balanceCurrency; }
    public void setBalanceCurrency(String balanceCurrency) { this.balanceCurrency = balanceCurrency; }
    public Double getBalanceMoney() { return balanceMoney; }
    public void setBalanceMoney(Double balanceMoney) { this.balanceMoney = balanceMoney; }
    public Integer getCardTypeId() { return cardTypeId; }
    public void setCardTypeId(Integer cardTypeId) { this.cardTypeId = cardTypeId; }
    public String getCardTypeName() { return cardTypeName; }
    public void setCardTypeName(String cardTypeName) { this.cardTypeName = cardTypeName; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Integer getConsumptionType() { return consumptionType; }
    public void setConsumptionType(Integer consumptionType) { this.consumptionType = consumptionType; }
    public String getConsumeID() { return consumeID; }
    public void setConsumeID(String consumeID) { this.consumeID = consumeID; }
    public String getConsumeCode() { return consumeCode; }
    public void setConsumeCode(String consumeCode) { this.consumeCode = consumeCode; }
    public String getConsumeName() { return consumeName; }
    public void setConsumeName(String consumeName) { this.consumeName = consumeName; }
    public String getDemoArea() { return demoArea; }
    public void setDemoArea(String demoArea) { this.demoArea = demoArea; }
    public String getRecordID() { return recordID; }
    public void setRecordID(String recordID) { this.recordID = recordID; }
}
