package com.finsight.web.api.transaction;

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

    public TransactionStatsController(TransactionStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public CommonResult stats(TransactionParam param) throws Exception {
        return CommonResult.success(statsService.aggregate(param));
    }
}
