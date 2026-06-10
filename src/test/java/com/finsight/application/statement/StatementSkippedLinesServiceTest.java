package com.finsight.application.statement;

import com.finsight.domain.model.Statement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    }
}
