package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
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
    @Setter(AccessLevel.NONE)
    private Double balanceMoney;
    private Integer cardTypeId;
    private String cardTypeName;
    private Integer deleted;
    private Integer consumptionType;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @TableField(value = "consume_id")
    private String consumeID;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @TableField(value = "consume_code")
    private String consumeCode;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @TableField(value = "consume_name")
    private String consumeName;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @TableField(value = "category_id")
    private String categoryId;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @TableField(value = "category_code")
    private String categoryCode;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @TableField(value = "category_name")
    private String categoryName;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @TableField(value = "memo")
    private String demoArea;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @TableField(value = "statement_id")
    private String recordID;

    @TableField(value = "expense_amount")
    private Double expenseAmount;

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

    /** Resolved from fin_bank_account or import statement source. */
    @TableField(exist = false)
    private String bankCode;

    @TableField(exist = false)
    private String cardTypeCode;

    /** From {@code v_transaction_finance_semantics} — not persisted. */
    @TableField(exist = false)
    private String economicNature;

    @TableField(exist = false)
    private String budgetBehavior;

    @TableField(exist = false)
    private String financeReportRole;

    @TableField(exist = false)
    private String qualityState;

    @TableField(exist = false)
    private String categoryParentId;

    @TableField(exist = false)
    private String categorySemanticTag;

    @TableField(exist = false)
    private String categoryL1Name;

    @TableField(exist = false)
    private Boolean includeInIncomeTrend;

    @TableField(exist = false)
    private Boolean includeInExpenseTrend;

    @TableField(exist = false)
    private Boolean includeInBudget;

    @TableField(exist = false)
    private String semanticsSummary;

    @TableField(exist = false)
    private java.util.List<com.finsight.web.api.dto.TransactionDisplayTag> displayTags;

    public Double getBalanceMoney() { return balanceMoney; }
    public String getConsumeID() { return consumeID != null ? consumeID : categoryId; }
    public void setConsumeID(String consumeID) {
        this.consumeID = consumeID;
        this.categoryId = consumeID;
    }
    public String getConsumeCode() { return consumeCode != null ? consumeCode : categoryCode; }
    public void setConsumeCode(String consumeCode) {
        this.consumeCode = consumeCode;
        this.categoryCode = consumeCode;
    }
    public String getConsumeName() { return consumeName != null ? consumeName : categoryName; }
    public void setConsumeName(String consumeName) {
        this.consumeName = consumeName;
        this.categoryName = consumeName;
    }
    public String getCategoryId() { return categoryId != null ? categoryId : consumeID; }
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        this.consumeID = categoryId;
    }
    public String getCategoryCode() { return categoryCode != null ? categoryCode : consumeCode; }
    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
        this.consumeCode = categoryCode;
    }
    public String getCategoryName() { return categoryName != null ? categoryName : consumeName; }
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
        this.consumeName = categoryName;
    }
    public String getDemoArea() { return demoArea; }
    public void setDemoArea(String demoArea) { this.demoArea = demoArea; }
    public String getMemo() { return demoArea; }
    public void setMemo(String memo) { this.demoArea = memo; }
    public String getRecordID() { return recordID; }
    public void setRecordID(String recordID) { this.recordID = recordID; }
    public String getStatementId() { return recordID; }
    public void setStatementId(String statementId) { this.recordID = statementId; }

    /** True when a category column was explicitly set on this instance (partial update detection). */
    public boolean hasCategoryFieldPatch() {
        return consumeCode != null || categoryCode != null || consumeID != null || categoryId != null
                || consumeName != null || categoryName != null;
    }

    public Double getExpenseAmount() { return expenseAmount; }
    public void setExpenseAmount(Double expenseAmount) { this.expenseAmount = expenseAmount; }
    public void setBalanceMoney(Double balanceMoney) {
        this.balanceMoney = balanceMoney;
        if (balanceMoney == null) {
            return;
        }
        if (balanceMoney < 0) {
            this.expenseAmount = Math.abs(balanceMoney);
        } else if (balanceMoney > 0) {
            this.expenseAmount = balanceMoney;
        }
    }
}
