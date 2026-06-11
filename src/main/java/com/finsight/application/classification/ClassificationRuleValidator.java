package com.finsight.application.classification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.domain.model.ClassificationRule;
import com.finsight.domain.model.ConsumeCategory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ClassificationRuleValidator {

    private final ConsumeCategoryService categoryService;

    public ClassificationRuleValidator(ConsumeCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public void validate(ClassificationRule rule) {
        if (rule == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rule body is required");
        }
        if (StringUtils.isBlank(rule.getPattern())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keyword (pattern) is required");
        }
        String categoryRef = StringUtils.trimToEmpty(rule.getCategoryId());
        if (categoryRef.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
        }
        ConsumeCategory category = resolveActiveCategory(categoryRef);
        if (category == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Category not found or deleted: " + categoryRef);
        }
        if (StringUtils.isNotBlank(category.getCode())) {
            rule.setCategoryId(category.getCode());
        }
    }

    private ConsumeCategory resolveActiveCategory(String ref) {
        LambdaQueryWrapper<ConsumeCategory> byCode = Wrappers.lambdaQuery();
        byCode.eq(ConsumeCategory::getCode, ref).ne(ConsumeCategory::getDeleted, 1);
        ConsumeCategory cat = categoryService.getOne(byCode, false);
        if (cat != null) {
            return cat;
        }
        ConsumeCategory byId = categoryService.getById(ref);
        if (byId != null && byId.getDeleted() != null && byId.getDeleted() == 1) {
            return null;
        }
        return byId;
    }
}
