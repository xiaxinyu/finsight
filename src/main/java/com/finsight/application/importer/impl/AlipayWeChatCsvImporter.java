package com.finsight.application.importer.impl;

import com.finsight.application.importer.StatementImporter;
import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Parses common Alipay / WeChat Pay CSV export layouts (Chinese headers).
 */
public class AlipayWeChatCsvImporter implements StatementImporter {

    @Override
    public List<Transaction> parse(List<String[]> rows, String bankCode, String cardTypeCode, String cardNo) {
        List<Transaction> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        int headerRow = findHeader(rows);
        if (headerRow < 0) {
            return out;
        }
        String[] header = rows.get(headerRow);
        int dateIdx = indexOf(header, "交易时间", "交易创建时间", "时间");
        int amountIdx = indexOf(header, "金额", "金额(元)", "收入/支出");
        int descIdx = indexOf(header, "商品说明", "交易对方", "备注", "名称");
        int typeIdx = indexOf(header, "收/支", "收支");

        for (int i = headerRow + 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row == null || row.length == 0) {
                continue;
            }
            Transaction t = new Transaction();
            t.setId(UUID.randomUUID().toString());
            t.setTransactionDate(parseDate(safe(row, dateIdx)));
            t.setTransactionDesc(safe(row, descIdx));
            double amount = parseAmount(safe(row, amountIdx));
            String io = safe(row, typeIdx);
            if (io.contains("支") || io.startsWith("-") || amount < 0) {
                t.setBalanceMoney(Math.abs(amount));
            } else {
                t.setIncomeMoney(Math.abs(amount));
            }
            t.setBalanceCurrency("CNY");
            t.setBankCardName(bankCode);
            out.add(t);
        }
        return out;
    }

    private static int findHeader(List<String[]> rows) {
        for (int i = 0; i < Math.min(rows.size(), 20); i++) {
            String line = String.join(",", rows.get(i));
            if (line.contains("交易时间") || line.contains("金额") || line.contains("收/支")) {
                return i;
            }
        }
        return rows.size() > 1 ? 0 : -1;
    }

    private static int indexOf(String[] header, String... names) {
        for (int i = 0; i < header.length; i++) {
            for (String n : names) {
                if (header[i] != null && header[i].contains(n)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String safe(String[] row, int idx) {
        if (idx < 0 || idx >= row.length) {
            return "";
        }
        return row[idx] == null ? "" : row[idx].trim();
    }

    private static double parseAmount(String raw) {
        if (StringUtils.isBlank(raw)) {
            return 0;
        }
        String s = raw.replace("¥", "").replace(",", "").replace("元", "").trim();
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Date parseDate(String raw) {
        if (StringUtils.isBlank(raw)) {
            return new Date();
        }
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd"};
        for (String p : patterns) {
            try {
                return new SimpleDateFormat(p).parse(raw.trim());
            } catch (Exception ignored) {
            }
        }
        return new Date();
    }
}
