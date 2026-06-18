package com.finsight.web.api.transaction;

import com.finsight.application.transaction.TransactionDrillBreakdownService;
import com.finsight.application.transaction.TransactionStatsService;
import com.finsight.web.api.dto.CommonResult;
import com.finsight.web.api.dto.TransactionParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionStatsController {

    private final TransactionStatsService statsService;
    private final TransactionDrillBreakdownService drillBreakdownService;

    public TransactionStatsController(TransactionStatsService statsService,
                                      TransactionDrillBreakdownService drillBreakdownService) {
        this.statsService = statsService;
        this.drillBreakdownService = drillBreakdownService;
    }

    @GetMapping("/stats")
    public CommonResult stats(TransactionParam param) throws Exception {
        return CommonResult.success(statsService.aggregate(param));
    }

    @GetMapping("/drill-breakdown")
    public CommonResult drillBreakdown(TransactionParam param,
                                       @org.springframework.web.bind.annotation.RequestParam(
                                               value = "sampleLimit", defaultValue = "200") int sampleLimit)
            throws Exception {
        return CommonResult.success(drillBreakdownService.load(param, sampleLimit));
    }
}
