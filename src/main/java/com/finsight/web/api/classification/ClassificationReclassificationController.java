package com.finsight.web.api.classification;

import com.finsight.application.classification.ClassificationMigrationBatchService;
import com.finsight.application.classification.ClassificationReclassificationFacade;
import com.finsight.application.classification.ConfigVersionService;
import com.finsight.application.transaction.TransactionReclassificationResult;
import com.finsight.web.api.dto.TransactionParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/classification/reclassification")
public class ClassificationReclassificationController {

    private final ClassificationReclassificationFacade facade;
    private final ClassificationMigrationBatchService batchService;
    private final ConfigVersionService configVersionService;

    public ClassificationReclassificationController(ClassificationReclassificationFacade facade,
                                                    ClassificationMigrationBatchService batchService,
                                                    ConfigVersionService configVersionService) {
        this.facade = facade;
        this.batchService = batchService;
        this.configVersionService = configVersionService;
    }

    @PostMapping("/preview")
    public TransactionReclassificationResult preview(
            @RequestParam(value = "ids", required = false) String ids,
            @RequestParam(value = "overrideExisting", defaultValue = "false") boolean overrideExisting,
            TransactionParam param) throws Exception {
        if (ids != null && !ids.isBlank()) {
            return facade.previewByIds(ids, overrideExisting);
        }
        return facade.previewUnclassified(param);
    }

    @PostMapping("/apply")
    public Map<String, Object> apply(
            @RequestParam String ids,
            @RequestParam(value = "overrideExisting", defaultValue = "false") boolean overrideExisting,
            @RequestParam(value = "reason", required = false) String reason) {
        return facade.applyByIds(ids, overrideExisting, reason);
    }

    @GetMapping("/batches")
    public java.util.List<Map<String, Object>> batches(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return batchService.listRecent(limit);
    }

    @GetMapping("/batches/{batchId}")
    public Map<String, Object> batch(@PathVariable String batchId) {
        return batchService.getBatch(batchId);
    }

    @GetMapping("/versions")
    public Map<String, Object> versions() {
        return configVersionService.asMap();
    }
}
