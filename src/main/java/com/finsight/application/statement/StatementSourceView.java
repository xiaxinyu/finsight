package com.finsight.application.statement;

import java.util.ArrayList;
import java.util.List;

/** Parsed statement.content for formatted UI display. */
public class StatementSourceView {
    private String statementId;
    private String fileName;
    private String bankCode;
    private List<String> columnHeaders = new ArrayList<>();
    private int lines;
    private int transactions;
    private int linked;
    private int skipped;
    private int ignored;
    private List<StatementSourceLine> rows = new ArrayList<>();

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public List<String> getColumnHeaders() {
        return columnHeaders;
    }

    public void setColumnHeaders(List<String> columnHeaders) {
        this.columnHeaders = columnHeaders == null ? new ArrayList<>() : columnHeaders;
    }

    public int getLines() {
        return lines;
    }

    public void setLines(int lines) {
        this.lines = lines;
    }

    public int getTransactions() {
        return transactions;
    }

    public void setTransactions(int transactions) {
        this.transactions = transactions;
    }

    public int getLinked() {
        return linked;
    }

    public void setLinked(int linked) {
        this.linked = linked;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getIgnored() {
        return ignored;
    }

    public void setIgnored(int ignored) {
        this.ignored = ignored;
    }

    public List<StatementSourceLine> getRows() {
        return rows;
    }

    public void setRows(List<StatementSourceLine> rows) {
        this.rows = rows == null ? new ArrayList<>() : rows;
    }
}
