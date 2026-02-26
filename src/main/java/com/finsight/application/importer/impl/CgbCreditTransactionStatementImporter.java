package com.finsight.application.importer.impl;

import com.finsight.core.DateTool;
import com.finsight.core.StringTool;
import com.finsight.domain.model.Transaction;
import com.finsight.application.importer.StatementImporter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CgbCreditTransactionStatementImporter implements StatementImporter {

    @Override
    public List<Transaction> parse(List<String[]> rows, String bankCode, String cardTypeCode, String cardNo) {
        List<Transaction> list = new ArrayList<>();
        
        // Default column indices based on user description
        // Date, Desc, Currency, Amount, CardNo
        int dateIdx = 0;
        int descIdx = 1;
        int currencyIdx = 2;
        int amountIdx = 3;
        int cardIdx = 4;
        
        // Heuristic to detect if we are in the data section or if columns are shifted
        boolean headerFound = false;

        for (String[] row : rows) {
            if (row == null || row.length < 2) continue;
            
            // Attempt to find header row to adjust indices
            if (!headerFound) {
                boolean isHeader = false;
                // Temporary indices to verify if it's a header line
                int d = -1, de = -1, a = -1;
                
                for (int i = 0; i < row.length; i++) {
                    String val = StringUtils.trimToEmpty(row[i]);
                    if (val.contains("交易日期") || val.contains("日期")) { dateIdx = i; d = i; isHeader = true; }
                    else if (val.contains("交易描述") || val.contains("摘要") || val.contains("说明")) { descIdx = i; de = i; isHeader = true; }
                    else if (val.contains("金额") || val.contains("交易金额") || val.contains("入账金额")) { amountIdx = i; a = i; isHeader = true; }
                    else if (val.contains("币种")) { currencyIdx = i; isHeader = true; }
                    else if (val.contains("卡号") || val.contains("末四位")) { cardIdx = i; isHeader = true; }
                }
                
                // Only consider it a header if we found at least date and amount/desc
                if (isHeader && d != -1 && (a != -1 || de != -1)) {
                    headerFound = true;
                    continue; // Skip header
                }
            }

            // Check if this row looks like data (starts with date at dateIdx)
            String dateStr = safeGet(row, dateIdx);
            // 2021/05/07 or 2021-05-07 or 2026/02/06 19:13:34
            if (!dateStr.matches("^\\d{4}[/\\-]\\d{2}[/\\-]\\d{2}(?:\\s+\\d{1,2}:\\d{2}(?::\\d{2})?)?$")) {
                continue;
            }
            
            try {
                Transaction t = new Transaction();
                t.setId(StringTool.generateID());
                
                // Date
                if (dateStr.contains(":")) {
                    t.setTransactionDate(DateTool.changeStringToDate(dateStr, null)); // Defaults to yyyy-MM-dd HH:mm:ss
                } else {
                    t.setTransactionDate(DateTool.changeStringToDate(dateStr, DateTool.DF_YYYY_MM_DD));
                }
                // Set bookkeeping date same as transaction date if not available
                t.setBookKeepingDate(t.getTransactionDate());
                
                // Description
                t.setTransactionDesc(safeGet(row, descIdx));
                
                // Currency
                String curr = safeGet(row, currencyIdx);
                t.setBalanceCurrency(StringUtils.isBlank(curr) ? "CNY" : curr);
                
                // Amount
                String amtStr = safeGet(row, amountIdx).replaceAll("[^0-9\\-\\.]", "");
                if (StringUtils.isNotBlank(amtStr)) {
                    t.setBalanceMoney(Double.valueOf(amtStr));
                }
                
                // Card
                String cNo = safeGet(row, cardIdx);
                // Clean card number (remove non-digits)
                if (StringUtils.isNotBlank(cNo)) {
                    cNo = cNo.replaceAll("\\D", "");
                }
                
                if (StringUtils.isNotBlank(cardNo)) {
                    t.setCardId(cardNo);
                } else if (StringUtils.isNotBlank(cNo)) {
                    t.setCardId(cNo);
                }
                
                t.setCardTypeId(1); // Credit
                t.setCardTypeName("信用卡");
                
                list.add(t);
            } catch (Exception e) {
                // Ignore malformed rows
                e.printStackTrace();
            }
        }
        return list;
    }
    
    private String safeGet(String[] row, int idx) {
        if (row != null && idx >= 0 && idx < row.length) {
            return StringUtils.trimToEmpty(row[idx]);
        }
        return "";
    }
}
