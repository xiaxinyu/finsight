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

public class CmbDebitTransactionStatementImporter implements StatementImporter {
    private static final Logger log = LoggerFactory.getLogger(CmbDebitTransactionStatementImporter.class);
    private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}|\\d{8})$");

    @Override
    public List<Transaction> parse(List<String[]> rows, String bankCode, String cardTypeCode, String cardNo) {
        List<Transaction> list = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {
            return list;
        }
        
        log.info("Starting CMB parsing, total rows: {}", rows.size());
        int summaryCol = -1;

        for (String[] row : rows) {
            if (row == null || row.length == 0) continue;
            boolean headerDetected = false;
            if (summaryCol == -1) {
                for (int i = 0; i < row.length; i++) {
                    String col = StringTool.cleanStr(row[i]);
                    String lower = col.toLowerCase();
                    if (col.contains("交易摘要") || lower.contains("transaction type")) {
                        summaryCol = i;
                        headerDetected = true;
                        break;
                    }
                }
                if (headerDetected) {
                    continue;
                }
            }
            String line = StringUtils.join(row, " ").trim();
            // 预处理：确保日期和CNY前后有空格，避免粘连
            line = line.replaceAll("(\\d{4}-\\d{2}-\\d{2})", " $1 ");
            line = line.replaceAll("(?i)(CNY|人民币)", " $1 ");
            // 去除千分位逗号
            line = line.replaceAll("(\\d),(\\d{3})", "$1$2"); 
            line = line.replaceAll("\\s+", " ").trim();
            
            String[] tokens = line.split(" ");
            if (tokens.length < 3) {
                log.warn("Line skipped (too short): {}", line);
                continue;
            }

            int postingIdx = -1, txnIdx = -1;
            for (int i = 0; i < tokens.length; i++) {
                String tk = tokens[i];
                if (tk.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                    if (postingIdx == -1) postingIdx = i;
                    else if (txnIdx == -1) { txnIdx = i; break; }
                }
            }
            
            // 宽松策略：只要找到一个日期即可
            if (postingIdx == -1) {
                log.warn("Line skipped (no date found): {}", line);
                continue;
            }
            if (txnIdx == -1) txnIdx = postingIdx; // 如果只有一个日期，视为同一个

            // 从日期之后开始解析
            // 策略：寻找金额字段（带有小数点的数字），金额之前为摘要，金额之后为余额和对手信息
            int amountIdx = -1;
            int balanceIdx = -1;
            
            for (int i = txnIdx + 1; i < tokens.length; i++) {
                // 匹配金额：可能是负数，包含小数点
                if (tokens[i].matches("^[-+]?\\d+\\.\\d{2}$")) {
                    amountIdx = i;
                    // 检查下一个是否也是金额（余额）
                    if (i + 1 < tokens.length && tokens[i+1].matches("^[-+]?\\d+\\.\\d{2}$")) {
                        balanceIdx = i + 1;
                    }
                    break;
                }
            }
            
            // 如果没找到严格匹配 .xx 的金额，尝试找任何数字（排除CNY）
            if (amountIdx == -1) {
                 for (int i = txnIdx + 1; i < tokens.length; i++) {
                    if (tokens[i].matches("^[-+]?\\d+(\\.\\d+)?$") && !tokens[i].equalsIgnoreCase("CNY")) {
                         amountIdx = i;
                         if (i + 1 < tokens.length && tokens[i+1].matches("^[-+]?\\d+(\\.\\d+)?$")) {
                            balanceIdx = i + 1;
                        }
                        break;
                    }
                }
            }

            String narration = "";
            Double income = 0.0, expense = 0.0, balance = 0.0;
            String opponentInfo = "";

            if (amountIdx != -1) {
                // 摘要：日期之后，金额之前
                int start = txnIdx + 1;
                int end = amountIdx;
                List<String> narTokens = new ArrayList<>();
                for (int k = start; k < end; k++) {
                    // 跳过 CNY 标记
                    if (!tokens[k].equalsIgnoreCase("CNY") && !tokens[k].equals("人民币")) {
                        narTokens.add(tokens[k]);
                    }
                }
                narration = String.join(" ", narTokens);
                
                // 金额
                Double amt = parseDouble(tokens[amountIdx]);
                if (amt < 0) {
                    expense = Math.abs(amt);
                } else {
                    income = amt;
                }
                
                // 余额
                int nextIdx = amountIdx + 1;
                if (balanceIdx != -1) {
                    balance = parseDouble(tokens[balanceIdx]);
                    nextIdx = balanceIdx + 1;
                }
                
                // 对手信息：余额之后的所有内容
                if (nextIdx < tokens.length) {
                    opponentInfo = String.join(" ", java.util.Arrays.copyOfRange(tokens, nextIdx, tokens.length));
                }
            } else {
                // 没找到金额，暂时把剩下的当摘要
                narration = String.join(" ", java.util.Arrays.copyOfRange(tokens, txnIdx + 1, tokens.length)).replaceAll("(?i)CNY", "").trim();
            }

            String postingDateStr = tokens[postingIdx];
            String txnDateStr = tokens[txnIdx];

            String opponentAcc = "";
            String opponentName = "";
            if (StringUtils.isNotBlank(opponentInfo)) {
                String digits = opponentInfo.replaceAll("\\D", "");
                // 如果数字长度足够长，认为是账号
                if (digits.length() >= 10) {
                    opponentAcc = digits;
                    opponentName = opponentInfo.replace(digits, "").trim();
                } else {
                    opponentName = opponentInfo;
                }
            }

            Transaction t = new Transaction();
            t.setId(StringTool.generateID());
            t.setCardId(StringUtils.isNotBlank(cardNo) ? StringTool.cleanStr(cardNo) : "");
            try {
                t.setBookKeepingDate(DateTool.changeStringToDate(postingDateStr, DateTool.DF_YYYY_MM_DD));
                t.setTransactionDate(DateTool.changeStringToDate(txnDateStr, DateTool.DF_YYYY_MM_DD));
            } catch (Exception e) {
                log.warn("Date parse error: {} or {}", postingDateStr, txnDateStr);
                continue;
            }
            String summaryCandidate = getValue(row, summaryCol);
            String desc = org.apache.commons.lang3.StringUtils.isNotBlank(summaryCandidate)
                    ? summaryCandidate
                    : org.apache.commons.lang3.StringUtils.trimToEmpty(narration);
            if (org.apache.commons.lang3.StringUtils.isBlank(desc)) {
                if (org.apache.commons.lang3.StringUtils.isNotBlank(opponentName)) {
                    desc = opponentName;
                } else if (org.apache.commons.lang3.StringUtils.isNotBlank(opponentAcc)) {
                    desc = opponentAcc;
                }
            }
            t.setTransactionDesc(desc);
            t.setBalanceCurrency("CNY");
            t.setIncomeMoney(income == null ? 0.0 : income);
            t.setBalanceMoney(expense == null ? 0.0 : expense);
            t.setAccountBalance(balance == null ? 0.0 : balance);
            String dateStr = "";
            try { dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(t.getTransactionDate()); } catch (Exception ignore) {}
            t.setTransactionDateTime(dateStr);
            t.setOpponentAccount(opponentAcc);
            t.setOpponentName(opponentName);
            t.setCardTypeId(2);
            t.setCardTypeName("储蓄卡");
            list.add(t);
        }
        log.info("CMB importer finished, parsed transactions={}", list.size());
        return list;
    }

    private String getValue(String[] row, int index) {
        if (index >= 0 && index < row.length) {
            return StringTool.cleanStr(row[index]);
        }
        return "";
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
