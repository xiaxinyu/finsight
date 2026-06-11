package com.finsight.application.statement;

import com.finsight.domain.model.Statement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementSkippedLinesServiceTest {

    private final StatementSkippedLinesService service = new StatementSkippedLinesService();

    @Test
    void analyze_marksHeaderAndNoiseAsSkipped() {
        Statement statement = new Statement();
        statement.setSource("CRBANK");
        statement.setFileName("crbank.csv");
        statement.setContent(String.join("\n",
                "华润银行个人账户交易明细",
                "账号,户名,起止日期",
                "交易日期,渠道,收入,支出,账户余额,摘要,对方户名,对方账号,交易地点",
                "2025-10-01,网银,0,12.50,1000.00,午餐,商户A,622200,深圳",
                "2025-10-02,ATM,500.00,0,1500.00,工资,公司,622201,上海"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        Set<String> reasons = skipped.stream().map(SkippedImportRow::getReason).collect(Collectors.toSet());

        assertFalse(skipped.isEmpty());
        assertTrue(reasons.stream().anyMatch(r -> r.contains("metadata") || r.contains("header") || r.contains("Preamble")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("2025-10-01")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("2025-10-02")));
        SkippedImportRow header = skipped.stream()
                .filter(r -> r.getReason().contains("header"))
                .findFirst()
                .orElse(null);
        assertTrue(header != null);
        assertTrue(header.getFileLineNumber() > 0);
        assertTrue(header.getColumns() != null && !header.getColumns().isEmpty());
        assertTrue(header.getHint() != null && header.getHint().contains("columns="));
    }

    @Test
    void analyze_enrichesContinuationLineWithContext() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-05-26 CNY -3.00 49494.31 快捷支付 深圳市地铁",
                "屈臣氏个人护理用品商店深圳分公司"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        SkippedImportRow continuation = skipped.stream()
                .filter(r -> r.getRawText().contains("屈臣氏"))
                .findFirst()
                .orElse(null);
        assertTrue(continuation != null);
        assertTrue(continuation.getReason().contains("Continuation"));
        assertEquals(3, continuation.getFileLineNumber());
        assertTrue(continuation.getContextBefore() != null && continuation.getContextBefore().contains("2025-05-26"));
        assertTrue(continuation.getHint() != null && continuation.getHint().contains("columns="));
    }
}
