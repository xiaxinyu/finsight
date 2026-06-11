package com.finsight.application.statement;

import com.finsight.domain.model.Statement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementSkippedLinesServiceTest {

    private final StatementSkippedLinesService service = new StatementSkippedLinesService();

    @Test
    void analyze_marksHeaderAndNoiseAsIgnoredNotSkipped() {
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

        ImportLineStats stats = service.summarize(statement, "debit", 2);
        assertTrue(stats.skipped() == 0);
        assertTrue(stats.ignored() > 0);
        assertTrue(stats.skippedRows().stream().noneMatch(r -> r.getRawText().contains("2025-10-01")));
        assertTrue(stats.skippedRows().stream().noneMatch(r -> r.getRawText().contains("2025-10-02")));
    }

    @Test
    void analyze_mergedContinuationLineNotListedAsSkipped() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-05-26 CNY -3.00 49494.31 快捷支付 深圳市地铁",
                "屈臣氏个人护理用品商店深圳分公司"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("屈臣氏")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("2025-05-26")));
    }

    @Test
    void analyze_normalCmbRowWithPageFooter_notMarkedSkipped() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-08-07 CNY -1,000.00 798.98 银联无卡自助消费 夏昕雨",
                "2025-08-08 CNY -3.00 795.98 快捷支付 扫二维码付款",
                "2/6"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("2025-08-08")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("扫二维码付款")));
        assertTrue(skipped.stream().noneMatch(r -> "2/6".equals(r.getRawText())));
    }

    @Test
    void analyze_cmbCommaThousands_expenseNotMarkedSkipped() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-12-03 CNY -5,749.00 2,362.39 转账汇款 夏斯雨"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("2025-12-03")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("夏斯雨")));
    }

    @Test
    void analyze_cmbCommaThousands_incomeNotMarkedSkipped() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2026-03-22 CNY 200,000.00 200,659.99 个贷放款 夏昕雨"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("2026-03-22")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("个贷放款")));
    }

    @Test
    void summarize_lineCountsAddUp() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "招商银行交易流水",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "账户类型：ALL/全币种 开户行：深圳时代广场支行",
                "2025-10-14 CNY 48,333.30 190,183.34 行内转账转入 欧涛",
                "2025-07-03 GBP -200.00 0.00 柜台取现",
                "2/6"
        ));

        ImportLineStats stats = service.summarize(statement, "debit", 2);
        assertEquals(stats.lines(), stats.linked() + stats.skipped() + stats.ignored());
        assertTrue(stats.ignored() > 0);
    }

    @Test
    void analyze_gbpWithdrawal_notMarkedSkipped() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要",
                "2025-07-03 GBP -200.00 0.00 柜台取现"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("柜台取现")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("GBP")));
    }

    @Test
    void analyze_sameDaySameOpponentCommaAmounts_bothLinked() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-10-14 CNY 75,000.30 141,850.04 行内转账转入 欧涛",
                "2025-10-14 CNY 48,333.30 190,183.34 行内转账转入 欧涛"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("48,333.30")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("75,000.30")));
    }

    @Test
    void analyze_allFiveReportedPageBoundaryRows_parseAndLink() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-07-03 CNY -1,960.94 7,189.77 结售汇即时售汇 夏昕雨",
                "1/6",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-08-08 CNY -3.00 795.98 快捷支付 扫二维码付款",
                "2/6",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-10-14 CNY 48,333.30 190,183.34 行内转账转入 欧涛",
                "3/6",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-12-03 CNY -5,749.00 2,362.39 转账汇款 夏昕雨",
                "4/6",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2026-03-22 CNY 200,000.00 200,659.99 个贷放款 夏昕雨",
                "5/6"
        ));

        ImportLineStats stats = service.summarize(statement, "debit", -1);
        assertEquals(5, stats.transactions());
        assertEquals(0, stats.skipped());
    }

    @Test
    void analyze_pageBoundaryTxnNotMarkedSkipped() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-10-14 CNY 75,000.30 141,850.04 行内转账转入 欧涛",
                "2025-10-14 CNY 48,333.30 190,183.34 行内转账转入 欧涛",
                "3/6",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-10-15 CNY 39,000.00 229,183.34 汇入汇款 夏昕雨"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("48,333.30")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("75,000.30")));
    }

    @Test
    void analyze_balanceColumnDoesNotStealTxnMatch() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-05-26 CNY -3.00 49,494.31 快捷支付 深圳市地铁相关运营主体",
                "2025-05-27 CNY 49,494.31 100,000.00 行内转账转入 某人"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("深圳市地铁")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("-3.00")));
    }

    @Test
    void analyze_bulkCmbTxnLines_notMarkedSkipped() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-05-26 CNY -3.00 49,494.31 快捷支付 深圳市地铁相关运营主体",
                "2025-07-03 CNY -1,960.94 7,189.77 结售汇即时售汇 夏昕雨",
                "2025-08-08 CNY -3.00 795.98 快捷支付 扫二维码付款",
                "2025-10-14 CNY 48,333.30 190,183.34 行内转账转入 欧涛",
                "2025-12-03 CNY -5,749.00 2,362.39 转账汇款 夏斯雨",
                "2026-03-22 CNY 200,000.00 200,659.99 个贷放款 夏昕雨"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("深圳市地铁")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("结售汇")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("扫二维码付款")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("48,333.30")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("5,749.00")));
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("200,000.00")));
    }

    @Test
    void analyze_watsonsSplitAcrossRows_mergedIntoPreviousTransaction() {
        Statement statement = new Statement();
        statement.setSource("CMB");
        statement.setFileName("cmb.pdf");
        statement.setContent(String.join("\n",
                "记账日期 货币 交易金额 联机余额 交易摘要 对手信息",
                "2025-07-30 CNY -26.40 1731.46 快捷支付 刘美妮",
                "屈臣氏珠海横琴管理咨询有限公"
        ));

        List<SkippedImportRow> skipped = service.analyze(statement, "debit");
        assertTrue(skipped.stream().noneMatch(r -> r.getRawText().contains("屈臣氏")));
    }
}
