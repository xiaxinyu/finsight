package com.finsight.application.importer;

import com.finsight.application.importer.impl.CcbTransactionStatementImporter;
import com.finsight.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CcbCreditTransactionStatementImporterTest {

    private final CcbTransactionStatementImporter importer = new CcbTransactionStatementImporter();

    @Test
    void parsesCcbCreditExcelLayoutWithPreamble() {
        List<String[]> rows = List.of(
                new String[]{"中国建设银行"},
                new String[]{"中国建设银行信用卡交易明细"},
                new String[]{"客户姓名：夏昕雨"},
                new String[]{""},
                new String[]{"交易日", "入账日", "信用卡卡", "类型", "入账币种", "入账金额", "交易描述"},
                new String[]{""},
                new String[]{"20260615", "20260615", "'6227080473", "消费", "人民币", "6", "财付通-深圳市地铁相关运营主体"},
                new String[]{"20260614", "20260614", "'6227080473", "消费", "人民币", "2", "财付通-深圳市地铁相关运营主体"}
        );
        List<Transaction> out = importer.parse(rows, "CCB", "credit", "");
        assertEquals(2, out.size());
        assertEquals(6.0, out.get(0).getBalanceMoney());
        assertTrue(out.get(0).getTransactionDesc().contains("财付通"));
        assertEquals("人民币", out.get(0).getBalanceCurrency());
    }

    @Test
    void parsesSixColumnRowsWithoutDescriptionColumnPadding() {
        List<String[]> rows = List.of(
                new String[]{"交易日", "入账日", "6227080473", "消费", "人民币", "4"},
                new String[]{"20260613", "20260613", "6227080473", "消费", "人民币", "4", "地铁扫码"}
        );
        List<Transaction> out = importer.parse(rows, "CCB", "credit", "");
        assertEquals(1, out.size());
        assertEquals(4.0, out.get(0).getBalanceMoney());
    }

    @Test
    void parsesDatesWithDecimalSuffixFromExcel() {
        List<String[]> rows = List.of(
                new String[]{"交易日", "入账日", "信用卡卡", "类型", "入账币种", "入账金额", "交易描述"},
                new String[]{"20260615.0", "20260615.0", "6227080473", "消费", "人民币", "6", "地铁"}
        );
        List<Transaction> out = importer.parse(rows, "CCB", "credit", "");
        assertEquals(1, out.size());
    }
}
