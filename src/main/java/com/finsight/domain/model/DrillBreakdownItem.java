package com.finsight.domain.model;

import java.io.Serializable;

/** Aggregated row for drill-down category or merchant breakdown. */
public class DrillBreakdownItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String token;
    private String label;
    private Integer txnCount;
    private Double total;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getTxnCount() {
        return txnCount;
    }

    public void setTxnCount(Integer txnCount) {
        this.txnCount = txnCount;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}
