package com.finsight.application.importer.impl;

import com.finsight.application.importer.StatementImporter;
import com.finsight.application.transaction.TransactionAmountNormalizer;
import com.finsight.common.util.DateTool;
import com.finsight.common.util.StringTool;
import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * CCB (China Construction Bank) credit card statement — Excel/CSV export with preamble rows
 * and columns: 交易日, 入账日, 信用卡卡号, 类型, 入账币种, 入账金额, 交易描述.
 */
public class CcbTransactionStatementImporter implements StatementImporter {

    private static final Pattern DATE8 = Pattern.compile("^\\d{8}$");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    private static final class ColumnMap {
        int txnDate = 0;
        int postDate = 1;
        int card = 2;
        int type = 3;
        int currency = 4;
        int amount = 5;
        int desc = 6;
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
                if (v.contains("交易描述") || v.contains("摘要")) {
                    map.desc = i;
                    map.resolved = true;
                } else if (v.contains("入账金额") || v.contains("交易金额")) {
                    map.amount = i;
                    sawAmount = true;
                    map.resolved = true;
                } else if (v.contains("入账币种") || v.equals("币种") || v.contains("货币")) {
                    map.currency = i;
                    map.resolved = true;
                } else if (v.contains("类型")) {
                    map.type = i;
                    map.resolved = true;
                } else if (v.contains("信用卡") || v.contains("卡号")) {
                    map.card = i;
                    map.resolved = true;
                } else if (v.contains("入账日")) {
                    map.postDate = i;
                    map.resolved = true;
                } else if (v.contains("交易日") || v.contains("交易日期")) {
                    map.txnDate = i;
                    sawTxnDate = true;
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
                if (looksLikeCcbCreditDataRow(row)) {
                    inTable = true;
                } else {
                    continue;
                }
            }

            String txnDateRaw = cell(row, columns.txnDate);
            String postDateRaw = cell(row, columns.postDate);
            String txnDate = normalizeDateDigits(txnDateRaw);
            String postDate = normalizeDateDigits(postDateRaw);

            if (!DATE8.matcher(txnDate).matches()) {
                continue;
            }
            if (!DATE8.matcher(postDate).matches()) {
                postDate = txnDate;
            }

            String type = cell(row, columns.type);
            String currency = cell(row, columns.currency);
            String amountRaw = cell(row, columns.amount);
            String desc = cell(row, columns.desc);

            if (StringUtils.isBlank(amountRaw) && StringUtils.isBlank(desc)) {
                continue;
            }

            Double amount = parseAmount(amountRaw);
            if (amount == null || amount <= 0) {
                continue;
            }

            String card = StringUtils.isNotBlank(cardNo)
                    ? StringTool.cleanStr(cardNo)
                    : cleanCard(cell(row, columns.card));

            Transaction transaction = new Transaction();
            transaction.setId(StringTool.generateID());
            transaction.setCardId(card);
            try {
                transaction.setTransactionDate(DateTool.changeStringToDate(txnDate, DateTool.DF_YYYYMMDD));
                transaction.setBookKeepingDate(DateTool.changeStringToDate(postDate, DateTool.DF_YYYYMMDD));
            } catch (Exception e) {
                continue;
            }

            transaction.setTransactionDesc(StringUtils.defaultIfBlank(desc, type));
            transaction.setBalanceCurrency(StringUtils.defaultIfBlank(currency, "人民币"));
            transaction.setBalanceMoney(amount);
            TransactionAmountNormalizer.normalize(transaction);
            transaction.setCardTypeId(1);
            transaction.setCardTypeName("信用卡");
            list.add(transaction);
        }
        return list;
    }

    private static boolean isHeaderRow(String[] row) {
        String joined = StringUtils.join(row, "");
        return (joined.contains("交易日") || joined.contains("交易日期"))
                && (joined.contains("入账金额") || joined.contains("交易金额"));
    }

    private static boolean looksLikeCcbCreditDataRow(String[] row) {
        if (row.length < 4) {
            return false;
        }
        String d0 = normalizeDateDigits(cell(row, 0));
        String d1 = normalizeDateDigits(cell(row, 1));
        if (!DATE8.matcher(d0).matches()) {
            return false;
        }
        if (!DATE8.matcher(d1).matches()) {
            return false;
        }
        for (int i = 2; i < row.length; i++) {
            if (parseAmount(cell(row, i)) != null) {
                return true;
            }
        }
        return false;
    }

    static String normalizeDateDigits(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        String cleaned = StringTool.cleanStr(raw).replace("'", "").trim();
        if (cleaned.matches("^\\d+(\\.\\d+)?$")) {
            try {
                double serial = Double.parseDouble(cleaned);
                if (serial > 30_000 && serial < 100_000) {
                    LocalDate date = LocalDate.of(1899, 12, 30).plusDays((long) serial);
                    return date.format(YMD);
                }
            } catch (NumberFormatException ignore) {
                // fall through
            }
        }
        cleaned = cleaned.replace('/', '-').replace('.', '-');
        if (cleaned.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return cleaned.replace("-", "");
        }
        String digits = cleaned.replaceAll("\\D", "");
        if (digits.length() >= 8) {
            return digits.substring(0, 8);
        }
        return digits;
    }

    private static String cell(String[] row, int idx) {
        if (idx < 0 || idx >= row.length || row[idx] == null) {
            return "";
        }
        return StringTool.cleanStr(row[idx]);
    }

    private static String cleanCard(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        return raw.replace("'", "").replaceAll("\\D", "");
    }

    private static Double parseAmount(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String amt = raw.replaceAll("[^0-9\\-\\.]", "");
        if (StringUtils.isBlank(amt)) {
            return null;
        }
        try {
            return Double.valueOf(amt);
        } catch (Exception ex) {
            return null;
        }
    }
}
