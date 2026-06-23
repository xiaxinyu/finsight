package com.finsight.web.api.system;

import com.finsight.application.analytics.MetricMonthlyService;
import com.finsight.application.analytics.MetricReconciliationService;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.classification.ClassificationAuditSummary;
import com.finsight.application.classification.ClassificationAuditSummaryService;
import com.finsight.application.classification.ClassificationRemediationPlanner;
import com.finsight.application.maintenance.SchemaMigrationVerificationService;
import com.finsight.application.transaction.TransactionDataMigrationService;
import com.finsight.web.api.dto.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/maintenance")
public class MaintenanceController {

    private final TransactionDataMigrationService migrationService;
    private final SchemaMigrationVerificationService verificationService;
    private final MetricMonthlyService metricMonthlyService;
    private final MetricReconciliationService metricReconciliationService;
    private final AuthenticationFacade authenticationFacade;
    private final ClassificationAuditSummaryService auditSummaryService;

    public MaintenanceController(TransactionDataMigrationService migrationService,
                               SchemaMigrationVerificationService verificationService,
                               MetricMonthlyService metricMonthlyService,
                               MetricReconciliationService metricReconciliationService,
                               AuthenticationFacade authenticationFacade,
                               ClassificationAuditSummaryService auditSummaryService) {
        this.migrationService = migrationService;
        this.verificationService = verificationService;
        this.metricMonthlyService = metricMonthlyService;
        this.metricReconciliationService = metricReconciliationService;
        this.authenticationFacade = authenticationFacade;
        this.auditSummaryService = auditSummaryService;
    }

    @PostMapping("/normalize-transaction-amounts")
    public CommonResult normalizeTransactionAmounts() {
        Map<String, Object> result = migrationService.normalizeTransactionAmounts();
        return CommonResult.success(result);
    }

    @PostMapping("/verify-schema-migration")
    public CommonResult verifySchemaMigration() {
        return CommonResult.success(verificationService.verify());
    }

    @GetMapping("/classification-audit-summary")
    public CommonResult classificationAuditSummary() {
        ClassificationAuditSummary summary = auditSummaryService.loadSummary();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("summary", summary);
        out.put("remediationPlan", ClassificationRemediationPlanner.buildPlan(summary));
        out.put("exportScript", "scripts/db/export-classification-audit-baseline.sh");
        out.put("resultsDir", "docs/tech/database/audit-results/");
        return CommonResult.success(out);
    }

    @PostMapping("/refresh-metrics")
    public CommonResult refreshMetrics(@org.springframework.web.bind.annotation.RequestParam String yearMonth) throws Exception {
        return CommonResult.success(metricMonthlyService.refresh(yearMonth));
    }

    @PostMapping("/reconcile-metrics")
    public CommonResult reconcileMetrics(@org.springframework.web.bind.annotation.RequestParam String yearMonth) throws Exception {
        String user = authenticationFacade.getUserName();
        String userId = user == null || user.isBlank() ? "_anonymous" : user;
        return CommonResult.success(metricReconciliationService.reconcile(userId, yearMonth));
    }
}
