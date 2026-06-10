package com.finsight.web.api.system;

import com.finsight.application.transaction.TransactionDataMigrationService;
import com.finsight.web.api.dto.CommonResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/maintenance")
public class MaintenanceController {

    private final TransactionDataMigrationService migrationService;

    public MaintenanceController(TransactionDataMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/normalize-transaction-amounts")
    public CommonResult normalizeTransactionAmounts() {
        Map<String, Object> result = migrationService.normalizeTransactionAmounts();
        return CommonResult.success(result);
    }
}
