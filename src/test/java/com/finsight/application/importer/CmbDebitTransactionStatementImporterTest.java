package com.finsight.application.importer;

import com.finsight.application.importer.impl.CmbDebitTransactionStatementImporter;
import com.finsight.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CmbDebitTransactionStatementImporterTest {

    private final CmbDebitTransactionStatementImporter importer = new CmbDebitTransactionStatementImporter();

    @Test
    void parsesSignedAmountWithDotDateAndComma() {
        List<String[]> rows = List.of(
                new String[]{"记账日期", "币种", "交易金额", "联机余额", "交易摘要", "对手信息"},
                new String[]{"2025.03.08", "CNY", "-1,234.56", "12,345.67", "快捷支付", "财付通"}
        );
        List<Transaction> out = importer.parse(rows, "CMB", "debit", "");
        assertEquals(1, out.size());
        assertEquals(1234.56, out.get(0).getBalanceMoney());
        assertEquals("快捷支付", out.get(0).getTransactionDesc());
    }

    @Test
    void parsesDualIncomeExpenseColumns() {
        List<String[]> rows = List.of(
                new String[]{"交易日", "摘要", "收入", "支出", "余额"},
                new String[]{"2025-03-08", "代发工资", "8,000.00", "0.00", "18,000.00"}
        );
        List<Transaction> out = importer.parse(rows, "CMB", "debit", "");
        assertEquals(1, out.size());
        assertEquals(8000.0, out.get(0).getIncomeMoney());
        assertEquals(0.0, out.get(0).getBalanceMoney());
    }

    @Test
    void mergesContinuationLineWithoutDate() {
        List<String[]> rows = Arrays.asList(
                new String[]{"2025-03-08", "CNY", "-81.00", "1000.00", "快捷支付", "财付通"},
                new String[]{"支付科技有限公司"}
        );
        List<Transaction> out = importer.parse(rows, "CMB", "debit", "");
        assertEquals(1, out.size());
        assertTrue(out.get(0).getDemoArea().contains("支付科技有限公司"));
    }

    @Test
    void parsesForeignCurrencyWithdrawal() {
        List<String[]> rows = List.of(
                new String[]{"记账日期", "货币", "交易金额", "联机余额", "交易摘要"},
                new String[]{"2025-07-03", "GBP", "-200.00", "0.00", "柜台取现"}
        );
        List<Transaction> out = importer.parse(rows, "CMB", "debit", "");
        assertEquals(1, out.size());
        assertEquals(200.0, out.get(0).getBalanceMoney());
        assertEquals("GBP", out.get(0).getBalanceCurrency());
        assertEquals("柜台取现", out.get(0).getTransactionDesc());
    }

    @Test
    void pageFractionFooterNotMergedIntoTransaction() {
        List<String[]> rows = List.of(
                new String[]{"2025-08-08", "CNY", "-3.00", "795.98", "快捷支付", "扫二维码付款"},
                new String[]{"2/6"}
        );
        List<Transaction> out = importer.parse(rows, "CMB", "debit", "");
        assertEquals(1, out.size());
        assertTrue(out.get(0).getOpponentName().contains("扫二维码付款"));
        assertFalse(out.get(0).getDemoArea().contains("2/6"));
    }

    @Test
    void skipsHeaderAndFooterNoise() {
        List<String[]> rows = List.of(
                new String[]{"招商银行交易流水"},
                new String[]{"记账日期", "交易金额", "交易摘要"},
                new String[]{"2025-03-08", "CNY", "100.00", "200.00", "利息"},
                new String[]{"第 1 页 共 2 页"}
        );
        List<Transaction> out = importer.parse(rows, "CMB", "debit", "");
        assertEquals(1, out.size());
        assertEquals(100.0, out.get(0).getIncomeMoney());
    }
}
