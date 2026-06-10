package com.finsight.application.importer.impl;

import com.finsight.common.util.DateTool;
import com.finsight.common.util.StringTool;
import com.finsight.domain.model.Transaction;
import com.finsight.application.importer.StatementImporter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CcbTransactionStatementImporter implements StatementImporter {
    private static final Pattern DATE8 = Pattern.compile("^\\d{8}$");
    private static final Pattern CARD = Pattern.compile("^\\d{12,19}$");

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
                if (StringUtils.contains(joined, "交易日") && StringUtils.contains(joined, "入账金额")) {
                    inTable = true;
                }
                continue;
            }
            String c0 = StringTool.cleanStr(row[0]).replaceAll("\\D", "");
            String c1 = StringTool.cleanStr(row[1]).replaceAll("\\D", "");
            String c2 = StringTool.cleanStr(row[2]).replace("'", "");
            String c3 = StringTool.cleanStr(row[3]);
            String c4 = StringTool.cleanStr(row[4]);
            String c5 = StringTool.cleanStr(row[5]);
            String c6 = StringTool.cleanStr(row[6]);

            if (!inTable) {
                if (StringUtils.equalsAny(c0, "交易日")) {
                    inTable = true;
                }
                continue;
            }

            if (!DATE8.matcher(c0).matches()) {
                continue;
            }
            if (!DATE8.matcher(c1).matches()) {
                continue;
            }
            String card = StringUtils.isNotBlank(cardNo) ? StringTool.cleanStr(cardNo) : c2;
            if (StringUtils.isBlank(c3)) {
                continue;
            }
            if (StringUtils.isBlank(c4)) {
                continue;
            }
            String amt = c5 == null ? "" : c5.replaceAll("[^0-9\\-\\.]", "");
            Double amount;
            try { amount = Double.valueOf(amt); } catch (Exception ex) { continue; }
            if (amount == null) {
                continue;
            }

            Transaction transaction = new Transaction();
            transaction.setId(StringTool.generateID());
            transaction.setCardId(card);
            try {
                transaction.setTransactionDate(DateTool.changeStringToDate(c0, DateTool.DF_YYYYMMDD));
                transaction.setBookKeepingDate(DateTool.changeStringToDate(c1, DateTool.DF_YYYYMMDD));
            } catch (Exception e) {
                continue;
            }
            transaction.setTransactionDesc(c6);
            transaction.setBalanceCurrency(c4);
            transaction.setBalanceMoney(amount);
            transaction.setCardTypeId(1);
            transaction.setCardTypeName("信用卡");
            list.add(transaction);
        }
        return list;
    }
}
