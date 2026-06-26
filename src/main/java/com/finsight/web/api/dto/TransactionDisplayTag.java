package com.finsight.web.api.dto;

/**
 * Server-authored label for transaction list / ledger UI (from finance semantics).
 */
public class TransactionDisplayTag {

    private String id;
    private String label;
    private String color;
    private String hint;

    public TransactionDisplayTag() {
    }

    public TransactionDisplayTag(String id, String label, String color, String hint) {
        this.id = id;
        this.label = label;
        this.color = color;
        this.hint = hint;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
