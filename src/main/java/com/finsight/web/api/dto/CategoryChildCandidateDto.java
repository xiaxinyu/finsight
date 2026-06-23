package com.finsight.web.api.dto;

/**
 * Proposed L2 category from seed/audit catalog (insert not executed until user confirms).
 */
public class CategoryChildCandidateDto {

    private String code;
    private String name;
    private String parentL1Code;
    private int sortNo;
    private String txnTypes;
    private String reportRole;
    private String reason;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentL1Code() {
        return parentL1Code;
    }

    public void setParentL1Code(String parentL1Code) {
        this.parentL1Code = parentL1Code;
    }

    public int getSortNo() {
        return sortNo;
    }

    public void setSortNo(int sortNo) {
        this.sortNo = sortNo;
    }

    public String getTxnTypes() {
        return txnTypes;
    }

    public void setTxnTypes(String txnTypes) {
        this.txnTypes = txnTypes;
    }

    public String getReportRole() {
        return reportRole;
    }

    public void setReportRole(String reportRole) {
        this.reportRole = reportRole;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
