package com.finsight.application.importer.impl;

import com.finsight.application.importer.StatementImporter;
import com.finsight.application.transaction.TransactionAmountNormalizer;
import com.finsight.common.util.DateTool;
import com.finsight.common.util.StringTool;
import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ABC (Agricultural Bank of China / 农业银行) credit card CSV export.
 * <p>
 * Typical columns: 交易日期, 入账日期, 卡号后四位, 交易摘要, 交易地点, 交易金额, 入账金额.
 * Amounts may include currency suffix, e.g. {@code -299960/CNY}, {@code 5749.00/CNY}.
 * Integer amounts without a decimal point and {@code |value| >= 10000} are treated as fen (÷100).
 * Sign convention (ABC credit export): negative = charge/repayment outflow; positive = transfer/credit inflow.
 * Summary disambiguates {@code 跨行消费} (expense) vs {@code 还款转出} (finance repayment).
 */
public class AbcTransactionStatementImporter implements StatementImporter {

    private static final Pattern DATE8 = Pattern.compile("^\\d{8}$");
    private static final Pattern AMOUNT_WITH_CURRENCY = Pattern.compile(
            "^\\s*(-?\\d+(?:\\.\\d+)?)\\s*(?:/\\s*([A-Za-z]{3}|人民币|RMB|CNY))?\\s*$");

    private static final class ColumnMap {
        int txnDate = 0;
        int postDate = 1;
        int cardTail = 2;
        int summary = 3;
        int location = 4;
        int txnAmount = 5;
        int postAmount = 6;
        boolean resolved;

        static ColumnMap fromHeader(String[] row) {
            ColumnMap map = new ColumnMap();
            if (row == null) {
                return map;
            }
            boolean sawTxnDate = false;
            boolean sawAmount = false;
            for (int i = 0; i < row.length; i++) {
                String v = StringTool.cleanStr(row[i]);
                if (StringUtils.isBlank(v)) {
                    continue;
                }
                if (v.contains("交易日期") || v.contains("交易日")) {
                    map.txnDate = i;
                    sawTxnDate = true;
                    map.resolved = true;
                } else if (v.contains("入账日期") || v.contains("入账日")) {
                    map.postDate = i;
                    map.resolved = true;
                } else if (v.contains("卡号后四位") || v.contains("卡号") || v.contains("末四位")) {
                    map.cardTail = i;
                    map.resolved = true;
                } else if (v.contains("交易摘要") || (v.contains("摘要") && !v.contains("地点"))) {
                    map.summary = i;
                    map.resolved = true;
                } else if (v.contains("交易地点") || v.contains("地点") || v.contains("商户")) {
                    map.location = i;
                    map.resolved = true;
                } else if (v.contains("入账金额")) {
                    map.postAmount = i;
                    sawAmount = true;
                    map.resolved = true;
                } else if (v.contains("交易金额")) {
                    map.txnAmount = i;
                    sawAmount = true;
                    map.resolved = true;
                }
            }
            if (sawTxnDate && sawAmount) {
                map.resolved = true;
            }
            return map;
        }
    }

    @Override
    public List<Transaction> parse(List<String[]> rows, String bankCode, String cardTypeCode, String cardNo) {
        List<Transaction> list = new ArrayList<>();
        ColumnMap columns = new ColumnMap();
        boolean inTable = false;

        for (String[] row : rows) {
            if (row == null || row.length == 0) {
                continue;
            }

            if (isHeaderRow(row)) {
                columns = ColumnMap.fromHeader(row);
                inTable = true;
                continue;
            }

            if (!inTable) {
                if (looksLikeAbcDataRow(row)) {
                    inTable = true;
                } else {
                    continue;
                }
            }

            String txnDate = CcbTransactionStatementImporter.normalizeDateDigits(cell(row, columns.txnDate));
            String postDate = CcbTransactionStatementImporter.normalizeDateDigits(cell(row, columns.postDate));

            if (!DATE8.matcher(txnDate).matches()) {
                continue;
            }
            if (!DATE8.matcher(postDate).matches()) {
                postDate = txnDate;
            }

            String amountRaw = firstNonBlank(
                    cell(row, columns.postAmount),
                    cell(row, columns.txnAmount));
            SignedAmount signed = parseSignedAmount(amountRaw);
            if (signed == null || signed.magnitude() <= 0) {
                continue;
            }

            String summary = cell(row, columns.summary);
            String location = cell(row, columns.location);
            String desc = buildDescription(summary, location);
            if (StringUtils.isBlank(desc)) {
                continue;
            }

            String cardTail = cleanCardTail(cell(row, columns.cardTail));
            String card = StringUtils.isNotBlank(cardNo)
                    ? StringTool.cleanStr(cardNo)
                    : cardTail;

            Transaction transaction = new Transaction();
            transaction.setId(StringTool.generateID());
            transaction.setCardId(card);
            try {
                transaction.setTransactionDate(DateTool.changeStringToDate(txnDate, DateTool.DF_YYYYMMDD));
                transaction.setBookKeepingDate(DateTool.changeStringToDate(postDate, DateTool.DF_YYYYMMDD));
            } catch (Exception e) {
                continue;
            }

            transaction.setTransactionDesc(desc);
            transaction.setBalanceCurrency(StringUtils.defaultIfBlank(signed.currency(), "CNY"));
            applyFlowAmounts(transaction, signed, summary);
            TransactionAmountNormalizer.normalize(transaction);
            applyCardType(transaction, cardTypeCode);
            list.add(transaction);
        }
        return list;
    }

