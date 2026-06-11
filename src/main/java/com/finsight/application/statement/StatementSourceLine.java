package com.finsight.application.statement;

import java.util.ArrayList;
import java.util.List;

/** One parsed row from statement.content for UI inspection. */
public class StatementSourceLine {
    /** 1-based index among loaded source rows. */
    private int lineNumber;
    /** 1-based line number in the original file content. */
    private int fileLineNumber;
    private String originalLine;
    private List<String> columns = new ArrayList<>();
    /** linked | skipped | ignored | header | noise */
    private String kind;
    private String reason;
    private String hint;

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getFileLineNumber() {
        return fileLineNumber;
    }

    public void setFileLineNumber(int fileLineNumber) {
        this.fileLineNumber = fileLineNumber;
    }

    public String getOriginalLine() {
        return originalLine;
    }

    public void setOriginalLine(String originalLine) {
        this.originalLine = originalLine;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns == null ? new ArrayList<>() : columns;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
