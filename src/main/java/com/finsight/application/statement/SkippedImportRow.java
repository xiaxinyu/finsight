package com.finsight.application.statement;

/**
 * One raw statement line that was not imported as a transaction during preview parse.
 */
public class SkippedImportRow {
    private int lineNumber;
    private String rawText;
    private String reason;

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

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
