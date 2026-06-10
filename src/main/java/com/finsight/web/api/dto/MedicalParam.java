package com.finsight.web.restful.model;

/**
 * Created by Summer.Xia on 2015/10/13.
 */
public class MedicalParam extends PageParam {

    private String transactionDateStartStr;
    private String transactionDateEndStr;
    private String demoArea;
    private String unitNo;

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

    public String getDemoArea() {
        return demoArea;
    }

    public void setDemoArea(String demoArea) {
        this.demoArea = demoArea;
    }

    public String getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(String unitNo) {
        this.unitNo = unitNo;
    }
}
