package com.finsight.application.consume.impl;

import com.finsight.application.classification.ClassificationRuleValidator;
import com.finsight.infrastructure.mapper.ConsumeRuleMapper;
import com.finsight.infrastructure.mapper.ConsumeRuleTagMapper;
import com.finsight.domain.model.ConsumeRule;
import com.finsight.domain.model.ConsumeRuleTag;
import com.finsight.application.consume.ConsumeRuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ConsumeRuleServiceImpl extends ServiceImpl<ConsumeRuleMapper, ConsumeRule> implements ConsumeRuleService {

    @Autowired
    private ConsumeRuleTagMapper consumeRuleTagMapper;

    @Autowired
    private ClassificationRuleValidator ruleValidator;

    @Override
    public List<ConsumeRule> listActive() {
        LambdaQueryWrapper<ConsumeRule> qw = Wrappers.lambdaQuery();
        qw.eq(ConsumeRule::getActive, 1).orderByAsc(ConsumeRule::getPriority);
        List<ConsumeRule> list = super.list(qw);
        loadTags(list);
        return list;
    }

    @Override
    @Transactional
    public boolean save(ConsumeRule entity) {
        ruleValidator.validate(entity);
        boolean success = super.save(entity);
        if (success) {
            saveTags(entity);
        }
        return success;
    }

    @Override
    @Transactional
    public boolean updateById(ConsumeRule entity) {
        ruleValidator.validate(entity);
        boolean success = super.updateById(entity);
        if (success) {
            // delete old tags
            LambdaQueryWrapper<ConsumeRuleTag> qw = Wrappers.lambdaQuery();
            qw.eq(ConsumeRuleTag::getRuleId, entity.getId());
            consumeRuleTagMapper.delete(qw);
            // save new tags
            saveTags(entity);
        }
        return success;
    }

    private void saveTags(ConsumeRule entity) {
        if (entity.getTags() != null && !entity.getTags().isEmpty()) {
            for (String tag : entity.getTags()) {
                ConsumeRuleTag ruleTag = new ConsumeRuleTag();
                ruleTag.setRuleId(entity.getId());
                ruleTag.setTag(tag);
                consumeRuleTagMapper.insert(ruleTag);
            }
        }
    }

    public void loadTags(List<ConsumeRule> rules) {
        if (rules == null || rules.isEmpty()) return;
        List<String> ruleIds = rules.stream().map(ConsumeRule::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ConsumeRuleTag> qw = Wrappers.lambdaQuery();
        qw.in(ConsumeRuleTag::getRuleId, ruleIds);
        List<ConsumeRuleTag> allTags = consumeRuleTagMapper.selectList(qw);
        
        for (ConsumeRule rule : rules) {
            List<String> tags = allTags.stream()
                .filter(t -> t.getRuleId().equals(rule.getId()))
                .map(ConsumeRuleTag::getTag)
                .collect(Collectors.toList());
            rule.setTags(tags);
        }
    }
}
