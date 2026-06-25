package com.finsight.application.consume;

import com.finsight.domain.model.ConsumeRule;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ConsumeRuleService extends IService<ConsumeRule> {
    List<ConsumeRule> listActive();
    void loadTags(List<ConsumeRule> rules);
    void softDeleteById(String id, String updateUser);
}
