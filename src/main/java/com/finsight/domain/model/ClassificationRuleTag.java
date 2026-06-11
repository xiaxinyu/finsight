package com.finsight.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("cls_rule_tag")
@Getter
@Setter
public class ClassificationRuleTag {
    private String ruleId;
    private String tag;
}
