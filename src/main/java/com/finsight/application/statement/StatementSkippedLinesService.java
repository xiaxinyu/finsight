package com.finsight.application.statement;

import com.finsight.application.importer.StatementImporterFactory;
import com.finsight.domain.model.Statement;
import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class StatementSkippedLinesService {

    private static final Pattern DATE_TOKEN = Pattern.compile("\\d{4}[-./]?\\d{2}[-./]?\\d{2}");
    private static final Pattern AMOUNT_TOKEN = Pattern.compile(
            "[-+]?\\d{1,3}(?:,\\d{3})+\\.\\d{1,2}|[-+]?\\d+\\.\\d{1,2}");
    private static final Pattern STRICT_DATE = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}|\\d{8})$");
    private static final Pattern CURRENCY_TOKEN = Pattern.compile(
            "^(?i)(CNY|RMB|人民币|GBP|USD|EUR|HKD|JPY|AUD|CAD|SGD|CHF)$");

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
        return summarize(statement, cardTypeCode, -1).skippedRows();
    }

    public ImportLineStats summarize(Statement statement, String cardTypeCode, int transactionCount) {
        if (statement == null || StringUtils.isBlank(statement.getContent())) {
            return new ImportLineStats(0, Math.max(0, transactionCount), 0, 0, 0, List.of());
        }
        List<SourceRow> sourceRows = loadSourceRows(statement);
        if (sourceRows.isEmpty()) {
            return new ImportLineStats(0, Math.max(0, transactionCount), 0, 0, 0, List.of());
        }

        String bankCode = StringUtils.trimToEmpty(statement.getSource());
        String cardType = StringUtils.defaultIfBlank(cardTypeCode, "debit");
        List<String[]> rowArrays = sourceRows.stream().map(r -> r.cells).collect(Collectors.toList());
        List<Transaction> parsed = StatementImporterFactory.get(bankCode, cardType)
                .parse(rowArrays, bankCode, cardType, null);
        int txnCount = transactionCount >= 0
                ? transactionCount
                : (parsed == null ? 0 : parsed.size());
        Set<Integer> consumed = matchRowsToTransactions(rowArrays, parsed == null ? List.of() : parsed);
        markMergedContinuationRows(rowArrays, consumed);

        List<SkippedImportRow> skipped = new ArrayList<>();
        for (int i = 0; i < sourceRows.size(); i++) {
            if (consumed.contains(i)) {
                continue;
            }
            SourceRow source = sourceRows.get(i);
            String raw = formatRaw(source.cells);
            if (StringUtils.isBlank(raw) || isNoiseLine(raw)) {
                continue;
            }
            if (isHeaderRow(raw) || looksLikeContinuationLine(raw, source.cells)) {
                continue;
            }
            skipped.add(buildSkippedRow(source, i + 1, raw, bankCode, cardType, sourceRows, i));
        }
        int lines = sourceRows.size();
        int linked = consumed.size();
        int skippedCount = skipped.size();
        int ignored = Math.max(0, lines - linked - skippedCount);
        return new ImportLineStats(lines, txnCount, linked, skippedCount, ignored, skipped);
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
        if (transactions == null || transactions.isEmpty()) {
            return matched;
        }
        boolean[] txnUsed = new boolean[transactions.size()];
        Map<String, Deque<Integer>> txnByKey = indexTransactionsByDateAndPrimaryAmount(transactions);

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row == null) {
                continue;
            }
            String rowKey = rowMatchKey(row);
            if (rowKey == null) {
                continue;
            }
            Deque<Integer> pool = txnByKey.get(rowKey);
            if (pool == null || pool.isEmpty()) {
                continue;
            }
            int bestTxn = pickBestTxnIndex(row, pool, transactions, txnUsed);
            if (bestTxn >= 0) {
                txnUsed[bestTxn] = true;
                matched.add(i);
            }
        }

        boolean[] rowUsed = new boolean[rows.size()];
        for (int idx : matched) {
            rowUsed[idx] = true;
        }
        List<MatchCandidate> weak = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (rowUsed[i] || rows.get(i) == null) {
                continue;
            }
            for (int t = 0; t < transactions.size(); t++) {
                if (txnUsed[t]) {
                    continue;
                }
                int score = rowMatchScore(rows.get(i), transactions.get(t));
                if (score >= 10) {
                    weak.add(new MatchCandidate(score, i, t));
                }
            }
        }
        weak.sort(Comparator.comparingInt(MatchCandidate::score).reversed());
        for (MatchCandidate c : weak) {
            if (!rowUsed[c.rowIdx()] && !txnUsed[c.txnIdx()]) {
                rowUsed[c.rowIdx()] = true;
                txnUsed[c.txnIdx()] = true;
                matched.add(c.rowIdx());
            }
        }
        return matched;
    }

    private Map<String, Deque<Integer>> indexTransactionsByDateAndPrimaryAmount(List<Transaction> transactions) {
        Map<String, Deque<Integer>> index = new HashMap<>();
        for (int t = 0; t < transactions.size(); t++) {
            String key = txnMatchKey(transactions.get(t));
            if (key == null) {
                continue;
            }
            index.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(t);
        }
        return index;
    }

    private int pickBestTxnIndex(
            String[] row,
            Deque<Integer> pool,
            List<Transaction> transactions,
            boolean[] txnUsed) {
        String joined = formatRaw(row);
        int bestTxn = -1;
        int bestScore = -1;
        for (int t : pool) {
            if (txnUsed[t]) {
                continue;
            }
            int score = rowDetailScore(joined, row, transactions.get(t));
            if (score > bestScore) {
                bestScore = score;
                bestTxn = t;
            }
        }
        return bestTxn;
    }

    private int rowDetailScore(String joined, String[] row, Transaction txn) {
        int score = 0;
        if (StringUtils.isNotBlank(txn.getTransactionDesc()) && joined.contains(txn.getTransactionDesc().trim())) {
            score += 5;
        }
        if (StringUtils.isNotBlank(txn.getOpponentName()) && joined.contains(txn.getOpponentName().trim())) {
            score += 3;
        }
        String memo = txn.getDemoArea();
        if (StringUtils.isNotBlank(memo)) {
            for (String part : memo.split("\\s+")) {
                if (part.length() >= 4 && joined.contains(part)) {
                    score += 2;
                    break;
                }
            }
        }
        return score;
    }

    private record MatchCandidate(int score, int rowIdx, int txnIdx) {
    }

    private String txnMatchKey(Transaction txn) {
        if (txn == null || txn.getTransactionDate() == null) {
            return null;
        }
        String date = new SimpleDateFormat("yyyy-MM-dd").format(txn.getTransactionDate());
        String amount = txnSignedAmountKey(txn);
        if (StringUtils.isBlank(amount)) {
            return null;
        }
        return date + "|" + amount;
    }

    private String rowMatchKey(String[] row) {
        if (row == null || row.length == 0) {
            return null;
        }
        String date = firstStrictDateCell(row);
        if (StringUtils.isBlank(date)) {
            return null;
        }
        String dualColumnKey = dualColumnRowMatchKey(row, date);
        if (dualColumnKey != null) {
            return dualColumnKey;
        }
        String primaryAmount = extractPrimaryAmountCell(row);
        if (StringUtils.isBlank(primaryAmount)) {
            return null;
        }
        try {
            double signed = Double.parseDouble(primaryAmount.replace(",", "").trim());
            if (Math.abs(signed) < 0.001) {
                return null;
            }
            return date + "|" + signedAmountKey(signed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** CRBANK-style CSV: date, channel, income, expense, balance, ... */
    private String dualColumnRowMatchKey(String[] row, String date) {
        if (row.length < 4 || !date.equals(normalizeDateToken(StringUtils.trimToEmpty(row[0])))) {
            return null;
        }
        if (isCurrencyCell(row[1])) {
            return null;
        }
        if (!isNumericAmountCell(row[2]) || !isNumericAmountCell(row[3])) {
            return null;
        }
        try {
            double income = Double.parseDouble(row[2].replace(",", "").trim());
            double expense = Double.parseDouble(row[3].replace(",", "").trim());
            if (income > 0.001) {
                return date + "|" + signedAmountKey(income);
            }
            if (expense > 0.001) {
                return date + "|" + signedAmountKey(-expense);
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return null;
    }

    private String txnSignedAmountKey(Transaction txn) {
        double income = txn.getIncomeMoney() == null ? 0.0 : Math.max(0.0, txn.getIncomeMoney());
        double expense = txn.getExpenseAmount() == null ? 0.0 : Math.max(0.0, txn.getExpenseAmount());
        if (expense <= 0.0 && txn.getBalanceMoney() != null && txn.getBalanceMoney() > 0) {
            expense = txn.getBalanceMoney();
        }
        if (income > 0.001) {
            return signedAmountKey(income);
        }
        if (expense > 0.001) {
            return signedAmountKey(-expense);
        }
        return "";
    }

    private String signedAmountKey(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    private String firstStrictDateCell(String[] row) {
        for (String cell : row) {
            String normalized = normalizeDateToken(StringUtils.trimToEmpty(cell));
            if (STRICT_DATE.matcher(normalized).matches()) {
                return normalized;
            }
        }
        return "";
    }

    /**
     * Transaction amount only — never use balance column (e.g. 49,494.31), which collides with other txns.
     */
    private String extractPrimaryAmountCell(String[] row) {
        if (row == null || row.length < 2) {
            return "";
        }
        int dateIdx = -1;
        for (int i = 0; i < row.length; i++) {
            String normalized = normalizeDateToken(StringUtils.trimToEmpty(row[i]));
            if (STRICT_DATE.matcher(normalized).matches()) {
                dateIdx = i;
                break;
            }
        }
        if (dateIdx < 0) {
            return "";
        }
        if (dateIdx == 0 && row.length >= 4 && !isCurrencyCell(row[1])
                && isNumericAmountCell(row[2]) && isNumericAmountCell(row[3])) {
            try {
                double income = Double.parseDouble(row[2].replace(",", "").trim());
                double expense = Double.parseDouble(row[3].replace(",", "").trim());
                if (Math.abs(income) >= 0.001) {
                    return row[2];
                }
                if (Math.abs(expense) >= 0.001) {
                    return row[3];
                }
            } catch (NumberFormatException ignore) {
                // fall through
            }
        }
        for (int i = dateIdx + 1; i < row.length; i++) {
            if (isCurrencyCell(row[i]) && i + 1 < row.length && isAmountCell(row[i + 1])) {
                return StringUtils.trimToEmpty(row[i + 1]);
            }
        }
        for (int i = dateIdx + 1; i < row.length; i++) {
            if (isAmountCell(row[i])) {
                return StringUtils.trimToEmpty(row[i]);
            }
        }
        return "";
    }

    private boolean isCurrencyCell(String cell) {
        return StringUtils.isNotBlank(cell) && CURRENCY_TOKEN.matcher(cell.trim()).matches();
    }

    private boolean isNumericAmountCell(String cell) {
        if (StringUtils.isBlank(cell)) {
            return false;
        }
        String normalized = cell.replace(",", "").trim();
        try {
            Double.parseDouble(normalized);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean isAmountCell(String cell) {
        if (!isNumericAmountCell(cell)) {
            return false;
        }
        try {
            return Math.abs(Double.parseDouble(cell.replace(",", "").trim())) >= 0.001;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private int rowMatchScore(String[] row, Transaction txn) {
        if (txn == null) {
            return 0;
        }
        String joined = formatRaw(row);
        if (StringUtils.isBlank(joined)) {
            return 0;
        }
        int score = 0;
        if (txn.getTransactionDate() != null) {
            String ymd = new SimpleDateFormat("yyyy-MM-dd").format(txn.getTransactionDate());
            String compact = ymd.replace("-", "");
            if (!joined.contains(ymd) && !joined.contains(compact)) {
                return 0;
            }
            score += 10;
        }

        String rowKey = rowMatchKey(row);
        String txnKey = txnMatchKey(txn);
        if (rowKey != null && rowKey.equals(txnKey)) {
            return score + 100 + rowDetailScore(joined, row, txn);
        }
        if (StringUtils.isNotBlank(extractPrimaryAmountCell(row))) {
            return 0;
        }
        if (StringUtils.isNotBlank(txn.getTransactionDesc()) && joined.contains(txn.getTransactionDesc().trim())) {
            score += 5;
        }
        if (StringUtils.isNotBlank(txn.getOpponentName()) && joined.contains(txn.getOpponentName().trim())) {
            score += 3;
        }
        return score >= 10 ? score : 0;
    }

    /** Continuation rows (no date) are merged by CMB importer into the previous line — not parse failures. */
    private void markMergedContinuationRows(List<String[]> rows, Set<Integer> consumed) {
        for (int i = 0; i < rows.size(); i++) {
            if (consumed.contains(i) || i == 0) {
                continue;
            }
            if (!consumed.contains(i - 1)) {
                continue;
            }
            String raw = formatRaw(rows.get(i));
            if (!isNoiseLine(raw) && looksLikeContinuationLine(raw, rows.get(i))) {
                consumed.add(i);
            }
        }
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
        return "Row not linked to a parsed transaction "
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
            sb.append("; comma_in_line=yes (thousands separator — importer strips commas before parse)");
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
        if (s.matches("\\d{1,3}/\\d{1,3}")) {
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
