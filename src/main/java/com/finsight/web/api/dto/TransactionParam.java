package com.finsight.web.api.dto;

/**
 * Created by Summer.Xia on 2015/9/17.
 */
public class TransactionParam extends PageParam {
    private String transactionDateStartStr;
    private String transactionDateEndStr;
    private String consumptionType;
    private String cardTypeName;
    private String cardId;
    private String consumeName;
    private String consumeID;
    private String demoArea;
    private String weekName;
    private String year;
    private String month;
    private String txnTypes;
    private String emptyConsume; // '1' means query where consume is empty
    private String sortField;
    private String sortOrder;
    private String merchantToken;

	public String getTransactionDateStartStr() {
		return transactionDateStartStr;
	}

	public void setTransactionDateStartStr(String transactionDateStartStr) {
		this.transactionDateStartStr = transactionDateStartStr;
	}

	public String getTransactionDateEndStr() {
		return transactionDateEndStr;
	}

	public void setTransactionDateEndStr(String transactionDateEndStr) {
		this.transactionDateEndStr = transactionDateEndStr;
	}

	public String getConsumptionType() {
		return consumptionType;
	}

	public void setConsumptionType(String consumptionType) {
		this.consumptionType = consumptionType;
	}

    public String getCardTypeName() {
        return cardTypeName;
    }

    public void setCardTypeName(String cardTypeName) {
        this.cardTypeName = cardTypeName;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

	public String getConsumeName() {
		return consumeName;
	}

	public void setConsumeName(String consumeName) {
		this.consumeName = consumeName;
	}

	public String getConsumeID() {
		return consumeID;
	}

	public void setConsumeID(String consumeID) {
		this.consumeID = consumeID;
	}

	public String getDemoArea() {
		return demoArea;
	}

	public void setDemoArea(String demoArea) {
		this.demoArea = demoArea;
	}

	public String getWeekName() {
		return weekName;
	}

	public void setWeekName(String weekName) {
		this.weekName = weekName;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getMonth() {
		return month;
	}

    public void setMonth(String month) {
        this.month = month;
    }

    public String getTxnTypes() {
        return txnTypes;
    }

    public void setTxnTypes(String txnTypes) {
        this.txnTypes = txnTypes;
    }

    public String getEmptyConsume() {
        return emptyConsume;
    }

    public void setEmptyConsume(String emptyConsume) {
        this.emptyConsume = emptyConsume;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getMerchantToken() {
        return merchantToken;
    }

    public void setMerchantToken(String merchantToken) {
        this.merchantToken = merchantToken;
    }
}
