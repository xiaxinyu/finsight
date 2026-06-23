package com.finsight.web.api.classification;

import com.finsight.application.classification.CategoryImpactAction;
import com.finsight.application.classification.CategoryImpactPreviewService;
import com.finsight.application.consume.ConsumeCategoryAdminFacade;
import com.finsight.domain.model.Category;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.web.api.dto.CategoryImpactPreview;
import com.finsight.web.api.dto.CollectionResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/classification/categories")
public class ClassificationCategoryAdminController {

    private final ConsumeCategoryAdminFacade adminFacade;
    private final CategoryImpactPreviewService impactPreviewService;

    public ClassificationCategoryAdminController(ConsumeCategoryAdminFacade adminFacade,
                                                 CategoryImpactPreviewService impactPreviewService) {
        this.adminFacade = adminFacade;
        this.impactPreviewService = impactPreviewService;
    }

    @GetMapping
    public CollectionResult<Category> list(
            @RequestParam(value = "includeDeleted", required = false, defaultValue = "false") boolean includeDeleted) {
        CollectionResult<ConsumeCategory> raw = adminFacade.list(includeDeleted);
        CollectionResult<Category> out = new CollectionResult<>();
        out.setTotal(raw.getTotal());
        out.setRows(raw.getRows() == null ? null : raw.getRows().stream().map(c -> (Category) c).toList());
        return out;
    }

    @PostMapping
    public Category add(@RequestBody Category cat) {
        return adminFacade.add(cat);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable String id, @RequestBody Category cat,
                           @RequestParam(value = "cascade", required = false, defaultValue = "false") boolean cascade) {
        return adminFacade.update(id, cat, cascade);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        adminFacade.delete(id);
    }

    @GetMapping("/{id}/impact-preview")
    public CategoryImpactPreview impactPreview(
            @PathVariable String id,
            @RequestParam(value = "action", required = false, defaultValue = "delete") String action,
            @RequestParam(value = "targetCode", required = false) String targetCode) {
        return impactPreviewService.preview(id, CategoryImpactAction.parse(action), targetCode);
    }

    @PostMapping("/{id}/migrate")
    public CollectionResult<String> migrate(
            @PathVariable String id,
            @RequestParam(value = "toCode", required = false) String toCode,
            @RequestParam(value = "deleteAfter", required = false, defaultValue = "true") boolean deleteAfter,
            @RequestParam(value = "cascade", required = false, defaultValue = "true") boolean cascade) {
        return adminFacade.migrate(id, toCode, deleteAfter, cascade);
    }
}
