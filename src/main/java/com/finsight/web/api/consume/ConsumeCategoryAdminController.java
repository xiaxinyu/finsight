package com.finsight.web.api.consume;

import com.finsight.domain.model.Category;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.application.consume.ConsumeCategoryAdminFacade;
import com.finsight.web.api.dto.CollectionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @deprecated use {@link com.finsight.web.api.classification.ClassificationCategoryAdminController}.
 */
@Deprecated
@RestController
@RequestMapping("/api/v1/consume/categories")
public class ConsumeCategoryAdminController {
    @Autowired
    private ConsumeCategoryAdminFacade adminFacade;

    @GetMapping
    public CollectionResult<ConsumeCategory> list(
            @RequestParam(value = "includeDeleted", required = false, defaultValue = "false") boolean includeDeleted) {
        return adminFacade.list(includeDeleted);
    }

    @PostMapping
    public Category add(@RequestBody ConsumeCategory cat) {
        return adminFacade.add(cat);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable("id") String id,
                           @RequestBody ConsumeCategory cat,
                           @RequestParam(value = "cascade", required = false) Boolean cascade) {
        return adminFacade.update(id, cat, cascade);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id){
        adminFacade.delete(id);
    }

    @PostMapping("/{id}/migrate")
    public CollectionResult<String> migrate(@PathVariable("id") String id,
                                            @RequestParam(value = "toCode", required = false) String toCode,
                                            @RequestParam(value = "deleteAfter", required = false, defaultValue = "true") boolean deleteAfter,
                                            @RequestParam(value = "cascade", required = false, defaultValue = "true") boolean cascade){
        return adminFacade.migrate(id, toCode, deleteAfter, cascade);
    }
}
