package com.finsight.application.statement;

import com.finsight.application.importer.StatementImporterFactory;
import com.finsight.domain.model.Statement;
import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class StatementSkippedLinesService {

    private static final Pattern DATE_TOKEN = Pattern.compile("\\d{4}[-./]?\\d{2}[-./]?\\d{2}");
    private static final Pattern AMOUNT_TOKEN = Pattern.compile("[-+]?\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|[-+]?\\d+(?:\\.\\d{1,2})?");
    private static final Pattern STRICT_DATE = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}|\\d{8})$");

    private static final class SourceRow {
        final int fileLineNumber;
        final String originalLine;
        final String[] cells;

        SourceRow(int fileLineNumber, String originalLine, String[] cells) {
            this.fileLineNumber = fileLineNumber;
            this.originalLine = originalLine;
            this.cells = cells;
        }
    }

    public List<SkippedImportRow> analyze(Statement statement, String cardTypeCode) {
        if (statement == null || StringUtils.isBlank(statement.getContent())) {
            return List.of();
        }
        List<SourceRow> sourceRows = loadSourceRows(statement);
        if (sourceRows.isEmpty()) {
            return List.of();
        }

        String bankCode = StringUtils.trimToEmpty(statement.getSource());
        String cardType = StringUtils.defaultIfBlank(cardTypeCode, "debit");
        List<String[]> rowArrays = sourceRows.stream().map(r -> r.cells).collect(Collectors.toList());
        List<Transaction> parsed = StatementImporterFactory.get(bankCode, cardType)
                .parse(rowArrays, bankCode, cardType, null);
        Set<Integer> consumed = matchRowsToTransactions(rowArrays, parsed == null ? List.of() : parsed);

        List<SkippedImportRow> skipped = new ArrayList<>();
        for (int i = 0; i < sourceRows.size(); i++) {
            if (consumed.contains(i)) {
                continue;
            }
            SourceRow source = sourceRows.get(i);
            String raw = formatRaw(source.cells);
            if (StringUtils.isBlank(raw)) {
                continue;
            }
            skipped.add(buildSkippedRow(source, i + 1, raw, bankCode, cardType, sourceRows, i));
        }
        return skipped;
    }

    private SkippedImportRow buildSkippedRow(
            SourceRow source,
            int rowIndex,
            String raw,
            String bankCode,
            String cardType,
            List<SourceRow> allRows,
            int index) {
        SkippedImportRow row = new SkippedImportRow();
        row.setLineNumber(rowIndex);
        row.setFileLineNumber(source.fileLineNumber);
        row.setOriginalLine(source.originalLine);
        row.setRawText(raw);
        row.setColumns(toColumnList(source.cells));
        row.setReason(classifySkipReason(source.cells, raw, bankCode, cardType));
        row.setHint(buildHint(source.cells, raw, bankCode, cardType));
        if (index > 0) {
            row.setContextBefore(StringUtils.trimToEmpty(allRows.get(index - 1).originalLine));
        }
        if (index + 1 < allRows.size()) {
            row.setContextAfter(StringUtils.trimToEmpty(allRows.get(index + 1).originalLine));
        }
        return row;
    }

    private List<String> toColumnList(String[] cells) {
        List<String> cols = new ArrayList<>();
        if (cells == null) {
            return cols;
        }
        for (String cell : cells) {
            cols.add(StringUtils.trimToEmpty(cell));
        }
        return cols;
    }

    private List<SourceRow> loadSourceRows(Statement statement) {
        String content = statement.getContent();
        String fileName = StringUtils.lowerCase(StringUtils.trimToEmpty(statement.getFileName()));
        if (fileName.endsWith(".pdf")) {
            return parsePlainTableRows(content);
        }
        return parseCsvRows(content);
    }

    private List<SourceRow> parseCsvRows(String content) {
        String[] lines = content.split("\n", -1);
        List<SourceRow> rows = new ArrayList<>();
        String delimiter = content.contains(",") ? "," : "\\t";
        if (content.contains(";")) {
            delimiter = ";";
        }
        for (int i = 0; i < lines.length; i++) {
            String original = lines[i];
            String line = StringUtils.trim(original);
            if (StringUtils.isBlank(line)) {
                continue;
            }
            String[] cols;
            if (",".equals(delimiter)) {
                cols = line.split("\\s*,\\s*");
            } else {
                cols = line.split(delimiter);
            }
            rows.add(new SourceRow(i + 1, original, cols));
        }
        return rows;
    }

    private List<SourceRow> parsePlainTableRows(String content) {
        String[] lines = content.split("\n", -1);
        List<SourceRow> rows = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String original = lines[i];
            String ln = StringUtils.trimToEmpty(original);
            if (StringUtils.isBlank(ln)) {
                continue;
            }
            rows.add(new SourceRow(i + 1, original, ln.split("\\s+")));
        }
        return rows;
    }

    private Set<Integer> matchRowsToTransactions(List<String[]> rows, List<Transaction> transactions) {
        Set<Integer> matched = new HashSet<>();
        boolean[] txnUsed = new boolean[transactions.size()];
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row == null) {
                continue;
            }
            for (int t = 0; t < transactions.size(); t++) {
                if (txnUsed[t]) {
                    continue;
                }
                if (rowMatches(row, transactions.get(t))) {
                    matched.add(i);
                    txnUsed[t] = true;
                    break;
                }
            }
        }
        return matched;
    }

    private boolean rowMatches(String[] row, Transaction txn) {
        if (txn == null) {
            return false;
        }
        String joined = formatRaw(row);
        if (StringUtils.isBlank(joined)) {
            return false;
        }

        if (txn.getTransactionDate() != null) {
            String ymd = new SimpleDateFormat("yyyy-MM-dd").format(txn.getTransactionDate());
            String compact = ymd.replace("-", "");
            if (!joined.contains(ymd) && !joined.contains(compact)) {
                return false;
            }
        }

        double income = txn.getIncomeMoney() == null ? 0.0 : Math.max(0.0, txn.getIncomeMoney());
        double expense = txn.getBalanceMoney() == null ? 0.0 : Math.max(0.0, txn.getBalanceMoney());
        double amount = Math.max(income, expense);
        if (amount <= 0) {
            return StringUtils.isNotBlank(txn.getTransactionDesc()) && joined.contains(txn.getTransactionDesc().trim());
        }
        return amountPresentInText(joined, amount);
    }

    private boolean amountPresentInText(String text, double amount) {
        String normalized = text.replace(",", "");
        String[] candidates = {
                String.format(Locale.ROOT, "%.2f", amount),
                String.format(Locale.ROOT, "%.2f", -amount),
                new DecimalFormat("0.##").format(amount),
        };
        for (String c : candidates) {
            if (normalized.contains(c)) {
                return true;
            }
        }
        return false;
    }

    private String classifySkipReason(String[] row, String raw, String bankCode, String cardType) {
        if (row == null || row.length == 0 || StringUtils.isBlank(raw)) {
            return "Empty line";
        }
        if (isHeaderRow(raw)) {
            return "Column header row";
        }
        if (isNoiseLine(raw)) {
            return "Statement metadata (title, page footer, totals, or bank boilerplate)";
        }
        if (isBeforeTablePreamble(raw, bankCode)) {
            return "Preamble before transaction table";
        }

        String firstDate = firstDateToken(raw);
        boolean hasDate = StringUtils.isNotBlank(firstDate);
        boolean hasAmount = hasAmountToken(raw);

        if (!hasDate) {
            if (looksLikeContinuationLine(raw, row)) {
                return "Continuation line — merchant/counterparty text split across rows (no date on this line)";
            }
            if (raw.length() < 12) {
                return "Too short — no transaction date found";
            }
            return "No transaction date — likely a label or note line";
        }
        if (!STRICT_DATE.matcher(normalizeDateToken(firstDate)).matches() && !DATE_TOKEN.matcher(raw).find()) {
            return "Unrecognized date format";
        }
        if (!hasAmount) {
            return "Has date but no amount — not imported as a transaction";
        }
        if (raw.contains(",")) {
            return "Parser rejected row — likely comma in balance/amount breaks token split "
                    + "(bank=" + bankCode + ", card=" + cardType + ")";
        }
        return "Parser could not build a transaction from this row "
                + "(bank=" + bankCode + ", card=" + cardType + ", columns=" + row.length + ")";
    }

    private boolean looksLikeContinuationLine(String raw, String[] row) {
        if (row != null && row.length == 1) {
            return true;
        }
        if (raw.length() > 80 && !hasAmountToken(raw)) {
            return true;
        }
        return raw.matches(".*[\\u4e00-\\u9fa5A-Za-z].*") && !DATE_TOKEN.matcher(raw).find();
    }

    private String buildHint(String[] row, String raw, String bankCode, String cardType) {
        StringBuilder sb = new StringBuilder();
        sb.append("bank=").append(bankCode).append(", card=").append(cardType);
        sb.append(", columns=").append(row == null ? 0 : row.length);
        if (row != null && row.length > 0) {
            sb.append(" → ");
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    sb.append(" | ");
                }
                sb.append('[').append(i).append(']').append(StringUtils.abbreviate(StringUtils.trimToEmpty(row[i]), 64));
            }
        }
        String date = firstDateToken(raw);
        if (StringUtils.isNotBlank(date)) {
            sb.append("; date=").append(date);
        }
        List<String> amounts = extractAmountTokens(raw);
        if (!amounts.isEmpty()) {
            sb.append("; amounts=").append(String.join(", ", amounts));
        }
        if (raw.contains(",")) {
            sb.append("; comma_in_line=yes (CMB PDF often fails when balance has thousands separator)");
        }
        return sb.toString();
    }

    private List<String> extractAmountTokens(String raw) {
        List<String> found = new ArrayList<>();
        java.util.regex.Matcher m = AMOUNT_TOKEN.matcher(raw);
        while (m.find()) {
            String token = m.group();
            if (token == null) {
                continue;
            }
            try {
                double v = Math.abs(Double.parseDouble(token.replace(",", "")));
                if (v >= 0.01 && v < 1_000_000_000) {
                    found.add(token);
                }
            } catch (NumberFormatException ignore) {
                // continue
            }
        }
        return found;
    }

    private boolean isHeaderRow(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        int hits = 0;
        if (raw.contains("交易日期") || raw.contains("交易日") || raw.contains("记账日期") || lower.contains("posting date")) {
            hits++;
        }
        if (raw.contains("支出") || raw.contains("收入") || raw.contains("交易金额") || raw.contains("借方") || raw.contains("贷方")
                || lower.contains("debit") || lower.contains("credit") || lower.contains("amount")) {
            hits++;
        }
        if (raw.contains("摘要") || raw.contains("对方") || raw.contains("余额") || lower.contains("balance") || lower.contains("narration")) {
            hits++;
        }
        return hits >= 2;
    }

    private boolean isNoiseLine(String raw) {
        String s = raw.trim();
        if (s.length() < 3) {
            return true;
        }
        if (s.contains("招商银行交易流水") || s.contains("Transaction Statement of China Merchants Bank")) {
            return true;
        }
        if (s.matches(".*第\\s*\\d+\\s*页.*共\\s*\\d+\\s*页.*") || s.toLowerCase(Locale.ROOT).contains("page ")) {
            return true;
        }
        if (s.startsWith("合计") || s.startsWith("Total") || s.contains("温馨提示") || s.contains("声明")) {
            return true;
        }
        if (s.contains("华润银行") && !DATE_TOKEN.matcher(s).find()) {
            return true;
        }
        return false;
    }

    private boolean isBeforeTablePreamble(String raw, String bankCode) {
        if ("CRBANK".equalsIgnoreCase(bankCode) && !DATE_TOKEN.matcher(raw).find()) {
            return raw.contains("账号") || raw.contains("户名") || raw.contains("起止日期") || raw.contains("币种");
        }
        return false;
    }

    private boolean hasAmountToken(String raw) {
        return !extractAmountTokens(raw).isEmpty();
    }

    private String firstDateToken(String raw) {
        java.util.regex.Matcher m = DATE_TOKEN.matcher(raw);
        return m.find() ? normalizeDateToken(m.group()) : "";
    }

    private String normalizeDateToken(String token) {
        String t = StringUtils.trimToEmpty(token).replace('.', '-').replace('/', '-');
        if (t.matches("\\d{8}")) {
            return t.substring(0, 4) + "-" + t.substring(4, 6) + "-" + t.substring(6, 8);
        }
        return t;
    }

    private String formatRaw(String[] row) {
        if (row == null || row.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String cell : row) {
            String v = StringUtils.trimToEmpty(cell);
            if (StringUtils.isBlank(v)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(v);
        }
        return sb.toString().trim();
    }
}
