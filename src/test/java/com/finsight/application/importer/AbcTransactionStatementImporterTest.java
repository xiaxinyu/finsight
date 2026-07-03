package com.finsight.application.importer;

import com.finsight.application.importer.impl.AbcTransactionStatementImporter;
import com.finsight.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbcTransactionStatementImporterTest {

    private final AbcTransactionStatementImporter importer = new AbcTransactionStatementImporter();
    private final SimpleDateFormat ymd = new SimpleDateFormat("yyyyMMdd");

    private static List<String[]> sampleRows() {
        return List.of(
                new String[]{"交易日期", "入账日期", "卡号后四位", "交易摘要", "交易地点", "交易金额", "入账金额"},
                new String[]{"20241031", "20241031", "7888", "跨行消费", "深圳市龙岗区爱一家室内装饰商行",
                        "-299960/CNY", "-299960/CNY"},
                new String[]{"20241203", "20241203", "7888", "跨行转账（", "深圳市分行清算中心",
                        "5749.00/CNY", "5749.00/CNY"},
                new String[]{"20241203", "20241203", "7888", "还款转出", "", "-5748.9/CNY", "-5748.9/CNY"},
                new String[]{"20241217", "20241217", "7888", "还款转出", "", "-0.1/CNY", "-0.1/CNY"},
                new String[]{"20250102", "20250102", "7888", "跨行转账（", "深圳市分行清算中心",
                        "5750.00/CNY", "5750.00/CNY"}
        );
    }

    @Test
    void parsesFullAbcCreditSample() throws Exception {
        List<Transaction> out = importer.parse(sampleRows(), "ABC", "credit", "");

        assertEquals(5, out.size());

        // 跨行消费: integer fen export → ¥2999.60 expense
        Transaction purchase = out.get(0);
        assertEquals("20241031", ymd.format(purchase.getTransactionDate()));
        assertEquals("20241031", ymd.format(purchase.getBookKeepingDate()));
        assertEquals(2999.60, purchase.getBalanceMoney(), 0.001);
        assertEquals(0.0, purchase.getIncomeMoney());
        assertEquals("expense", purchase.getTxnKind());
        assertTrue(purchase.getTransactionDesc().contains("跨行消费"));
        assertTrue(purchase.getTransactionDesc().contains("爱一家"));

        // 跨行转账 in: finance inflow (not lifestyle income)
        Transaction transferIn = out.get(1);
        assertEquals("20241203", ymd.format(transferIn.getTransactionDate()));
        assertEquals(5749.0, transferIn.getIncomeMoney(), 0.001);
        assertEquals(0.0, transferIn.getBalanceMoney());
        assertEquals("finance", transferIn.getTxnKind());

        // 还款转出: finance repayment outflow, no location required
        Transaction repay1 = out.get(2);
        assertEquals("20241203", ymd.format(repay1.getTransactionDate()));
        assertEquals(5748.9, repay1.getBalanceMoney(), 0.001);
        assertEquals("finance", repay1.getTxnKind());
        assertEquals("还款转出", repay1.getTransactionDesc());

        Transaction repay2 = out.get(3);
        assertEquals("20241217", ymd.format(repay2.getTransactionDate()));
        assertEquals(0.1, repay2.getBalanceMoney(), 0.001);
        assertEquals("finance", repay2.getTxnKind());

        // Later transfer in
        Transaction transferIn2 = out.get(4);
        assertEquals("20250102", ymd.format(transferIn2.getTransactionDate()));
        assertEquals(5750.0, transferIn2.getIncomeMoney(), 0.001);
        assertEquals("finance", transferIn2.getTxnKind());
    }

    @Test
    void scalesIntegerFenOnlyWhenNoDecimalPoint() {
        List<String[]> rows = List.of(
                new String[]{"交易日期", "入账日期", "卡号后四位", "交易摘要", "交易地点", "交易金额", "入账金额"},
                new String[]{"20241031", "20241031", "7888", "跨行消费", "商户", "-299960/CNY", "-299960/CNY"},
                new String[]{"20241203", "20241203", "7888", "跨行转账（", "清算中心", "5749.00/CNY", "5749.00/CNY"},
                new String[]{"20241203", "20241203", "7888", "跨行消费", "商户", "-100/CNY", "-100/CNY"}
        );
        List<Transaction> out = importer.parse(rows, "ABC", "credit", "");
        assertEquals(2999.60, out.get(0).getBalanceMoney(), 0.001);
        assertEquals(5749.0, out.get(1).getIncomeMoney(), 0.001);
        assertEquals(100.0, out.get(2).getBalanceMoney(), 0.001);
    }

    @Test
    void parsesWithoutHeaderWhenRowsLookLikeData() {
        List<String[]> rows = Collections.singletonList(
                new String[]{"20241031", "20241031", "7888", "跨行消费", "商户A", "-100/CNY", "-100/CNY"});
        List<Transaction> out = importer.parse(rows, "ABC", "credit", "");
        assertEquals(1, out.size());
        assertEquals(100.0, out.get(0).getBalanceMoney());
    }
}
