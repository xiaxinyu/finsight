package com.finsight.application.classification;

import com.finsight.application.analytics.ConfigVersionBump;
import com.finsight.application.analytics.DirtyMonthService;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.transaction.TransactionReclassificationResult;
import com.finsight.application.transaction.TransactionReclassificationService;
import com.finsight.domain.model.ClassificationMigrationDetail;
import com.finsight.web.api.dto.TransactionParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ClassificationReclassificationFacade {

    private final TransactionReclassificationService reclassificationService;
    private final ClassificationMigrationBatchService batchService;
    private final AuthenticationFacade authenticationFacade;
    private final ConfigVersionBump configVersionBump;
    private final DirtyMonthService dirtyMonthService;

    public ClassificationReclassificationFacade(TransactionReclassificationService reclassificationService,
                                                ClassificationMigrationBatchService batchService,
                                                AuthenticationFacade authenticationFacade,
                                                ConfigVersionBump configVersionBump,
                                                DirtyMonthService dirtyMonthService) {
        this.reclassificationService = reclassificationService;
        this.batchService = batchService;
        this.authenticationFacade = authenticationFacade;
        this.configVersionBump = configVersionBump;
        this.dirtyMonthService = dirtyMonthService;
    }

    public TransactionReclassificationResult previewByIds(String ids, boolean overrideExisting) {
        return reclassificationService.reclassify(ids, false, overrideExisting, false, userName());
    }

    public TransactionReclassificationResult previewUnclassified(TransactionParam param) throws Exception {
        return reclassificationService.reclassifyUnclassified(param, false, false, userName());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyByIds(String ids, boolean overrideExisting, String reason) {
        TransactionReclassificationResult preview = reclassificationService.reclassify(
                ids, false, overrideExisting, false, userName());
        List<ClassificationMigrationDetail> details = toDetails(preview);
        var batch = batchService.createBatch("RECLASSIFY", reason, userName(), details);
        TransactionReclassificationResult applied = reclassificationService.reclassify(
                ids, true, overrideExisting, false, userName());
        batchService.markApplied(batch.getId());
        configVersionBump.bumpMetricRefresh();
        return Map.of(
                "batchId", batch.getId(),
                "result", applied,
                "dirtyMonths", dirtyMonthService.listDirty());
    }

    private static List<ClassificationMigrationDetail> toDetails(TransactionReclassificationResult preview) {
        List<ClassificationMigrationDetail> details = new ArrayList<>();
        if (preview.getPreview() == null) {
            return details;
        }
        for (Map<String, Object> row : preview.getPreview()) {
            ClassificationMigrationDetail d = new ClassificationMigrationDetail();
            d.setTransactionId(String.valueOf(row.getOrDefault("transactionId", row.get("id"))));
            d.setOldConsumeCode(stringVal(row.get("beforeCategoryCode")));
            d.setNewConsumeCode(stringVal(row.get("categoryCode")));
            d.setOldConsumeName(stringVal(row.get("beforeCategoryName")));
            d.setNewConsumeName(stringVal(row.get("categoryName")));
            d.setAction(String.valueOf(row.getOrDefault("action", "PREVIEW")));
            details.add(d);
        }
        return details;
    }

    private static String stringVal(Object v) {
        return v == null ? null : StringUtils.trimToNull(String.valueOf(v));
    }

    private String userName() {
        return authenticationFacade.getUserName();
    }
}