    private static boolean isHeaderRow(String[] row) {
        String joined = StringUtils.join(row, "");
        return (joined.contains("交易日期") || joined.contains("交易日"))
                && (joined.contains("入账金额") || joined.contains("交易金额"));
    }

    private static boolean looksLikeAbcDataRow(String[] row) {
        if (row.length < 4) {
            return false;
        }
        String d0 = CcbTransactionStatementImporter.normalizeDateDigits(cell(row, 0));
        String d1 = row.length > 1
                ? CcbTransactionStatementImporter.normalizeDateDigits(cell(row, 1))
                : "";
        if (!DATE8.matcher(d0).matches()) {
            return false;
        }
        if (!DATE8.matcher(d1).matches()) {
            return false;
        }
        for (int i = 2; i < row.length; i++) {
            if (parseSignedAmount(cell(row, i)) != null) {
                return true;
            }
        }
        return false;
    }

    public static SignedAmount parseSignedAmount(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String cleaned = StringTool.cleanStr(raw).replace("'", "").trim();
        java.util.regex.Matcher m = AMOUNT_WITH_CURRENCY.matcher(cleaned);
        if (m.matches()) {
            try {
                double value = Double.parseDouble(m.group(1));
                value = scaleFenIfNeeded(raw, value);
                String currency = normalizeCurrency(m.group(2));
                return SignedAmount.of(value, currency);
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        String digits = cleaned.replaceAll("[^0-9\\-\\.]", "");
        if (StringUtils.isBlank(digits)) {
            return null;
        }
        try {
            double value = Double.parseDouble(digits);
            value = scaleFenIfNeeded(raw, value);
            return SignedAmount.of(value, "CNY");
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** ABC sometimes exports yuan as integer fen when no decimal point is present. */
    private static double scaleFenIfNeeded(String raw, double value) {
        if (raw == null || raw.contains(".")) {
            return value;
        }
        if (Math.abs(value) >= 10_000) {
            return value / 100.0;
        }
        return value;
    }

    private static void applyFlowAmounts(Transaction transaction, SignedAmount signed, String summary) {
        double mag = signed.magnitude();
        String kind = flowKind(summary);
        if (signed.expense()) {
            transaction.setBalanceMoney(mag);
            transaction.setIncomeMoney(0.0);
        } else {
            transaction.setIncomeMoney(mag);
            transaction.setBalanceMoney(0.0);
        }
        transaction.setTxnKind(kind);
    }

    /** expense = consumption; finance = transfer / card repayment (not lifestyle income). */
    private static String flowKind(String summary) {
        String s = StringUtils.trimToEmpty(summary);
        if (s.contains("还款")) {
            return "finance";
        }
        if (s.contains("转账") || s.contains("转入") || s.contains("转出")) {
            return "finance";
        }
        if (s.contains("消费") || s.contains("购物") || s.contains("商户")) {
            return "expense";
        }
        return "expense";
    }

    private static String normalizeCurrency(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "CNY";
        }
        String c = raw.trim().toUpperCase();
        if ("RMB".equals(c) || "人民币".equals(raw.trim())) {
            return "CNY";
        }
        return c;
    }

    private static String buildDescription(String summary, String location) {
        String s = StringUtils.trimToEmpty(summary);
        String l = StringUtils.trimToEmpty(location);
        if (StringUtils.isNotBlank(s) && StringUtils.isNotBlank(l)) {
            return s + " · " + l;
        }
        return StringUtils.defaultIfBlank(s, l);
    }

    private static String cleanCardTail(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        return raw.replace("'", "").replaceAll("\\D", "");
    }

    private static String cell(String[] row, int idx) {
        if (idx < 0 || idx >= row.length || row[idx] == null) {
            return "";
        }
        return StringTool.cleanStr(row[idx]);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.isNotBlank(v)) {
                return v;
            }
        }
        return "";
    }

    private static void applyCardType(Transaction transaction, String cardTypeCode) {
        if ("debit".equalsIgnoreCase(cardTypeCode)) {
            transaction.setCardTypeId(2);
            transaction.setCardTypeName("储蓄卡");
            return;
        }
        transaction.setCardTypeId(1);
        transaction.setCardTypeName("信用卡");
    }

    record SignedAmount(double signedValue, String currency) {
        static SignedAmount of(double value, String currency) {
            return new SignedAmount(value, currency);
        }

        double magnitude() {
            return Math.abs(signedValue);
        }

        boolean expense() {
            return signedValue < 0;
        }
    }
}
