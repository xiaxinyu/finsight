package com.finsight.web.api.classification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.application.classification.ClassificationRuleHygieneService;
import com.finsight.application.classification.ClassificationRuleValidator;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.consume.ConsumeRuleService;
import com.finsight.domain.model.ClassificationRule;
import com.finsight.domain.model.ConsumeRule;
import com.finsight.web.api.dto.ClassificationTestRequest;
import com.finsight.web.api.dto.ClassificationTestResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/classification/rules")
public class ClassificationRuleController {

    private final ConsumeRuleService ruleService;
    private final ClassificationService classificationService;
    private final ClassificationRuleValidator ruleValidator;
    private final ClassificationRuleHygieneService ruleHygieneService;

    public ClassificationRuleController(ConsumeRuleService ruleService,
                                        ClassificationService classificationService,
                                        ClassificationRuleValidator ruleValidator,
                                        ClassificationRuleHygieneService ruleHygieneService) {
        this.ruleService = ruleService;
        this.classificationService = classificationService;
        this.ruleValidator = ruleValidator;
        this.ruleHygieneService = ruleHygieneService;
    }

    @GetMapping
    public List<ClassificationRule> list(
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "active", required = false) Integer active,
            @RequestParam(value = "includeInactive", required = false, defaultValue = "false") boolean includeInactive,
            @RequestParam(value = "includeInvalid", required = false, defaultValue = "false") boolean includeInvalid) {
        LambdaQueryWrapper<ConsumeRule> qw = Wrappers.lambdaQuery();
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            qw.eq(ConsumeRule::getCategoryId, categoryId);
        }
        if (active != null) {
            qw.eq(ConsumeRule::getActive, active);
        } else if (!includeInactive) {
            qw.eq(ConsumeRule::getActive, 1);
        }
        qw.orderByAsc(ConsumeRule::getPriority);
        List<ConsumeRule> list = ruleService.list(qw);
        ruleService.loadTags(list);
        if (!includeInvalid) {
            list = list.stream()
                    .filter(r -> r != null && StringUtils.isNotBlank(r.getPattern()))
                    .collect(Collectors.toList());
        }
        if (tag != null && !tag.trim().isEmpty()) {
            String filterTag = tag.trim();
            list = list.stream()
                    .filter(r -> r.getTags() != null && r.getTags().contains(filterTag))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(list);
    }

    @PostMapping
    public ClassificationRule add(@RequestBody ClassificationRule rule) {
        ruleValidator.validate(rule);
        rule.setId(com.finsight.common.util.StringTool.generateID());
        ruleService.save(ConsumeRule.from(rule));
        classificationService.reload();
        return rule;
    }

    @PutMapping("/{id}")
    public ClassificationRule update(@PathVariable String id, @RequestBody ClassificationRule rule) {
        rule.setId(id);
        ruleValidator.validate(rule);
        ruleService.updateById(ConsumeRule.from(rule));
        classificationService.reload();
        return rule;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        ruleService.removeById(id);
        classificationService.reload();
    }

    @PostMapping("/reload")
    public String reload() {
        classificationService.reload();
        return "ok";
    }

    @GetMapping("/hygiene")
    public java.util.Map<String, Object> hygiene() {
        return ruleHygieneService.hygieneSummary();
    }

    @GetMapping("/orphans")
    public List<ClassificationRule> orphans() {
        return new ArrayList<>(ruleHygieneService.listOrphanRules());
    }

    @GetMapping("/invalid-patterns")
    public List<ClassificationRule> invalidPatterns() {
        return new ArrayList<>(ruleHygieneService.listActiveInvalidPatternRules());
    }

    @GetMapping("/recommend-unclassified")
    public List<String> recommendUnclassified(
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
        return ruleHygieneService.recommendKeywordsFromUnclassified(limit);
    }

    @PostMapping("/test")
    public ClassificationTestResult test(@RequestBody ClassificationTestRequest req) {
        ClassificationTestResult out = new ClassificationTestResult();
        out.setNarration(req.getNarration());
        if (StringUtils.isBlank(req.getNarration())) {
            out.setMessage("narration is required");
            return out;
        }
        int topN = req.getTopN() <= 0 ? 3 : Math.min(req.getTopN(), 10);
        List<ClassificationService.Result> hits = classificationService.classifyTopN(
                req.getNarration(),
                req.getBankCode(),
                req.getCardTypeCode(),
                req.getAmount(),
                req.getTxnDate(),
                topN);
        if (hits == null || hits.isEmpty()) {
            out.setMessage("No category matched");
            return out;
        }
        for (ClassificationService.Result h : hits) {
            ClassificationTestResult.Hit hit = new ClassificationTestResult.Hit();
            hit.setCategoryCode(h.id);
            hit.setCategoryName(h.name);
            out.getHits().add(hit);
        }
        out.setMessage("matched " + out.getHits().size() + " candidate(s)");
        return out;
    }
}
