package com.finsight.application.statement;

import java.util.ArrayList;
import java.util.List;

/**
 * One raw statement line that was not imported as a transaction during preview parse.
 */
public class SkippedImportRow {
    /** 1-based index among non-blank data rows (legacy / table row #). */
    private int lineNumber;
    /** 1-based line number in the uploaded file content. */
    private int fileLineNumber;
    private String rawText;
    /** Exact line text from the file (before cell joining). */
    private String originalLine;
    private List<String> columns = new ArrayList<>();
    private String reason;
    /** Machine-readable parse hints (column split, detected date/amounts, etc.). */
    private String hint;
    private String contextBefore;
    private String contextAfter;

    public SkippedImportRow() {
    }

    public SkippedImportRow(int lineNumber, String rawText, String reason) {
        this.lineNumber = lineNumber;
        this.rawText = rawText;
        this.reason = reason;
    }

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

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
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

    public String getContextBefore() {
        return contextBefore;
    }

    public void setContextBefore(String contextBefore) {
        this.contextBefore = contextBefore;
    }

    public String getContextAfter() {
        return contextAfter;
    }

    public void setContextAfter(String contextAfter) {
        this.contextAfter = contextAfter;
    }
}
