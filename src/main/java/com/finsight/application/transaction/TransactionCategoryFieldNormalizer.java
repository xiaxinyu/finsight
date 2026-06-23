package com.finsight.application.transaction;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Resolves {@code consume_name} from active {@code cls_category} and syncs all category columns.
 */
@Component
public class TransactionCategoryFieldNormalizer {

    private final ConsumeCategoryService categoryService;

    public TransactionCategoryFieldNormalizer(ConsumeCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public void normalize(Transaction t) {
        if (t == null) {
            return;
        }
        String code = TransactionCategoryFieldSync.resolveCanonicalCode(t);
        if (StringUtils.isBlank(code)) {
            return;
        }
        String name = lookupActiveCategoryName(code);
        if (StringUtils.isBlank(name)) {
            name = StringUtils.trimToNull(t.getConsumeName());
        }
        TransactionCategoryFieldSync.applyCategoryFields(t, code, name);
    }

    private String lookupActiveCategoryName(String code) {
        ConsumeCategory cat = categoryService.getOne(
                Wrappers.<ConsumeCategory>lambdaQuery()
                        .eq(ConsumeCategory::getCode, code)
                        .ne(ConsumeCategory::getDeleted, 1),
                false);
        return cat == null ? null : StringUtils.trimToNull(cat.getName());
    }
}
