package com.finsight.application.importer.impl;

import com.finsight.core.DateTool;
import com.finsight.core.StringTool;
import com.finsight.domain.model.Transaction;
import com.finsight.application.importer.StatementImporter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 华润银行储蓄卡账单导入
 */
public class CrbankDebitTransactionStatementImporter implements StatementImporter {
    private static final Logger log = LoggerFactory.getLogger(CrbankDebitTransactionStatementImporter.class);
    // 2025-10-29 or 20251029
    private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}|\\d{8})$");

    @Override
    public List<Transaction> parse(List<String[]> rows, String bankCode, String cardTypeCode, String cardNo) {
        List<Transaction> list = new ArrayList<>();
        boolean inTable = false;
        int nonDateSkipped = 0;
        
        // Column Indices (match CRBank debit layout as default)
        int dateIdx = -1;
        int amountIdx = -1;       // 支出
        int incomeIdx = -1;       // 存入
        int balanceIdx = -1;      // 余额
        int summaryIdx = -1;      // 摘要
        int opponentAccIdx = -1;  // 对方账号
        int opponentNameIdx = -1; // 对方户名
        int locationIdx = -1;     // 交易地点

        if (rows == null || rows.isEmpty()) {
            log.info("CRBANK importer received empty rows, bankCode={}, cardTypeCode={}", bankCode, cardTypeCode);
            return list;
        }

        log.info("CRBANK importer start: rows={}, bankCode={}, cardTypeCode={}, cardNo={}",
                rows.size(), bankCode, cardTypeCode, cardNo);

        for (String[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }

            String joined = StringUtils.join(row, "");
            
            // 1. Detect Header / Fallback indices
            if (!inTable) {
                boolean hasDate = StringUtils.contains(joined, "交易日期") || StringUtils.contains(joined, "交易日") || StringUtils.contains(joined, "日期");
                boolean hasMoney = StringUtils.contains(joined, "支出") || StringUtils.contains(joined, "交易金额") || StringUtils.contains(joined, "借方") || StringUtils.contains(joined, "收入") || StringUtils.contains(joined, "存入") || StringUtils.contains(joined, "贷方") || StringUtils.contains(joined, "余额");
                boolean hasOpponent = StringUtils.contains(joined, "对方") || StringUtils.contains(joined, "交易地点") || StringUtils.contains(joined, "摘要");

                // Relaxed condition: match any 2 of Date/Money/Opponent, or just Summary+Opponent
                if ((hasDate && hasMoney) || (hasOpponent && hasMoney) || (hasOpponent && hasDate) || (joined.contains("摘要") && joined.contains("对方"))) {

                    for (int i = 0; i < row.length; i++) {
                        String col = StringTool.cleanStr(row[i]);
                        if (col.contains("交易日期") || col.contains("交易日") || col.equals("日期")) dateIdx = i;
                        else if (col.contains("支出") || col.equals("交易金额") || col.contains("借方")) amountIdx = i;
                        else if (col.contains("存入") || col.contains("收入") || col.contains("贷方")) incomeIdx = i;
                        else if (col.contains("余额")) balanceIdx = i;
                        else if (col.contains("摘要") || col.contains("用途") || col.contains("备注")) summaryIdx = i;
                        else if ((col.contains("对方") && (col.contains("账号") || col.contains("账户") || col.contains("帐号")))) opponentAccIdx = i;
                        else if ((col.contains("对方") && (col.contains("户名") || col.contains("名称") || col.contains("姓名")))) opponentNameIdx = i;
                        else if (col.contains("交易地点") || col.contains("交易网点") || col.contains("网点") || col.contains("地点")) locationIdx = i;
                    }

                    // Fallback to standard CRBank layout if some columns are still unknown
                    // Layout guess (observed): 0:日期 1:渠道 2:收入 3:支出 4:账户余额 5:摘要 6:对方户名 7:对方账号 8:交易地点
                    if (row.length >= 6) {
                        if (dateIdx == -1) dateIdx = 0;
                        if (incomeIdx == -1 && row.length >= 3) incomeIdx = 2;
                        if (amountIdx == -1 && row.length >= 4) amountIdx = 3;
                        if (balanceIdx == -1 && row.length >= 5) balanceIdx = 4;
                        if (summaryIdx == -1 && row.length >= 6) summaryIdx = 5;
                        if (opponentNameIdx == -1 && row.length >= 7) opponentNameIdx = 6;
                        if (opponentAccIdx == -1 && row.length >= 8) opponentAccIdx = 7;
                        if (locationIdx == -1 && row.length >= 9) locationIdx = 8;
                    }

                    inTable = true;
                    log.info("CRBANK header detected, indices: date={}, amount={}, income={}, balance={}, summary={}, oppAcc={}, oppName={}, location={}",
                            dateIdx, amountIdx, incomeIdx, balanceIdx, summaryIdx, opponentAccIdx, opponentNameIdx, locationIdx);
                    continue;
                }

                // Fallback: no explicit header row, but data may start directly.
                int firstIdx = -1;
                String firstVal = "";
                for (int i = 0; i < row.length; i++) {
                    String v = StringTool.cleanStr(row[i]);
                    if (StringUtils.isNotBlank(v)) {
                        firstIdx = i;
                        firstVal = v;
                        break;
                    }
                }
                if (firstIdx >= 0 && DATE_PATTERN.matcher(firstVal).matches()) {
                    if (dateIdx == -1) {
                        dateIdx = firstIdx;
                        // Heuristic mapping to common CRBank layout when header row is absent
                        if (incomeIdx == -1 && row.length > firstIdx + 2) incomeIdx = firstIdx + 2;
                        if (amountIdx == -1 && row.length > firstIdx + 3) amountIdx = firstIdx + 3;
                        if (balanceIdx == -1 && row.length > firstIdx + 4) balanceIdx = firstIdx + 4;
                        if (summaryIdx == -1 && row.length > firstIdx + 5) summaryIdx = firstIdx + 5;
                        if (opponentNameIdx == -1 && row.length > firstIdx + 6) opponentNameIdx = firstIdx + 6;
                        if (opponentAccIdx == -1 && row.length > firstIdx + 7) opponentAccIdx = firstIdx + 7;
                        if (locationIdx == -1 && row.length > firstIdx + 8) locationIdx = firstIdx + 8;
                    }
                    inTable = true;
                    log.info("CRBANK fallback header applied, indices: date={}, income={}, amount={}, balance={}, summary={}, oppName={}, oppAcc={}, location={}",
                            dateIdx, incomeIdx, amountIdx, balanceIdx, summaryIdx, opponentNameIdx, opponentAccIdx, locationIdx);
                }
                if (!inTable) {
                    continue;
                }
            }

            // 2. Parse Data Row
            if (dateIdx == -1) { 
                continue; 
            }

            String cDate = getDateValue(row, dateIdx);
            if (!DATE_PATTERN.matcher(cDate).matches()) {
                if (nonDateSkipped < 5) {
                    log.info("CRBANK skip non-date row sample: dateRaw='{}', row='{}'", cDate, joined);
                }
                nonDateSkipped++;
                continue;
            }

            Transaction transaction = new Transaction();
            transaction.setId(StringTool.generateID());
            transaction.setCardId(StringUtils.isNotBlank(cardNo) ? StringTool.cleanStr(cardNo) : "");
            
            try {
                if(cDate.length() == 8) {
                    transaction.setTransactionDate(DateTool.changeStringToDate(cDate, DateTool.DF_YYYYMMDD));
                } else {
                    transaction.setTransactionDate(DateTool.changeStringToDate(cDate, DateTool.DF_YYYY_MM_DD));
                }
                transaction.setBookKeepingDate(transaction.getTransactionDate());
            } catch (Exception e) {
                log.warn("CRBANK parse date failed, raw='{}'", cDate, e);
                continue;
            }

            // Amount / Income / Balance Mapping
            Double expense = amountIdx != -1 ? parseDouble(getValue(row, amountIdx)) : 0.0;
            Double income = incomeIdx != -1 ? parseDouble(getValue(row, incomeIdx)) : 0.0;
            Double balance = balanceIdx != -1 ? parseDouble(getValue(row, balanceIdx)) : 0.0;

            // 预览表中：支出 -> balanceMoney, 存入 -> incomeMoney, 余额 -> accountBalance
            transaction.setBalanceMoney(expense != null ? expense : 0.0);
            transaction.setIncomeMoney(income != null ? income : 0.0);
            transaction.setAccountBalance(balance != null ? balance : 0.0);
            transaction.setBalanceCurrency("CNY");

            // Description: Summary + @@ + Location
            String summary = getValue(row, summaryIdx);
            String location = getValue(row, locationIdx);
            String desc = summary;
            if (StringUtils.isNotBlank(location)) {
                desc = desc + "@@" + location;
            }
            transaction.setTransactionDesc(desc);

            // Opponent
            String oppAccRaw = getValue(row, opponentAccIdx);
            String oppNameRaw = getValue(row, opponentNameIdx);
            if (isProbableMoney(oppAccRaw) || (!isProbableAccountNo(oppAccRaw))) {
                // 如果像金额或不像账号，就不要误填
                oppAccRaw = "";
            }
            if (isProbableNumeric(oppNameRaw) || isProbableMoney(oppNameRaw)) {
                // 名称不应是纯数字或金额
                oppNameRaw = "";
            }
            transaction.setOpponentAccount(oppAccRaw);
            transaction.setOpponentName(oppNameRaw);
            
            transaction.setCardTypeId(2);
            transaction.setCardTypeName("储蓄卡");
            list.add(transaction);
        }
        log.info("CRBANK importer finished, parsed transactions={}", list.size());
        return list;
    }

    private String getValue(String[] row, int index) {
        if (index >= 0 && index < row.length) {
            return StringTool.cleanStr(row[index]);
        }
        return "";
    }
    
    private String getDateValue(String[] row, int index) {
        if (index >= 0 && index < row.length) {
            return StringTool.cleanStr(row[index]);
        }
        return "";
    }

    private Double parseDouble(String str) {
        if (StringUtils.isBlank(str)) return 0.0;
        try {
            // Remove commas and other non-numeric chars except . and -
            return Double.valueOf(str.replaceAll("[^0-9\\-\\.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private boolean isProbableMoney(String v){
        if(StringUtils.isBlank(v)) return false;
        String s = v.trim();
        return s.contains(".") || s.contains(",");
    }
    private boolean isProbableNumeric(String v){
        if(StringUtils.isBlank(v)) return false;
        return v.trim().matches("^[-+]?\\d+(?:[.,]\\d+)?$");
    }
    private boolean isProbableAccountNo(String v){
        if(StringUtils.isBlank(v)) return false;
        String digits = v.replaceAll("\\D", "");
        return digits.length() >= 10; // most bank account/card numbers >= 10 digits
    }
}
