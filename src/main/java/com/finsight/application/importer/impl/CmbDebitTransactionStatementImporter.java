package com.finsight.application.importer.impl;

import com.finsight.application.importer.StatementImporter;
import com.finsight.common.util.DateTool;
import com.finsight.common.util.StringTool;
import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CmbDebitTransactionStatementImporter implements StatementImporter {
    private static final Logger log = LoggerFactory.getLogger(CmbDebitTransactionStatementImporter.class);

    private static final Pattern DATE_TOKEN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern DATE_IN_LINE = Pattern.compile("\\d{4}[-./]\\d{2}[-./]\\d{2}");
    private static final Pattern AMOUNT_STRICT = Pattern.compile("^[-+]?\\d+(?:\\.\\d{1,2})?$");

    @Override
    public List<Transaction> parse(List<String[]> rows, String bankCode, String cardTypeCode, String cardNo) {
        List<Transaction> list = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return list;
        }

        List<String[]> merged = mergeContinuationLines(rows);
        log.info("Starting CMB parsing, raw rows={}, merged rows={}", rows.size(), merged.size());

        int summaryCol = -1;
        int incomeCol = -1;
        int expenseCol = -1;
        boolean dualAmountColumns = false;
        boolean headerInitialized = false;

        for (String[] row : merged) {
            if (row == null || row.length == 0) {
                continue;
            }

            if (!headerInitialized) {
                HeaderScan scan = scanHeader(row);
                if (scan.summaryCol >= 0 || scan.dualAmountColumns) {
                    summaryCol = scan.summaryCol;
                    incomeCol = scan.incomeCol;
                    expenseCol = scan.expenseCol;
                    dualAmountColumns = scan.dualAmountColumns;
                    headerInitialized = true;
                    continue;
                }
            }

            String line = normalizeLine(StringUtils.join(row, " ").trim());
            if (StringUtils.isBlank(line) || isNoiseLine(line)) {
                continue;
            }
            if (headerInitialized && isHeaderLine(line)) {
                continue;
            }

            if (dualAmountColumns && incomeCol >= 0 && expenseCol >= 0 && row.length > Math.max(incomeCol, expenseCol)) {
                Transaction dual = parseDualColumnRow(row, summaryCol, incomeCol, expenseCol, cardTypeCode, cardNo);
                if (dual != null) {
                    list.add(dual);
                    continue;
                }
            }

            Transaction t = parseTokenLine(line, row, summaryCol, cardTypeCode, cardNo);
            if (t != null) {
                list.add(t);
            }
        }

        log.info("CMB importer finished, parsed transactions={}", list.size());
        return list;
    }

    private static class HeaderScan {
        int summaryCol = -1;
        int incomeCol = -1;
        int expenseCol = -1;
        boolean dualAmountColumns;
    }

    private HeaderScan scanHeader(String[] row) {
        HeaderScan scan = new HeaderScan();
        for (int i = 0; i < row.length; i++) {
            String col = StringTool.cleanStr(row[i]);
            if (StringUtils.isBlank(col)) {
                continue;
            }
            String lower = col.toLowerCase(Locale.ROOT);
            if (col.contains("交易摘要") || lower.contains("transaction type") || col.contains("摘要")) {
                scan.summaryCol = i;
            }
            if (col.contains("收入") || col.contains("贷方") || lower.contains("credit amount")) {
                scan.incomeCol = i;
            }
            if (col.contains("支出") || col.contains("借方") || lower.contains("debit amount")) {
                scan.expenseCol = i;
            }
        }
        scan.dualAmountColumns = scan.incomeCol >= 0 && scan.expenseCol >= 0;
        if (scan.summaryCol >= 0 || scan.dualAmountColumns) {
            return scan;
        }
        return new HeaderScan();
    }

    private List<String[]> mergeContinuationLines(List<String[]> rows) {
        List<String[]> merged = new ArrayList<>();
        String[] pending = null;
        for (String[] row : rows) {
            if (row == null || row.length == 0) {
                continue;
            }
            String line = StringUtils.join(row, " ").trim();
            if (StringUtils.isBlank(line)) {
                continue;
            }
            if (isNoiseLine(line)) {
                continue;
            }
            if (pending == null || looksLikeTransactionStart(line)) {
                if (pending != null) {
                    merged.add(pending);
                }
                pending = row;
            } else if (pending != null) {
                pending = concatRows(pending, row);
            }
        }
        if (pending != null) {
            merged.add(pending);
        }
        return merged;
    }

    private String[] concatRows(String[] left, String[] right) {
        String[] out = new String[left.length + right.length];
        System.arraycopy(left, 0, out, 0, left.length);
        System.arraycopy(right, 0, out, left.length, right.length);
        return out;
    }

    private boolean looksLikeTransactionStart(String line) {
        return DATE_IN_LINE.matcher(line).find();
    }

    private boolean isHeaderLine(String line) {
        return line.contains("记账日期") && line.contains("交易金额")
                || line.contains("交易日") && line.contains("收入") && line.contains("支出");
    }

    private boolean isNoiseLine(String line) {
        String s = line.trim();
        if (s.length() < 3) {
            return true;
        }
        if (s.contains("招商银行交易流水") || s.contains("Transaction Statement of China Merchants Bank")) {
            return true;
        }
        if (s.matches(".*第\\s*\\d+\\s*页.*共\\s*\\d+\\s*页.*") || s.toLowerCase(Locale.ROOT).contains("page ")) {
            return true;
        }
        if (s.contains("Date") && s.contains("Amount") && !s.contains("记账")) {
            return true;
        }
        if (s.startsWith("合计") || s.startsWith("Total") || s.contains("温馨提示")) {
            return true;
        }
        if (s.contains("户名") && s.contains("账号") || s.contains("Account No") && s.contains("Name")) {
            return true;
        }
        return false;
    }

    private String normalizeLine(String line) {
        String out = line;
        out = out.replaceAll("(\\d{4})[./](\\d{2})[./](\\d{2})", "$1-$2-$3");
        out = out.replaceAll("(\\d{4}-\\d{2}-\\d{2})", " $1 ");
        out = out.replaceAll("(?i)(CNY|人民币|RMB)", " CNY ");
        while (out.contains(",")) {
            String next = out.replaceAll("(\\d),(\\d{3})", "$1$2");
            if (next.equals(out)) {
                break;
            }
            out = next;
        }
        return out.replaceAll("\\s+", " ").trim();
    }

    private Transaction parseDualColumnRow(String[] row, int summaryCol, int incomeCol, int expenseCol,
                                           String cardTypeCode, String cardNo) {
        int dateCol = findDateColumn(row);
        if (dateCol < 0) {
            return null;
        }
        String dateStr = normalizeDateToken(StringTool.cleanStr(row[dateCol]));
        Date txnDate = parseDate(dateStr);
        if (txnDate == null) {
            return null;
        }

        double income = parseDouble(row[incomeCol]);
        double expense = parseDouble(row[expenseCol]);
        if (income <= 0 && expense <= 0) {
            return null;
        }

        String desc = summaryCol >= 0 && summaryCol < row.length
                ? StringTool.cleanStr(row[summaryCol])
                : "";
        if (StringUtils.isBlank(desc)) {
            desc = joinBetween(row, dateCol + 1, Math.min(incomeCol, expenseCol));
        }

        Transaction t = baseTransaction(cardTypeCode, cardNo);
        t.setTransactionDate(txnDate);
        t.setBookKeepingDate(txnDate);
        t.setTransactionDesc(StringUtils.defaultIfBlank(desc, "CMB transaction"));
        t.setIncomeMoney(income > 0 ? income : 0.0);
        t.setBalanceMoney(expense > 0 ? expense : 0.0);
        attachTrailingFields(t, row, Math.max(incomeCol, expenseCol) + 1);
        return t;
    }

    private Transaction parseTokenLine(String line, String[] row, int summaryCol, String cardTypeCode, String cardNo) {
        String[] tokens = line.split(" ");
        if (tokens.length < 3) {
            log.debug("Line skipped (too short): {}", line);
            return null;
        }

        int postingIdx = -1;
        int txnIdx = -1;
        for (int i = 0; i < tokens.length; i++) {
            String tk = normalizeDateToken(tokens[i]);
            if (DATE_TOKEN.matcher(tk).matches()) {
                tokens[i] = tk;
                if (postingIdx == -1) {
                    postingIdx = i;
                } else if (txnIdx == -1) {
                    txnIdx = i;
                    break;
                }
            }
        }
        if (postingIdx == -1) {
            log.debug("Line skipped (no date found): {}", line);
            return null;
        }
        if (txnIdx == -1) {
            txnIdx = postingIdx;
        }

        int amountIdx = -1;
        int balanceIdx = -1;
        for (int i = txnIdx + 1; i < tokens.length; i++) {
            if (isAmountToken(tokens[i])) {
                amountIdx = i;
                if (i + 1 < tokens.length && isAmountToken(tokens[i + 1])) {
                    balanceIdx = i + 1;
                }
                break;
            }
        }

        if (amountIdx == -1) {
            for (int i = txnIdx + 1; i < tokens.length; i++) {
                if (isAmountToken(tokens[i]) && !equalsIgnoreCase(tokens[i], "CNY")) {
                    amountIdx = i;
                    if (i + 1 < tokens.length && isAmountToken(tokens[i + 1])) {
                        balanceIdx = i + 1;
                    }
                    break;
                }
            }
        }

        String narration = "";
        double income = 0.0;
        double expense = 0.0;
        double balance = 0.0;
        String opponentInfo = "";

        if (amountIdx != -1) {
            List<String> narTokens = new ArrayList<>();
            for (int k = txnIdx + 1; k < amountIdx; k++) {
                if (!equalsIgnoreCase(tokens[k], "CNY") && !"人民币".equals(tokens[k])) {
                    narTokens.add(tokens[k]);
                }
            }
            narration = String.join(" ", narTokens);

            Double amt = parseDouble(tokens[amountIdx]);
            if (amt > 0) {
                income = amt;
            } else if (amt < 0) {
                expense = Math.abs(amt);
            }

            int nextIdx = amountIdx + 1;
            if (balanceIdx != -1) {
                balance = parseDouble(tokens[balanceIdx]);
                nextIdx = balanceIdx + 1;
            }
            if (nextIdx < tokens.length) {
                opponentInfo = String.join(" ", java.util.Arrays.copyOfRange(tokens, nextIdx, tokens.length));
            }
        } else {
            narration = String.join(" ", java.util.Arrays.copyOfRange(tokens, txnIdx + 1, tokens.length))
                    .replaceAll("(?i)CNY", "").trim();
        }

        if (income <= 0 && expense <= 0) {
            log.debug("Line skipped (no amount): {}", line);
            return null;
        }

        String summaryCandidate = getValue(row, summaryCol);
        String desc = StringUtils.isNotBlank(summaryCandidate) ? summaryCandidate : StringUtils.trimToEmpty(narration);

        String opponentAcc = "";
        String opponentName = "";
        if (StringUtils.isNotBlank(opponentInfo)) {
            String digits = opponentInfo.replaceAll("\\D", "");
            if (digits.length() >= 10) {
                opponentAcc = digits;
                opponentName = opponentInfo.replace(digits, "").trim();
            } else {
                opponentName = opponentInfo;
            }
        }
        if (StringUtils.isBlank(desc)) {
            desc = StringUtils.firstNonBlank(opponentName, opponentAcc, "CMB transaction");
        }

        Date postingDate = parseDate(tokens[postingIdx]);
        Date txnDate = parseDate(tokens[txnIdx]);
        if (postingDate == null || txnDate == null) {
            log.debug("Line skipped (date parse failed): {}", line);
            return null;
        }

        Transaction t = baseTransaction(cardTypeCode, cardNo);
        t.setBookKeepingDate(postingDate);
        t.setTransactionDate(txnDate);
        t.setTransactionDesc(desc);
        t.setIncomeMoney(income);
        t.setBalanceMoney(expense);
        t.setAccountBalance(balance);
        if (StringUtils.isNotBlank(opponentInfo)) {
            String note = opponentInfo.trim();
            t.setDemoArea(note.length() > 512 ? note.substring(0, 512) : note);
        }
        t.setOpponentAccount(opponentAcc);
        t.setOpponentName(opponentName);
        try {
            t.setTransactionDateTime(new java.text.SimpleDateFormat("yyyy-MM-dd").format(t.getTransactionDate()));
        } catch (Exception ignore) {
            // no-op
        }
        return t;
    }

    private Transaction baseTransaction(String cardTypeCode, String cardNo) {
        Transaction t = new Transaction();
        t.setId(StringTool.generateID());
        t.setCardId(StringUtils.isNotBlank(cardNo) ? StringTool.cleanStr(cardNo) : "");
        t.setBalanceCurrency("CNY");
        if ("credit".equalsIgnoreCase(cardTypeCode)) {
            t.setCardTypeId(1);
            t.setCardTypeName("信用卡");
        } else {
            t.setCardTypeId(2);
            t.setCardTypeName("储蓄卡");
        }
        return t;
    }

    private void attachTrailingFields(Transaction t, String[] row, int fromIdx) {
        if (fromIdx >= row.length) {
            return;
        }
        String tail = joinBetween(row, fromIdx, row.length);
        if (StringUtils.isBlank(tail)) {
            return;
        }
        String digits = tail.replaceAll("\\D", "");
        if (digits.length() >= 10) {
            t.setOpponentAccount(digits);
            t.setOpponentName(tail.replace(digits, "").trim());
        } else {
            t.setOpponentName(tail);
        }
        t.setDemoArea(tail.length() > 512 ? tail.substring(0, 512) : tail);
    }

    private int findDateColumn(String[] row) {
        for (int i = 0; i < row.length; i++) {
            String token = normalizeDateToken(StringTool.cleanStr(row[i]));
            if (DATE_TOKEN.matcher(token).matches()) {
                return i;
            }
        }
        return -1;
    }

    private String joinBetween(String[] row, int from, int to) {
        List<String> parts = new ArrayList<>();
        int end = Math.min(to, row.length);
        for (int i = Math.max(0, from); i < end; i++) {
            String v = StringTool.cleanStr(row[i]);
            if (StringUtils.isNotBlank(v) && !isAmountToken(v) && !DATE_TOKEN.matcher(normalizeDateToken(v)).matches()) {
                parts.add(v);
            }
        }
        return String.join(" ", parts).trim();
    }

    private String normalizeDateToken(String token) {
        if (StringUtils.isBlank(token)) {
            return "";
        }
        return token.replaceAll("[./]", "-").trim();
    }

    private Date parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = normalizeDateToken(value);
        try {
            return DateTool.changeStringToDate(normalized, DateTool.DF_YYYY_MM_DD);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAmountToken(String token) {
        if (StringUtils.isBlank(token) || equalsIgnoreCase(token, "CNY") || "人民币".equals(token)) {
            return false;
        }
        return AMOUNT_STRICT.matcher(token.replace(",", "")).matches();
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    private String getValue(String[] row, int index) {
        if (index >= 0 && index < row.length) {
            return StringTool.cleanStr(row[index]);
        }
        return "";
    }

    private Double parseDouble(String str) {
        if (StringUtils.isBlank(str)) {
            return 0.0;
        }
        try {
            return Double.valueOf(str.replaceAll("[^0-9\\-.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }
}
