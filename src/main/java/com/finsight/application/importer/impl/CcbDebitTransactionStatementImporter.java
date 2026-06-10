package com.finsight.application.importer.impl;

import com.finsight.common.exception.DateParseException;
import com.finsight.common.util.DateTool;
import com.finsight.common.util.StringTool;
import com.finsight.domain.model.Transaction;
import com.finsight.application.importer.StatementImporter;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CcbDebitTransactionStatementImporter implements StatementImporter {
    private static final Pattern DATE8 = Pattern.compile("^\\d{8}$");
    @Override
    public List<Transaction> parse(List<String[]> rows, String bankCode, String cardTypeCode, String cardNo) {
        List<Transaction> list = new ArrayList<>();
        boolean inTable = false;
        for (String[] row : rows) {
            if (row == null) {
                continue;
            }
            if (row.length < 7) {
                String joined = StringUtils.join(row, "");
                if (StringUtils.contains(joined, "交易日") &&
                        (StringUtils.contains(joined, "入账金额")
                                || StringUtils.contains(joined, "交易金额")
                                || StringUtils.contains(joined, "金额"))) {
                    inTable = true;
                }
                continue;
            }
            String originC0 = StringTool.cleanStr(row[0]);
            String c0 = StringTool.cleanStr(row[0]).replaceAll("\\D", ""); // Booking Date
            String c1 = StringTool.cleanStr(row[1]).replaceAll("\\D", ""); // Transaction Date
            String c2 = StringTool.cleanStr(row[2]); // Transaction Time
            String c3 = StringTool.cleanStr(row[3]); // Expense
            String c4 = StringTool.cleanStr(row[4]); // Income
            String c5 = StringTool.cleanStr(row[5]); // Balance
            String c6 = row.length > 6 ? StringTool.cleanStr(row[6]) : "人民币"; // Currency
            String c7 = row.length > 7 ? StringTool.cleanStr(row[7]) : ""; // Desc
            String c8 = row.length > 8 ? StringTool.cleanStr(row[8]) : ""; // Opponent Account
            String c9 = row.length > 9 ? StringTool.cleanStr(row[9]) : ""; // Opponent Name
            String c10 = row.length > 10 ? StringTool.cleanStr(row[10]) : ""; // Location

            if (!inTable) {
                if (StringUtils.equalsAny(originC0, "交易日", "交易日期")) {
                    inTable = true;
                    continue;
                }
                String joined = StringUtils.join(row, "");
                if (StringUtils.contains(joined, "交易日")
                        && (StringUtils.contains(joined, "入账金额")
                                || StringUtils.contains(joined, "交易金额")
                                || StringUtils.contains(joined, "金额")
                                || StringUtils.contains(joined, "收入")
                                || StringUtils.contains(joined, "支出")
                                || StringUtils.contains(joined, "交易时间"))) {
                    inTable = true;
                    continue;
                }
                if (DATE8.matcher(c0).matches() && DATE8.matcher(c1).matches()) {
                    inTable = true;
                    // fall through to parse data row
                } else {
                    continue;
                }
            }

            if (!DATE8.matcher(c0).matches()) {
                continue;
            }
            if (!DATE8.matcher(c1).matches()) {
                continue;
            }

            // Calculate Amount
            Double expense = parseDouble(c3);
            Double income = parseDouble(c4);
            Double amount = 0.0;
            if (income != null && income > 0) {
                amount = income;
            } else if (expense != null && expense > 0) {
                amount = -expense;
            }

            String card = StringUtils.isNotBlank(cardNo) ? StringTool.cleanStr(cardNo) : "";

            Transaction transaction = new Transaction();
            transaction.setId(StringTool.generateID());
            transaction.setCardId(card);
            try {
                // c1 is Transaction Date, c0 is Booking Date
                transaction.setTransactionDate(DateTool.changeStringToDate(c1, DateTool.DF_YYYYMMDD));
                transaction.setBookKeepingDate(DateTool.changeStringToDate(c0, DateTool.DF_YYYYMMDD));
            } catch (DateParseException e) {
                continue;
            }
            String dateStr = "";
            try {
                dateStr = new SimpleDateFormat("yyyy-MM-dd").format(transaction.getTransactionDate());
            } catch (Exception ignore) {
                // ignore
            }
            if (StringUtils.isNotBlank(c2)) {
                transaction.setTransactionDateTime((dateStr == null ? "" : dateStr) + " " + c2);
            } else {
                transaction.setTransactionDateTime(dateStr);
            }
            // Merge columns into description with @@ separator
            List<String> descParts = new ArrayList<>();
            if (StringUtils.isNotBlank(c7)) descParts.add(c7);
            if (StringUtils.isNotBlank(c8)) descParts.add(c8);
            if (StringUtils.isNotBlank(c9)) descParts.add(c9);
            if (StringUtils.isNotBlank(c10)) descParts.add(c10);
            transaction.setTransactionDesc(StringUtils.join(descParts, "@@"));

            transaction.setBalanceCurrency(c6);
            transaction.setBalanceMoney(amount);
            transaction.setAccountBalance(parseDouble(c5));
            
            // New Fields (Keep separate fields for structured access if needed)
            transaction.setIncomeMoney(income);
            transaction.setOpponentAccount(c8);
            transaction.setOpponentName(c9);
            transaction.setTransactionTime(c2); // Actual Transaction Time
            
            transaction.setCardTypeId(2); // 1 for Credit, 2 for Debit
            transaction.setCardTypeName("储蓄卡");
            list.add(transaction);
        }
        return list;
    }

    private Double parseDouble(String str) {
        if (StringUtils.isBlank(str)) return 0.0;
        try {
            return Double.valueOf(str.replaceAll("[^0-9\\-\\.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }
}
